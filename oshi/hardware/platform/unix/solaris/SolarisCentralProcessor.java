package oshi.hardware.platform.unix.solaris;

import com.sun.jna.platform.unix.solaris.LibKstat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class SolarisCentralProcessor extends AbstractCentralProcessor {
   private static final String KSTAT_SYSTEM_CPU = "kstat:/system/cpu/";
   private static final String INFO = "/info";
   private static final String SYS = "/sys";
   private static final String KSTAT_PM_CPU = "kstat:/pm/cpu/";
   private static final String PSTATE = "/pstate";
   private static final String CPU_INFO = "cpu_info";

   @Override
   protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
      boolean cpu64bit = "64".equals(ExecutingCommand.getFirstAnswer("isainfo -b").trim());
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return queryProcessorId2(cpu64bit);
      } else {
         String cpuVendor = "";
         String cpuName = "";
         String cpuFamily = "";
         String cpuModel = "";
         String cpuStepping = "";
         long cpuFreq = 0L;
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         try {
            LibKstat.Kstat ksp = kc.lookup("cpu_info", -1, null);
            if (ksp != null && kc.read(ksp)) {
               cpuVendor = KstatUtil.dataLookupString(ksp, "vendor_id");
               cpuName = KstatUtil.dataLookupString(ksp, "brand");
               cpuFamily = KstatUtil.dataLookupString(ksp, "family");
               cpuModel = KstatUtil.dataLookupString(ksp, "model");
               cpuStepping = KstatUtil.dataLookupString(ksp, "stepping");
               cpuFreq = KstatUtil.dataLookupLong(ksp, "clock_MHz") * 1000000L;
            }
         } catch (Throwable var13) {
            if (kc != null) {
               try {
                  kc.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }
            }

            throw var13;
         }

         if (kc != null) {
            kc.close();
         }

         String processorID = getProcessorID(cpuStepping, cpuModel, cpuFamily);
         return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuFreq);
      }
   }

   private static CentralProcessor.ProcessorIdentifier queryProcessorId2(boolean cpu64bit) {
      Object[] results = KstatUtil.queryKstat2("kstat:/system/cpu/0/info", "vendor_id", "brand", "family", "model", "stepping", "clock_MHz");
      String cpuVendor = results[0] == null ? "" : (String)results[0];
      String cpuName = results[1] == null ? "" : (String)results[1];
      String cpuFamily = results[2] == null ? "" : results[2].toString();
      String cpuModel = results[3] == null ? "" : results[3].toString();
      String cpuStepping = results[4] == null ? "" : results[4].toString();
      long cpuFreq = results[5] == null ? 0L : (Long)results[5];
      String processorID = getProcessorID(cpuStepping, cpuModel, cpuFamily);
      return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuFreq);
   }

   @Override
   protected Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> initProcessorCounts() {
      Map<Integer, Integer> numaNodeMap = mapNumaNodes();
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return new Triplet<>(initProcessorCounts2(numaNodeMap), null, null);
      } else {
         List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         try {
            for (LibKstat.Kstat ksp : kc.lookupAll("cpu_info", -1, null)) {
               if (ksp != null && kc.read(ksp)) {
                  int procId = logProcs.size();
                  String chipId = KstatUtil.dataLookupString(ksp, "chip_id");
                  String coreId = KstatUtil.dataLookupString(ksp, "core_id");
                  CentralProcessor.LogicalProcessor logProc = new CentralProcessor.LogicalProcessor(
                     procId, ParseUtil.parseIntOrDefault(coreId, 0), ParseUtil.parseIntOrDefault(chipId, 0), numaNodeMap.getOrDefault(procId, 0)
                  );
                  logProcs.add(logProc);
               }
            }
         } catch (Throwable var12) {
            if (kc != null) {
               try {
                  kc.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (kc != null) {
            kc.close();
         }

         if (logProcs.isEmpty()) {
            logProcs.add(new CentralProcessor.LogicalProcessor(0, 0, 0));
         }

         Map<Integer, String> dmesg = new HashMap<>();
         Pattern p = Pattern.compile(".* cpu(\\\\d+): ((ARM|AMD|Intel).+)");

         for (String s : ExecutingCommand.runNative("dmesg")) {
            Matcher m = p.matcher(s);
            if (m.matches()) {
               int coreId = ParseUtil.parseIntOrDefault(m.group(1), 0);
               dmesg.put(coreId, m.group(2).trim());
            }
         }

         return dmesg.isEmpty() ? new Triplet<>(logProcs, null, null) : new Triplet<>(logProcs, this.createProcListFromDmesg(logProcs, dmesg), null);
      }
   }

   private static List<CentralProcessor.LogicalProcessor> initProcessorCounts2(Map<Integer, Integer> numaNodeMap) {
      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();

      for (Object[] result : KstatUtil.queryKstat2List("kstat:/system/cpu/", "/info", "chip_id", "core_id")) {
         int procId = logProcs.size();
         long chipId = result[0] == null ? 0L : (Long)result[0];
         long coreId = result[1] == null ? 0L : (Long)result[1];
         CentralProcessor.LogicalProcessor logProc = new CentralProcessor.LogicalProcessor(
            procId, (int)coreId, (int)chipId, numaNodeMap.getOrDefault(procId, 0)
         );
         logProcs.add(logProc);
      }

      if (logProcs.isEmpty()) {
         logProcs.add(new CentralProcessor.LogicalProcessor(0, 0, 0));
      }

      return logProcs;
   }

   private static Map<Integer, Integer> mapNumaNodes() {
      Map<Integer, Integer> numaNodeMap = new HashMap<>();
      int lgroup = 0;

      for (String line : ExecutingCommand.runNative("lgrpinfo -c leaves")) {
         if (line.startsWith("lgroup")) {
            lgroup = ParseUtil.getFirstIntValue(line);
         } else if (line.contains("CPUs:") || line.contains("CPU:")) {
            for (Integer cpu : ParseUtil.parseHyphenatedIntList(line.split(":")[1])) {
               numaNodeMap.put(cpu, lgroup);
            }
         }
      }

      return numaNodeMap;
   }

   @Override
   public long[] querySystemCpuLoadTicks() {
      long[] ticks = new long[CentralProcessor.TickType.values().length];
      long[][] procTicks = this.getProcessorCpuLoadTicks();

      for (int i = 0; i < ticks.length; i++) {
         for (long[] procTick : procTicks) {
            ticks[i] += procTick[i];
         }

         ticks[i] /= procTicks.length;
      }

      return ticks;
   }

   @Override
   public long[] queryCurrentFreq() {
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return queryCurrentFreq2(this.getLogicalProcessorCount());
      } else {
         long[] freqs = new long[this.getLogicalProcessorCount()];
         Arrays.fill(freqs, -1L);
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         try {
            for (int i = 0; i < freqs.length; i++) {
               for (LibKstat.Kstat ksp : kc.lookupAll("cpu_info", i, null)) {
                  if (ksp != null && kc.read(ksp)) {
                     freqs[i] = KstatUtil.dataLookupLong(ksp, "current_clock_Hz");
                  }
               }
            }
         } catch (Throwable var7) {
            if (kc != null) {
               try {
                  kc.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (kc != null) {
            kc.close();
         }

         return freqs;
      }
   }

   private static long[] queryCurrentFreq2(int processorCount) {
      long[] freqs = new long[processorCount];
      Arrays.fill(freqs, -1L);
      List<Object[]> results = KstatUtil.queryKstat2List("kstat:/system/cpu/", "/info", "current_clock_Hz");
      int cpu = -1;

      for (Object[] result : results) {
         if (++cpu >= freqs.length) {
            break;
         }

         freqs[cpu] = result[0] == null ? -1L : (Long)result[0];
      }

      return freqs;
   }

   @Override
   public long queryMaxFreq() {
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return queryMaxFreq2();
      } else {
         long max = -1L;
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         try {
            for (LibKstat.Kstat ksp : kc.lookupAll("cpu_info", 0, null)) {
               if (kc.read(ksp)) {
                  String suppFreq = KstatUtil.dataLookupString(ksp, "supported_frequencies_Hz");
                  if (!suppFreq.isEmpty()) {
                     for (String s : suppFreq.split(":")) {
                        long freq = ParseUtil.parseLongOrDefault(s, -1L);
                        if (max < freq) {
                           max = freq;
                        }
                     }
                  }
               }
            }
         } catch (Throwable var14) {
            if (kc != null) {
               try {
                  kc.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (kc != null) {
            kc.close();
         }

         return max;
      }
   }

   private static long queryMaxFreq2() {
      long max = -1L;

      for (Object[] result : KstatUtil.queryKstat2List("kstat:/pm/cpu/", "/pstate", "supported_frequencies")) {
         for (long freq : result[0] == null ? new long[0] : (long[])result[0]) {
            if (freq > max) {
               max = freq;
            }
         }
      }

      return max;
   }

   @Override
   public double[] getSystemLoadAverage(int nelem) {
      if (nelem >= 1 && nelem <= 3) {
         double[] average = new double[nelem];
         int retval = SolarisLibc.INSTANCE.getloadavg(average, nelem);
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
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return queryProcessorCpuLoadTicks2(this.getLogicalProcessorCount());
      } else {
         long[][] ticks = new long[this.getLogicalProcessorCount()][CentralProcessor.TickType.values().length];
         int cpu = -1;
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         try {
            for (LibKstat.Kstat ksp : kc.lookupAll("cpu", -1, "sys")) {
               if (++cpu >= ticks.length) {
                  break;
               }

               if (kc.read(ksp)) {
                  ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] = KstatUtil.dataLookupLong(ksp, "cpu_ticks_idle");
                  ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] = KstatUtil.dataLookupLong(ksp, "cpu_ticks_kernel");
                  ticks[cpu][CentralProcessor.TickType.USER.getIndex()] = KstatUtil.dataLookupLong(ksp, "cpu_ticks_user");
               }
            }
         } catch (Throwable var7) {
            if (kc != null) {
               try {
                  kc.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (kc != null) {
            kc.close();
         }

         return ticks;
      }
   }

   private static long[][] queryProcessorCpuLoadTicks2(int processorCount) {
      long[][] ticks = new long[processorCount][CentralProcessor.TickType.values().length];
      List<Object[]> results = KstatUtil.queryKstat2List("kstat:/system/cpu/", "/sys", "cpu_ticks_idle", "cpu_ticks_kernel", "cpu_ticks_user");
      int cpu = -1;

      for (Object[] result : results) {
         if (++cpu >= ticks.length) {
            break;
         }

         ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] = result[0] == null ? 0L : (Long)result[0];
         ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] = result[1] == null ? 0L : (Long)result[1];
         ticks[cpu][CentralProcessor.TickType.USER.getIndex()] = result[2] == null ? 0L : (Long)result[2];
      }

      return ticks;
   }

   private static String getProcessorID(String stepping, String model, String family) {
      List<String> isainfo = ExecutingCommand.runNative("isainfo -v");
      StringBuilder flags = new StringBuilder();

      for (String line : isainfo) {
         if (line.startsWith("32-bit")) {
            break;
         }

         if (!line.startsWith("64-bit")) {
            flags.append(' ').append(line.trim());
         }
      }

      return createProcessorID(stepping, model, family, ParseUtil.whitespaces.split(flags.toString().toLowerCase()));
   }

   @Override
   public long queryContextSwitches() {
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return queryContextSwitches2();
      } else {
         long swtch = 0L;

         for (String s : ExecutingCommand.runNative("kstat -p cpu_stat:::/pswitch\\\\|inv_swtch/")) {
            swtch += ParseUtil.parseLastLong(s, 0L);
         }

         return swtch;
      }
   }

   private static long queryContextSwitches2() {
      long swtch = 0L;

      for (Object[] result : KstatUtil.queryKstat2List("kstat:/system/cpu/", "/sys", "pswitch", "inv_swtch")) {
         swtch += result[0] == null ? 0L : result[0];
         swtch += result[1] == null ? 0L : result[1];
      }

      return swtch;
   }

   @Override
   public long queryInterrupts() {
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return queryInterrupts2();
      } else {
         long intr = 0L;

         for (String s : ExecutingCommand.runNative("kstat -p cpu_stat:::/intr/")) {
            intr += ParseUtil.parseLastLong(s, 0L);
         }

         return intr;
      }
   }

   private static long queryInterrupts2() {
      long intr = 0L;

      for (Object[] result : KstatUtil.queryKstat2List("kstat:/system/cpu/", "/sys", "intr")) {
         intr += result[0] == null ? 0L : result[0];
      }

      return intr;
   }
}
