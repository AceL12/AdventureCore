package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.platform.mac.SystemB;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.util.FormatUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class MacCentralProcessor extends AbstractCentralProcessor {
   private static final Logger LOG = LoggerFactory.getLogger(MacCentralProcessor.class);
   private static final int ARM_CPUTYPE = 16777228;
   private static final int M1_CPUFAMILY = 458787763;
   private static final int M2_CPUFAMILY = -634136515;
   private static final long DEFAULT_FREQUENCY = 2400000000L;
   private final Supplier<String> vendor = Memoizer.memoize(MacCentralProcessor::platformExpert);
   private final boolean isArmCpu = this.isArmCpu();
   private long performanceCoreFrequency = 2400000000L;
   private long efficiencyCoreFrequency = 2400000000L;

   @Override
   protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
      String cpuName = SysctlUtil.sysctl("machdep.cpu.brand_string", "");
      String cpuVendor;
      String cpuStepping;
      String cpuModel;
      String cpuFamily;
      String processorID;
      if (cpuName.startsWith("Apple")) {
         cpuVendor = this.vendor.get();
         cpuStepping = "0";
         cpuModel = "0";
         int type;
         int family;
         if (this.isArmCpu) {
            type = 16777228;
            family = cpuName.contains("M2") ? -634136515 : 458787763;
         } else {
            type = SysctlUtil.sysctl("hw.cputype", 0);
            family = SysctlUtil.sysctl("hw.cpufamily", 0);
         }

         cpuFamily = String.format("0x%08x", family);
         processorID = String.format("%08x%08x", type, family);
      } else {
         cpuVendor = SysctlUtil.sysctl("machdep.cpu.vendor", "");
         int i = SysctlUtil.sysctl("machdep.cpu.stepping", -1);
         cpuStepping = i < 0 ? "" : Integer.toString(i);
         i = SysctlUtil.sysctl("machdep.cpu.model", -1);
         cpuModel = i < 0 ? "" : Integer.toString(i);
         i = SysctlUtil.sysctl("machdep.cpu.family", -1);
         cpuFamily = i < 0 ? "" : Integer.toString(i);
         long processorIdBits = 0L;
         processorIdBits |= SysctlUtil.sysctl("machdep.cpu.signature", 0);
         processorIdBits |= (SysctlUtil.sysctl("machdep.cpu.feature_bits", 0L) & -1L) << 32;
         processorID = String.format("%016x", processorIdBits);
      }

      if (this.isArmCpu) {
         this.calculateNominalFrequencies();
      }

      long cpuFreq = this.isArmCpu ? this.performanceCoreFrequency : SysctlUtil.sysctl("hw.cpufrequency", 0L);
      boolean cpu64bit = SysctlUtil.sysctl("hw.cpu64bit_capable", 0) != 0;
      return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuFreq);
   }

   @Override
   protected Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> initProcessorCounts() {
      int logicalProcessorCount = SysctlUtil.sysctl("hw.logicalcpu", 1);
      int physicalProcessorCount = SysctlUtil.sysctl("hw.physicalcpu", 1);
      int physicalPackageCount = SysctlUtil.sysctl("hw.packages", 1);
      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>(logicalProcessorCount);
      Set<Integer> pkgCoreKeys = new HashSet<>();

      for (int i = 0; i < logicalProcessorCount; i++) {
         int coreId = i * physicalProcessorCount / logicalProcessorCount;
         int pkgId = i * physicalPackageCount / logicalProcessorCount;
         logProcs.add(new CentralProcessor.LogicalProcessor(i, coreId, pkgId));
         pkgCoreKeys.add((pkgId << 16) + coreId);
      }

      Map<Integer, String> compatMap = queryCompatibleStrings();
      int perflevels = SysctlUtil.sysctl("hw.nperflevels", 1);
      List<CentralProcessor.PhysicalProcessor> physProcs = pkgCoreKeys.stream().sorted().map(k -> {
         String compat = compatMap.getOrDefault(k, "").toLowerCase();
         int efficiency = 0;
         if (compat.contains("firestorm") || compat.contains("avalanche")) {
            efficiency = 1;
         }

         return new CentralProcessor.PhysicalProcessor(k >> 16, k & 65535, efficiency, compat);
      }).collect(Collectors.toList());
      List<CentralProcessor.ProcessorCache> caches = orderedProcCaches(this.getCacheValues(perflevels));
      return new Triplet<>(logProcs, physProcs, caches);
   }

   private Set<CentralProcessor.ProcessorCache> getCacheValues(int perflevels) {
      int linesize = (int)SysctlUtil.sysctl("hw.cachelinesize", 0L);
      int l1associativity = SysctlUtil.sysctl("machdep.cpu.cache.L1_associativity", 0, false);
      int l2associativity = SysctlUtil.sysctl("machdep.cpu.cache.L2_associativity", 0, false);
      Set<CentralProcessor.ProcessorCache> caches = new HashSet<>();

      for (int i = 0; i < perflevels; i++) {
         int size = SysctlUtil.sysctl("hw.perflevel" + i + ".l1icachesize", 0, false);
         if (size > 0) {
            caches.add(new CentralProcessor.ProcessorCache(1, l1associativity, linesize, (long)size, CentralProcessor.ProcessorCache.Type.INSTRUCTION));
         }

         size = SysctlUtil.sysctl("hw.perflevel" + i + ".l1dcachesize", 0, false);
         if (size > 0) {
            caches.add(new CentralProcessor.ProcessorCache(1, l1associativity, linesize, (long)size, CentralProcessor.ProcessorCache.Type.DATA));
         }

         size = SysctlUtil.sysctl("hw.perflevel" + i + ".l2cachesize", 0, false);
         if (size > 0) {
            caches.add(new CentralProcessor.ProcessorCache(2, l2associativity, linesize, (long)size, CentralProcessor.ProcessorCache.Type.UNIFIED));
         }

         size = SysctlUtil.sysctl("hw.perflevel" + i + ".l3cachesize", 0, false);
         if (size > 0) {
            caches.add(new CentralProcessor.ProcessorCache(3, 0, linesize, (long)size, CentralProcessor.ProcessorCache.Type.UNIFIED));
         }
      }

      return caches;
   }

   @Override
   public long[] querySystemCpuLoadTicks() {
      long[] ticks = new long[CentralProcessor.TickType.values().length];
      int machPort = SystemB.INSTANCE.mach_host_self();
      Struct.CloseableHostCpuLoadInfo cpuLoadInfo = new Struct.CloseableHostCpuLoadInfo();

      long[] var5;
      label50: {
         try {
            ByRef.CloseableIntByReference size = new ByRef.CloseableIntByReference(cpuLoadInfo.size());

            label46: {
               try {
                  if (0 == SystemB.INSTANCE.host_statistics(machPort, 3, cpuLoadInfo, size)) {
                     ticks[CentralProcessor.TickType.USER.getIndex()] = cpuLoadInfo.cpu_ticks[0];
                     ticks[CentralProcessor.TickType.NICE.getIndex()] = cpuLoadInfo.cpu_ticks[3];
                     ticks[CentralProcessor.TickType.SYSTEM.getIndex()] = cpuLoadInfo.cpu_ticks[1];
                     ticks[CentralProcessor.TickType.IDLE.getIndex()] = cpuLoadInfo.cpu_ticks[2];
                     break label46;
                  }

                  LOG.error("Failed to get System CPU ticks. Error code: {} ", Native.getLastError());
                  var5 = ticks;
               } catch (Throwable var9) {
                  try {
                     size.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               size.close();
               break label50;
            }

            size.close();
         } catch (Throwable var10) {
            try {
               cpuLoadInfo.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }

            throw var10;
         }

         cpuLoadInfo.close();
         return ticks;
      }

      cpuLoadInfo.close();
      return var5;
   }

   @Override
   public long[] queryCurrentFreq() {
      if (this.isArmCpu) {
         Map<Integer, Long> physFreqMap = new HashMap<>();
         this.getPhysicalProcessors()
            .stream()
            .forEach(p -> physFreqMap.put(p.getPhysicalProcessorNumber(), p.getEfficiency() > 0 ? this.performanceCoreFrequency : this.efficiencyCoreFrequency));
         return this.getLogicalProcessors()
            .stream()
            .map(CentralProcessor.LogicalProcessor::getPhysicalProcessorNumber)
            .map(p -> physFreqMap.getOrDefault(p, this.performanceCoreFrequency))
            .mapToLong(f -> f)
            .toArray();
      } else {
         return new long[]{this.getProcessorIdentifier().getVendorFreq()};
      }
   }

   @Override
   public long queryMaxFreq() {
      return this.isArmCpu ? this.performanceCoreFrequency : SysctlUtil.sysctl("hw.cpufrequency_max", this.getProcessorIdentifier().getVendorFreq());
   }

   @Override
   public double[] getSystemLoadAverage(int nelem) {
      if (nelem >= 1 && nelem <= 3) {
         double[] average = new double[nelem];
         int retval = SystemB.INSTANCE.getloadavg(average, nelem);
         if (retval < nelem) {
            Arrays.fill(average, -1.0);
         }

         return average;
      } else {
         throw new IllegalArgumentException("Must include from one to three elements.");
      }
   }

   @Override
   public long[][] queryProcessorCpuLoadTicks() {
      long[][] ticks = new long[this.getLogicalProcessorCount()][CentralProcessor.TickType.values().length];
      int machPort = SystemB.INSTANCE.mach_host_self();
      ByRef.CloseableIntByReference procCount = new ByRef.CloseableIntByReference();

      long[][] var15;
      label75: {
         try {
            ByRef.CloseablePointerByReference procCpuLoadInfo;
            label79: {
               procCpuLoadInfo = new ByRef.CloseablePointerByReference();

               try {
                  ByRef.CloseableIntByReference procInfoCount = new ByRef.CloseableIntByReference();

                  label67: {
                     try {
                        if (0 != SystemB.INSTANCE.host_processor_info(machPort, 2, procCount, procCpuLoadInfo, procInfoCount)) {
                           LOG.error("Failed to update CPU Load. Error code: {}", Native.getLastError());
                           var15 = ticks;
                           break label67;
                        }

                        int[] cpuTicks = procCpuLoadInfo.getValue().getIntArray(0L, procInfoCount.getValue());

                        for (int cpu = 0; cpu < procCount.getValue(); cpu++) {
                           int offset = cpu * 4;
                           ticks[cpu][CentralProcessor.TickType.USER.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + 0]);
                           ticks[cpu][CentralProcessor.TickType.NICE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + 3]);
                           ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + 1]);
                           ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] = FormatUtil.getUnsignedInt(cpuTicks[offset + 2]);
                        }
                     } catch (Throwable var12) {
                        try {
                           procInfoCount.close();
                        } catch (Throwable var11) {
                           var12.addSuppressed(var11);
                        }

                        throw var12;
                     }

                     procInfoCount.close();
                     break label79;
                  }

                  procInfoCount.close();
               } catch (Throwable var13) {
                  try {
                     procCpuLoadInfo.close();
                  } catch (Throwable var10) {
                     var13.addSuppressed(var10);
                  }

                  throw var13;
               }

               procCpuLoadInfo.close();
               break label75;
            }

            procCpuLoadInfo.close();
         } catch (Throwable var14) {
            try {
               procCount.close();
            } catch (Throwable var9) {
               var14.addSuppressed(var9);
            }

            throw var14;
         }

         procCount.close();
         return ticks;
      }

      procCount.close();
      return var15;
   }

   @Override
   public long queryContextSwitches() {
      return 0L;
   }

   @Override
   public long queryInterrupts() {
      return 0L;
   }

   private static String platformExpert() {
      String manufacturer = null;
      IOKit.IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
      if (platformExpert != null) {
         byte[] data = platformExpert.getByteArrayProperty("manufacturer");
         if (data != null) {
            manufacturer = Native.toString(data, StandardCharsets.UTF_8);
         }

         platformExpert.release();
      }

      return Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer;
   }

   private static Map<Integer, String> queryCompatibleStrings() {
      Map<Integer, String> compatibleStrMap = new HashMap<>();
      IOKit.IOIterator iter = IOKitUtil.getMatchingServices("IOPlatformDevice");
      if (iter != null) {
         for (IOKit.IORegistryEntry cpu = iter.next(); cpu != null; cpu = iter.next()) {
            if (cpu.getName().toLowerCase().startsWith("cpu")) {
               int procId = ParseUtil.getFirstIntValue(cpu.getName());
               byte[] data = cpu.getByteArrayProperty("compatible");
               if (data != null) {
                  compatibleStrMap.put(procId, new String(data, StandardCharsets.UTF_8).replace('\u0000', ' '));
               }
            }

            cpu.release();
         }

         iter.release();
      }

      return compatibleStrMap;
   }

   private boolean isArmCpu() {
      return this.getPhysicalProcessors().stream().map(CentralProcessor.PhysicalProcessor::getEfficiency).anyMatch(e -> e > 0);
   }

   private void calculateNominalFrequencies() {
      IOKit.IOIterator iter = IOKitUtil.getMatchingServices("AppleARMIODevice");
      if (iter != null) {
         try {
            IOKit.IORegistryEntry device = iter.next();

            try {
               while (device != null) {
                  if (device.getName().toLowerCase().equals("pmgr")) {
                     this.performanceCoreFrequency = this.getMaxFreqFromByteArray(device.getByteArrayProperty("voltage-states5-sram"));
                     this.efficiencyCoreFrequency = this.getMaxFreqFromByteArray(device.getByteArrayProperty("voltage-states1-sram"));
                     return;
                  }

                  device.release();
                  device = iter.next();
               }
            } finally {
               if (device != null) {
                  device.release();
               }
            }
         } finally {
            iter.release();
         }
      }
   }

   private long getMaxFreqFromByteArray(byte[] data) {
      if (data != null && data.length >= 8) {
         byte[] freqData = Arrays.copyOfRange(data, data.length - 8, data.length - 4);
         return ParseUtil.byteArrayToLong(freqData, 4, false);
      } else {
         return 2400000000L;
      }
   }
}
