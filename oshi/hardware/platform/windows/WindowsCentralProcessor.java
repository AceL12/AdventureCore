package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.LogicalProcessorInformation;
import oshi.driver.windows.perfmon.LoadAverage;
import oshi.driver.windows.perfmon.ProcessorInformation;
import oshi.driver.windows.perfmon.SystemInformation;
import oshi.driver.windows.wmi.Win32Processor;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.Struct;
import oshi.jna.platform.windows.PowrProf;
import oshi.util.GlobalConfig;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class WindowsCentralProcessor extends AbstractCentralProcessor {
   private static final Logger LOG = LoggerFactory.getLogger(WindowsCentralProcessor.class);
   private Map<String, Integer> numaNodeProcToLogicalProcMap;
   private static final boolean USE_LOAD_AVERAGE = GlobalConfig.get("oshi.os.windows.loadaverage", false);
   private static final boolean USE_CPU_UTILITY;
   private final Supplier<Pair<List<String>, Map<ProcessorInformation.ProcessorUtilityTickCountProperty, List<Long>>>> processorUtilityCounters;
   private Map<ProcessorInformation.ProcessorUtilityTickCountProperty, List<Long>> initialUtilityCounters;
   private Long utilityBaseMultiplier;

   WindowsCentralProcessor() {
      this.processorUtilityCounters = USE_CPU_UTILITY
         ? Memoizer.memoize(WindowsCentralProcessor::queryProcessorUtilityCounters, TimeUnit.MILLISECONDS.toNanos(300L))
         : null;
      this.initialUtilityCounters = USE_CPU_UTILITY ? this.processorUtilityCounters.get().getB() : null;
      this.utilityBaseMultiplier = null;
   }

   @Override
   protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
      String cpuVendor = "";
      String cpuName = "";
      String cpuIdentifier = "";
      String cpuFamily = "";
      String cpuModel = "";
      String cpuStepping = "";
      long cpuVendorFreq = 0L;
      boolean cpu64bit = false;
      String cpuRegistryRoot = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\";
      String[] processorIds = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\");
      if (processorIds.length > 0) {
         String cpuRegistryPath = "HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\" + processorIds[0];
         cpuVendor = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "VendorIdentifier");
         cpuName = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "ProcessorNameString");
         cpuIdentifier = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "Identifier");

         try {
            cpuVendorFreq = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, cpuRegistryPath, "~MHz") * 1000000L;
         } catch (Win32Exception var17) {
         }
      }

      if (!cpuIdentifier.isEmpty()) {
         cpuFamily = parseIdentifier(cpuIdentifier, "Family");
         cpuModel = parseIdentifier(cpuIdentifier, "Model");
         cpuStepping = parseIdentifier(cpuIdentifier, "Stepping");
      }

      Struct.CloseableSystemInfo sysinfo = new Struct.CloseableSystemInfo();

      try {
         Kernel32.INSTANCE.GetNativeSystemInfo(sysinfo);
         int processorArchitecture = sysinfo.processorArchitecture.pi.wProcessorArchitecture.intValue();
         if (processorArchitecture == 9 || processorArchitecture == 12 || processorArchitecture == 6) {
            cpu64bit = true;
         }
      } catch (Throwable var18) {
         try {
            sysinfo.close();
         } catch (Throwable var16) {
            var18.addSuppressed(var16);
         }

         throw var18;
      }

      sysinfo.close();
      WbemcliUtil.WmiResult<Win32Processor.ProcessorIdProperty> processorId = Win32Processor.queryProcessorId();
      String processorID;
      if (processorId.getResultCount() > 0) {
         processorID = WmiUtil.getString(processorId, Win32Processor.ProcessorIdProperty.PROCESSORID, 0);
      } else {
         processorID = createProcessorID(cpuStepping, cpuModel, cpuFamily, cpu64bit ? new String[]{"ia64"} : new String[0]);
      }

      return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuVendorFreq);
   }

   private static String parseIdentifier(String identifier, String key) {
      String[] idSplit = ParseUtil.whitespaces.split(identifier);
      boolean found = false;

      for (String s : idSplit) {
         if (found) {
            return s;
         }

         found = s.equals(key);
      }

      return "";
   }

   @Override
   protected Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> initProcessorCounts() {
      if (VersionHelpers.IsWindows7OrGreater()) {
         Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> procs = LogicalProcessorInformation.getLogicalProcessorInformationEx();
         int curNode = -1;
         int procNum = 0;
         int lp = 0;
         this.numaNodeProcToLogicalProcMap = new HashMap<>();

         for (CentralProcessor.LogicalProcessor logProc : procs.getA()) {
            int node = logProc.getNumaNode();
            if (node != curNode) {
               curNode = node;
               procNum = 0;
            }

            this.numaNodeProcToLogicalProcMap.put(String.format("%d,%d", logProc.getNumaNode(), procNum++), lp++);
         }

         return procs;
      } else {
         return LogicalProcessorInformation.getLogicalProcessorInformation();
      }
   }

   @Override
   public long[] querySystemCpuLoadTicks() {
      long[] ticks = new long[CentralProcessor.TickType.values().length];
      long[][] procTicks = this.getProcessorCpuLoadTicks();

      for (int i = 0; i < ticks.length; i++) {
         for (long[] procTick : procTicks) {
            ticks[i] += procTick[i];
         }
      }

      return ticks;
   }

   @Override
   public long[] queryCurrentFreq() {
      if (VersionHelpers.IsWindows7OrGreater()) {
         Pair<List<String>, Map<ProcessorInformation.ProcessorFrequencyProperty, List<Long>>> instanceValuePair = ProcessorInformation.queryFrequencyCounters();
         List<String> instances = instanceValuePair.getA();
         Map<ProcessorInformation.ProcessorFrequencyProperty, List<Long>> valueMap = instanceValuePair.getB();
         List<Long> percentMaxList = valueMap.get(ProcessorInformation.ProcessorFrequencyProperty.PERCENTOFMAXIMUMFREQUENCY);
         if (!instances.isEmpty()) {
            long maxFreq = this.getMaxFreq();
            long[] freqs = new long[this.getLogicalProcessorCount()];

            for (String instance : instances) {
               int cpu = instance.contains(",") ? this.numaNodeProcToLogicalProcMap.getOrDefault(instance, 0) : ParseUtil.parseIntOrDefault(instance, 0);
               if (cpu < this.getLogicalProcessorCount()) {
                  freqs[cpu] = percentMaxList.get(cpu) * maxFreq / 100L;
               }
            }

            return freqs;
         }
      }

      return this.queryNTPower(2);
   }

   @Override
   public long queryMaxFreq() {
      long[] freqs = this.queryNTPower(1);
      return Arrays.stream(freqs).max().orElse(-1L);
   }

   private long[] queryNTPower(int fieldIndex) {
      PowrProf.ProcessorPowerInformation ppi = new PowrProf.ProcessorPowerInformation();
      PowrProf.ProcessorPowerInformation[] ppiArray = (PowrProf.ProcessorPowerInformation[])ppi.toArray(this.getLogicalProcessorCount());
      long[] freqs = new long[this.getLogicalProcessorCount()];
      if (0 != PowrProf.INSTANCE.CallNtPowerInformation(11, null, 0, ppiArray[0].getPointer(), ppi.size() * ppiArray.length)) {
         LOG.error("Unable to get Processor Information");
         Arrays.fill(freqs, -1L);
         return freqs;
      } else {
         for (int i = 0; i < freqs.length; i++) {
            if (fieldIndex == 1) {
               freqs[i] = ppiArray[i].maxMhz * 1000000L;
            } else if (fieldIndex == 2) {
               freqs[i] = ppiArray[i].currentMhz * 1000000L;
            } else {
               freqs[i] = -1L;
            }
         }

         return freqs;
      }
   }

   @Override
   public double[] getSystemLoadAverage(int nelem) {
      if (nelem >= 1 && nelem <= 3) {
         return LoadAverage.queryLoadAverage(nelem);
      } else {
         throw new IllegalArgumentException("Must include from one to three elements.");
      }
   }

   @Override
   public long[][] queryProcessorCpuLoadTicks() {
      List<Long> baseList = null;
      List<Long> systemUtility = null;
      List<Long> processorUtility = null;
      List<Long> processorUtilityBase = null;
      List<Long> initSystemList = null;
      List<Long> initUserList = null;
      List<Long> initBase = null;
      List<Long> initSystemUtility = null;
      List<Long> initProcessorUtility = null;
      List<Long> initProcessorUtilityBase = null;
      List<String> instances;
      List<Long> systemList;
      List<Long> userList;
      List<Long> irqList;
      List<Long> softIrqList;
      List<Long> idleList;
      if (USE_CPU_UTILITY) {
         Pair<List<String>, Map<ProcessorInformation.ProcessorUtilityTickCountProperty, List<Long>>> instanceValuePair = this.processorUtilityCounters.get();
         instances = instanceValuePair.getA();
         Map<ProcessorInformation.ProcessorUtilityTickCountProperty, List<Long>> valueMap = instanceValuePair.getB();
         systemList = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDTIME);
         userList = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTUSERTIME);
         irqList = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTINTERRUPTTIME);
         softIrqList = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTDPCTIME);
         idleList = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPROCESSORTIME);
         baseList = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.TIMESTAMP_SYS100NS);
         systemUtility = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDUTILITY);
         processorUtility = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY);
         processorUtilityBase = valueMap.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY_BASE);
         initSystemList = this.initialUtilityCounters.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDTIME);
         initUserList = this.initialUtilityCounters.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTUSERTIME);
         initBase = this.initialUtilityCounters.get(ProcessorInformation.ProcessorUtilityTickCountProperty.TIMESTAMP_SYS100NS);
         initSystemUtility = this.initialUtilityCounters.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPRIVILEGEDUTILITY);
         initProcessorUtility = this.initialUtilityCounters.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY);
         initProcessorUtilityBase = this.initialUtilityCounters.get(ProcessorInformation.ProcessorUtilityTickCountProperty.PERCENTPROCESSORUTILITY_BASE);
      } else {
         Pair<List<String>, Map<ProcessorInformation.ProcessorTickCountProperty, List<Long>>> instanceValuePair = ProcessorInformation.queryProcessorCounters();
         instances = instanceValuePair.getA();
         Map<ProcessorInformation.ProcessorTickCountProperty, List<Long>> valueMap = instanceValuePair.getB();
         systemList = valueMap.get(ProcessorInformation.ProcessorTickCountProperty.PERCENTPRIVILEGEDTIME);
         userList = valueMap.get(ProcessorInformation.ProcessorTickCountProperty.PERCENTUSERTIME);
         irqList = valueMap.get(ProcessorInformation.ProcessorTickCountProperty.PERCENTINTERRUPTTIME);
         softIrqList = valueMap.get(ProcessorInformation.ProcessorTickCountProperty.PERCENTDPCTIME);
         idleList = valueMap.get(ProcessorInformation.ProcessorTickCountProperty.PERCENTPROCESSORTIME);
      }

      int ncpu = this.getLogicalProcessorCount();
      long[][] ticks = new long[ncpu][CentralProcessor.TickType.values().length];
      if (!instances.isEmpty()
         && systemList != null
         && userList != null
         && irqList != null
         && softIrqList != null
         && idleList != null
         && (
            !USE_CPU_UTILITY
               || baseList != null
                  && systemUtility != null
                  && processorUtility != null
                  && processorUtilityBase != null
                  && initSystemList != null
                  && initUserList != null
                  && initBase != null
                  && initSystemUtility != null
                  && initProcessorUtility != null
                  && initProcessorUtilityBase != null
         )) {
         for (String instance : instances) {
            int cpu = instance.contains(",") ? this.numaNodeProcToLogicalProcMap.getOrDefault(instance, 0) : ParseUtil.parseIntOrDefault(instance, 0);
            if (cpu < ncpu) {
               ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] = systemList.get(cpu);
               ticks[cpu][CentralProcessor.TickType.USER.getIndex()] = userList.get(cpu);
               ticks[cpu][CentralProcessor.TickType.IRQ.getIndex()] = irqList.get(cpu);
               ticks[cpu][CentralProcessor.TickType.SOFTIRQ.getIndex()] = softIrqList.get(cpu);
               ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] = idleList.get(cpu);
               if (USE_CPU_UTILITY) {
                  long deltaT = baseList.get(cpu) - initBase.get(cpu);
                  if (deltaT > 0L) {
                     long deltaBase = processorUtilityBase.get(cpu) - initProcessorUtilityBase.get(cpu);
                     long multiplier = this.lazilyCalculateMultiplier(deltaBase, deltaT);
                     if (multiplier > 0L) {
                        long deltaProc = processorUtility.get(cpu) - initProcessorUtility.get(cpu);
                        long deltaSys = systemUtility.get(cpu) - initSystemUtility.get(cpu);
                        long newUser = initUserList.get(cpu) + multiplier * (deltaProc - deltaSys) / 100L;
                        long newSystem = initSystemList.get(cpu) + multiplier * deltaSys / 100L;
                        long delta = newUser - ticks[cpu][CentralProcessor.TickType.USER.getIndex()];
                        ticks[cpu][CentralProcessor.TickType.USER.getIndex()] = newUser;
                        delta += newSystem - ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()];
                        ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] = newSystem;
                        ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] -= delta;
                     }
                  }
               }

               ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] -= ticks[cpu][CentralProcessor.TickType.IRQ.getIndex()]
                  + ticks[cpu][CentralProcessor.TickType.SOFTIRQ.getIndex()];
               ticks[cpu][CentralProcessor.TickType.SYSTEM.getIndex()] /= 10000L;
               ticks[cpu][CentralProcessor.TickType.USER.getIndex()] /= 10000L;
               ticks[cpu][CentralProcessor.TickType.IRQ.getIndex()] /= 10000L;
               ticks[cpu][CentralProcessor.TickType.SOFTIRQ.getIndex()] /= 10000L;
               ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()] /= 10000L;
            }
         }

         return ticks;
      } else {
         return ticks;
      }
   }

   private synchronized long lazilyCalculateMultiplier(long deltaBase, long deltaT) {
      if (this.utilityBaseMultiplier == null) {
         if (deltaT >> 32 > 0L) {
            this.initialUtilityCounters = this.processorUtilityCounters.get().getB();
            return 0L;
         }

         if (deltaBase <= 0L) {
            deltaBase += 4294967296L;
         }

         long multiplier = Math.round((double)deltaT / deltaBase);
         if (deltaT < 50000000L) {
            return multiplier;
         }

         this.utilityBaseMultiplier = multiplier;
      }

      return this.utilityBaseMultiplier;
   }

   private static Pair<List<String>, Map<ProcessorInformation.ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorUtilityCounters() {
      return ProcessorInformation.queryProcessorCapacityCounters();
   }

   @Override
   public long queryContextSwitches() {
      return SystemInformation.queryContextSwitchCounters().getOrDefault(SystemInformation.ContextSwitchProperty.CONTEXTSWITCHESPERSEC, 0L);
   }

   @Override
   public long queryInterrupts() {
      return ProcessorInformation.queryInterruptCounters().getOrDefault(ProcessorInformation.InterruptsProperty.INTERRUPTSPERSEC, 0L);
   }

   static {
      if (USE_LOAD_AVERAGE) {
         LoadAverage.startDaemon();
      }

      USE_CPU_UTILITY = VersionHelpers.IsWindows8OrGreater() && GlobalConfig.get("oshi.os.windows.cpu.utility", false);
   }
}
