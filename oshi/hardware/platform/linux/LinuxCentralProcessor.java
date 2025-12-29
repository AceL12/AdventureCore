package oshi.hardware.platform.linux;

import com.sun.jna.platform.linux.Udev;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.Lshw;
import oshi.driver.linux.proc.CpuStat;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Quartet;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class LinuxCentralProcessor extends AbstractCentralProcessor {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxCentralProcessor.class);

   @Override
   protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
      String cpuVendor = "";
      String cpuName = "";
      String cpuFamily = "";
      String cpuModel = "";
      String cpuStepping = "";
      long cpuFreq = 0L;
      boolean cpu64bit = false;
      StringBuilder armStepping = new StringBuilder();
      String[] flags = new String[0];

      for (String line : FileUtil.readFile(ProcPath.CPUINFO)) {
         String[] splitLine = ParseUtil.whitespacesColonWhitespace.split(line);
         if (splitLine.length >= 2) {
            String var16 = splitLine[0];
            switch (var16) {
               case "vendor_id":
               case "CPU implementer":
                  cpuVendor = splitLine[1];
                  break;
               case "model name":
               case "Processor":
                  if (splitLine[1].indexOf(32) > 0) {
                     cpuName = splitLine[1];
                  }
                  break;
               case "flags":
                  flags = splitLine[1].toLowerCase().split(" ");

                  for (String flag : flags) {
                     if ("lm".equals(flag)) {
                        cpu64bit = true;
                        break;
                     }
                  }
                  break;
               case "stepping":
                  cpuStepping = splitLine[1];
                  break;
               case "CPU variant":
                  if (!armStepping.toString().startsWith("r")) {
                     int rev = ParseUtil.parseLastInt(splitLine[1], 0);
                     armStepping.insert(0, "r" + rev);
                  }
                  break;
               case "CPU revision":
                  if (!armStepping.toString().contains("p")) {
                     armStepping.append('p').append(splitLine[1]);
                  }
                  break;
               case "model":
               case "CPU part":
                  cpuModel = splitLine[1];
                  break;
               case "cpu family":
                  cpuFamily = splitLine[1];
                  break;
               case "cpu MHz":
                  cpuFreq = ParseUtil.parseHertz(splitLine[1]);
            }
         } else if (line.startsWith("CPU architecture: ")) {
            cpuFamily = line.replace("CPU architecture: ", "").trim();
         }
      }

      if (cpuName.isEmpty()) {
         cpuName = FileUtil.getStringFromFile(ProcPath.MODEL);
      }

      if (cpuName.contains("Hz")) {
         cpuFreq = -1L;
      } else {
         long cpuCapacity = Lshw.queryCpuCapacity();
         if (cpuCapacity > cpuFreq) {
            cpuFreq = cpuCapacity;
         }
      }

      if (cpuStepping.isEmpty()) {
         cpuStepping = armStepping.toString();
      }

      String processorID = getProcessorID(cpuVendor, cpuStepping, cpuModel, cpuFamily, flags);
      if (cpuVendor.startsWith("0x")) {
         for (String linex : ExecutingCommand.runNative("lscpu")) {
            if (linex.startsWith("Architecture:")) {
               cpuVendor = linex.replace("Architecture:", "").trim();
            }
         }
      }

      return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuFreq);
   }

   @Override
   protected Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> initProcessorCounts() {
      Quartet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> topology = LinuxOperatingSystem.HAS_UDEV
         ? readTopologyFromUdev()
         : readTopologyFromSysfs();
      List<CentralProcessor.LogicalProcessor> logProcs = topology.getA();
      List<CentralProcessor.ProcessorCache> caches = topology.getB();
      Map<Integer, Integer> coreEfficiencyMap = topology.getC();
      Map<Integer, String> modAliasMap = topology.getD();
      if (logProcs.isEmpty()) {
         logProcs.add(new CentralProcessor.LogicalProcessor(0, 0, 0));
         coreEfficiencyMap.put(0, 0);
      }

      logProcs.sort(Comparator.comparingInt(CentralProcessor.LogicalProcessor::getProcessorNumber));
      List<CentralProcessor.PhysicalProcessor> physProcs = coreEfficiencyMap.entrySet().stream().sorted(Entry.comparingByKey()).map(e -> {
         int pkgId = e.getKey() >> 16;
         int coreId = e.getKey() & 65535;
         return new CentralProcessor.PhysicalProcessor(pkgId, coreId, e.getValue(), modAliasMap.getOrDefault(e.getKey(), ""));
      }).collect(Collectors.toList());
      return new Triplet<>(logProcs, physProcs, caches);
   }

   private static Quartet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromUdev() {
      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
      Set<CentralProcessor.ProcessorCache> caches = new HashSet<>();
      Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
      Map<Integer, String> modAliasMap = new HashMap<>();
      Udev.UdevContext udev = Udev.INSTANCE.udev_new();

      try {
         Udev.UdevEnumerate enumerate = udev.enumerateNew();

         try {
            enumerate.addMatchSubsystem("cpu");
            enumerate.scanDevices();

            for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
               String syspath = entry.getName();
               Udev.UdevDevice device = udev.deviceNewFromSyspath(syspath);
               String modAlias = null;
               if (device != null) {
                  try {
                     modAlias = device.getPropertyValue("MODALIAS");
                  } finally {
                     device.unref();
                  }
               }

               logProcs.add(getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap, modAliasMap));
            }
         } finally {
            enumerate.unref();
         }
      } finally {
         udev.unref();
      }

      return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
   }

   private static Quartet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.ProcessorCache>, Map<Integer, Integer>, Map<Integer, String>> readTopologyFromSysfs() {
      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
      Set<CentralProcessor.ProcessorCache> caches = new HashSet<>();
      Map<Integer, Integer> coreEfficiencyMap = new HashMap<>();
      Map<Integer, String> modAliasMap = new HashMap<>();
      String cpuPath = "/sys/devices/system/cpu/";

      try {
         Stream<Path> cpuFiles = Files.find(Paths.get(cpuPath), Integer.MAX_VALUE, (path, basicFileAttributes) -> path.toFile().getName().matches("cpu\\d+"));

         try {
            cpuFiles.forEach(cpu -> {
               String syspath = cpu.toString();
               Map<String, String> uevent = FileUtil.getKeyValueMapFromFile(syspath + "/uevent", "=");
               String modAlias = uevent.get("MODALIAS");
               logProcs.add(getLogicalProcessorFromSyspath(syspath, caches, modAlias, coreEfficiencyMap, modAliasMap));
            });
         } catch (Throwable var9) {
            if (cpuFiles != null) {
               try {
                  cpuFiles.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (cpuFiles != null) {
            cpuFiles.close();
         }
      } catch (IOException var10) {
         LOG.warn("Unable to find CPU information in sysfs at path {}", cpuPath);
      }

      return new Quartet<>(logProcs, orderedProcCaches(caches), coreEfficiencyMap, modAliasMap);
   }

   private static CentralProcessor.LogicalProcessor getLogicalProcessorFromSyspath(
      String syspath, Set<CentralProcessor.ProcessorCache> caches, String modAlias, Map<Integer, Integer> coreEfficiencyMap, Map<Integer, String> modAliasMap
   ) {
      int processor = ParseUtil.getFirstIntValue(syspath);
      int coreId = FileUtil.getIntFromFile(syspath + "/topology/core_id");
      int pkgId = FileUtil.getIntFromFile(syspath + "/topology/physical_package_id");
      int pkgCoreKey = (pkgId << 16) + coreId;
      coreEfficiencyMap.put(pkgCoreKey, FileUtil.getIntFromFile(syspath + "/cpu_capacity"));
      if (!Util.isBlank(modAlias)) {
         modAliasMap.put(pkgCoreKey, modAlias);
      }

      int nodeId = 0;
      String nodePrefix = syspath + "/node";

      try {
         Stream<Path> path = Files.list(Paths.get(syspath));

         try {
            Optional<Path> first = path.filter(p -> p.toString().startsWith(nodePrefix)).findFirst();
            if (first.isPresent()) {
               nodeId = ParseUtil.getFirstIntValue(first.get().getFileName().toString());
            }
         } catch (Throwable var20) {
            if (path != null) {
               try {
                  path.close();
               } catch (Throwable var17) {
                  var20.addSuppressed(var17);
               }
            }

            throw var20;
         }

         if (path != null) {
            path.close();
         }
      } catch (IOException var21) {
      }

      String cachePath = syspath + "/cache";
      String indexPrefix = cachePath + "/index";

      try {
         Stream<Path> path = Files.list(Paths.get(cachePath));

         try {
            path.filter(p -> p.toString().startsWith(indexPrefix)).forEach(c -> {
               int level = FileUtil.getIntFromFile(c + "/level");
               CentralProcessor.ProcessorCache.Type type = parseCacheType(FileUtil.getStringFromFile(c + "/type"));
               int associativity = FileUtil.getIntFromFile(c + "/ways_of_associativity");
               int lineSize = FileUtil.getIntFromFile(c + "/coherency_line_size");
               long size = ParseUtil.parseDecimalMemorySizeToBinary(FileUtil.getStringFromFile(c + "/size"));
               caches.add(new CentralProcessor.ProcessorCache(level, associativity, lineSize, size, type));
            });
         } catch (Throwable var18) {
            if (path != null) {
               try {
                  path.close();
               } catch (Throwable var16) {
                  var18.addSuppressed(var16);
               }
            }

            throw var18;
         }

         if (path != null) {
            path.close();
         }
      } catch (IOException var19) {
      }

      return new CentralProcessor.LogicalProcessor(processor, coreId, pkgId, nodeId);
   }

   private static CentralProcessor.ProcessorCache.Type parseCacheType(String type) {
      try {
         return CentralProcessor.ProcessorCache.Type.valueOf(type.toUpperCase());
      } catch (IllegalArgumentException var2) {
         return CentralProcessor.ProcessorCache.Type.UNIFIED;
      }
   }

   @Override
   public long[] querySystemCpuLoadTicks() {
      long[] ticks = CpuStat.getSystemCpuLoadTicks();
      if (LongStream.of(ticks).sum() == 0L) {
         ticks = CpuStat.getSystemCpuLoadTicks();
      }

      long hz = LinuxOperatingSystem.getHz();

      for (int i = 0; i < ticks.length; i++) {
         ticks[i] = ticks[i] * 1000L / hz;
      }

      return ticks;
   }

   @Override
   public long[] queryCurrentFreq() {
      long[] freqs = new long[this.getLogicalProcessorCount()];
      long max = 0L;
      Udev.UdevContext udev = Udev.INSTANCE.udev_new();

      try {
         Udev.UdevEnumerate enumerate = udev.enumerateNew();

         try {
            enumerate.addMatchSubsystem("cpu");
            enumerate.scanDevices();

            for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
               String syspath = entry.getName();
               int cpu = ParseUtil.getFirstIntValue(syspath);
               if (cpu >= 0 && cpu < freqs.length) {
                  freqs[cpu] = FileUtil.getLongFromFile(syspath + "/cpufreq/scaling_cur_freq");
                  if (freqs[cpu] == 0L) {
                     freqs[cpu] = FileUtil.getLongFromFile(syspath + "/cpufreq/cpuinfo_cur_freq");
                  }
               }

               if (max < freqs[cpu]) {
                  max = freqs[cpu];
               }
            }

            if (max > 0L) {
               for (int i = 0; i < freqs.length; i++) {
                  freqs[i] *= 1000L;
               }

               return freqs;
            }
         } finally {
            enumerate.unref();
         }
      } finally {
         udev.unref();
      }

      Arrays.fill(freqs, -1L);
      List var17 = FileUtil.readFile(ProcPath.CPUINFO);
      int var18 = 0;

      for (String s : var17) {
         if (s.toLowerCase().contains("cpu mhz")) {
            freqs[var18] = Math.round(ParseUtil.parseLastDouble(s, 0.0) * 1000000.0);
            if (++var18 >= freqs.length) {
               break;
            }
         }
      }

      return freqs;
   }

   @Override
   public long queryMaxFreq() {
      long max = Arrays.stream(this.getCurrentFreq()).max().orElse(-1L);
      if (max > 0L) {
         max /= 1000L;
      }

      Udev.UdevContext udev = Udev.INSTANCE.udev_new();

      try {
         Udev.UdevEnumerate enumerate = udev.enumerateNew();

         try {
            enumerate.addMatchSubsystem("cpu");
            enumerate.scanDevices();
            Udev.UdevListEntry entry = enumerate.getListEntry();
            if (entry != null) {
               String syspath = entry.getName();
               String cpuFreqPath = syspath.substring(0, syspath.lastIndexOf(File.separatorChar)) + "/cpuFreq";
               String policyPrefix = cpuFreqPath + "/policy";

               try {
                  Stream<Path> path = Files.list(Paths.get(cpuFreqPath));

                  try {
                     Optional<Long> maxPolicy = path.filter(p -> p.toString().startsWith(policyPrefix)).map(p -> {
                        long freq = FileUtil.getLongFromFile(p.toString() + "/scaling_max_freq");
                        if (freq == 0L) {
                           freq = FileUtil.getLongFromFile(p.toString() + "/cpuinfo_max_freq");
                        }

                        return freq;
                     }).max(Long::compare);
                     if (maxPolicy.isPresent() && max < maxPolicy.get()) {
                        max = maxPolicy.get();
                     }
                  } catch (Throwable var25) {
                     if (path != null) {
                        try {
                           path.close();
                        } catch (Throwable var24) {
                           var25.addSuppressed(var24);
                        }
                     }

                     throw var25;
                  }

                  if (path != null) {
                     path.close();
                  }
               } catch (IOException var26) {
               }
            }
         } finally {
            enumerate.unref();
         }
      } finally {
         udev.unref();
      }

      if (max == 0L) {
         return -1L;
      } else {
         max *= 1000L;
         long lshwMax = Lshw.queryCpuCapacity();
         return lshwMax > max ? lshwMax : max;
      }
   }

   @Override
   public double[] getSystemLoadAverage(int nelem) {
      if (nelem >= 1 && nelem <= 3) {
         double[] average = new double[nelem];
         int retval = LinuxLibc.INSTANCE.getloadavg(average, nelem);
         if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
               average[i] = -1.0;
            }
         }

         return average;
      } else {
         throw new IllegalArgumentException("Must include from one to three elements.");
      }
   }

   @Override
   public long[][] queryProcessorCpuLoadTicks() {
      long[][] ticks = CpuStat.getProcessorCpuLoadTicks(this.getLogicalProcessorCount());
      if (LongStream.of(ticks[0]).sum() == 0L) {
         ticks = CpuStat.getProcessorCpuLoadTicks(this.getLogicalProcessorCount());
      }

      long hz = LinuxOperatingSystem.getHz();

      for (int i = 0; i < ticks.length; i++) {
         for (int j = 0; j < ticks[i].length; j++) {
            ticks[i][j] = ticks[i][j] * 1000L / hz;
         }
      }

      return ticks;
   }

   private static String getProcessorID(String vendor, String stepping, String model, String family, String[] flags) {
      boolean procInfo = false;
      String marker = "Processor Information";

      for (String checkLine : ExecutingCommand.runNative("dmidecode -t 4")) {
         if (!procInfo && checkLine.contains(marker)) {
            marker = "ID:";
            procInfo = true;
         } else if (procInfo && checkLine.contains(marker)) {
            return checkLine.split(marker)[1].trim();
         }
      }

      marker = "eax=";

      for (String checkLinex : ExecutingCommand.runNative("cpuid -1r")) {
         if (checkLinex.contains(marker) && checkLinex.trim().startsWith("0x00000001")) {
            String eax = "";
            String edx = "";

            for (String register : ParseUtil.whitespaces.split(checkLinex)) {
               if (register.startsWith("eax=")) {
                  eax = ParseUtil.removeMatchingString(register, "eax=0x");
               } else if (register.startsWith("edx=")) {
                  edx = ParseUtil.removeMatchingString(register, "edx=0x");
               }
            }

            return edx + eax;
         }
      }

      return vendor.startsWith("0x") ? createMIDR(vendor, stepping, model, family) + "00000000" : createProcessorID(stepping, model, family, flags);
   }

   private static String createMIDR(String vendor, String stepping, String model, String family) {
      int midrBytes = 0;
      if (stepping.startsWith("r") && stepping.contains("p")) {
         String[] rev = stepping.substring(1).split("p");
         midrBytes |= ParseUtil.parseLastInt(rev[1], 0);
         midrBytes |= ParseUtil.parseLastInt(rev[0], 0) << 20;
      }

      midrBytes |= ParseUtil.parseLastInt(model, 0) << 4;
      midrBytes |= ParseUtil.parseLastInt(family, 0) << 16;
      midrBytes |= ParseUtil.parseLastInt(vendor, 0) << 24;
      return String.format("%08X", midrBytes);
   }

   @Override
   public long queryContextSwitches() {
      return CpuStat.getContextSwitches();
   }

   @Override
   public long queryInterrupts() {
      return CpuStat.getInterrupts();
   }
}
