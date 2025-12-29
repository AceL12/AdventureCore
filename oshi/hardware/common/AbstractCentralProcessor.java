package oshi.hardware.common;

import com.sun.jna.Platform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import oshi.driver.linux.proc.Auxv;
import oshi.hardware.CentralProcessor;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
public abstract class AbstractCentralProcessor implements CentralProcessor {
   private static final Logger LOG = LoggerFactory.getLogger(AbstractCentralProcessor.class);
   private final Supplier<CentralProcessor.ProcessorIdentifier> cpuid = Memoizer.memoize(this::queryProcessorId);
   private final Supplier<Long> maxFreq = Memoizer.memoize(this::queryMaxFreq, Memoizer.defaultExpiration());
   private final Supplier<long[]> currentFreq = Memoizer.memoize(this::queryCurrentFreq, Memoizer.defaultExpiration());
   private final Supplier<Long> contextSwitches = Memoizer.memoize(this::queryContextSwitches, Memoizer.defaultExpiration());
   private final Supplier<Long> interrupts = Memoizer.memoize(this::queryInterrupts, Memoizer.defaultExpiration());
   private final Supplier<long[]> systemCpuLoadTicks = Memoizer.memoize(this::querySystemCpuLoadTicks, Memoizer.defaultExpiration());
   private final Supplier<long[][]> processorCpuLoadTicks = Memoizer.memoize(this::queryProcessorCpuLoadTicks, Memoizer.defaultExpiration());
   private final int physicalPackageCount;
   private final int physicalProcessorCount;
   private final int logicalProcessorCount;
   private final List<CentralProcessor.LogicalProcessor> logicalProcessors;
   private final List<CentralProcessor.PhysicalProcessor> physicalProcessors;
   private final List<CentralProcessor.ProcessorCache> processorCaches;

   protected AbstractCentralProcessor() {
      Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> processorLists = this.initProcessorCounts();
      this.logicalProcessors = Collections.unmodifiableList(processorLists.getA());
      if (processorLists.getB() == null) {
         Set<Integer> pkgCoreKeys = this.logicalProcessors
            .stream()
            .map(p -> (p.getPhysicalPackageNumber() << 16) + p.getPhysicalProcessorNumber())
            .collect(Collectors.toSet());
         List<CentralProcessor.PhysicalProcessor> physProcs = pkgCoreKeys.stream()
            .sorted()
            .map(k -> new CentralProcessor.PhysicalProcessor(k >> 16, k & 65535))
            .collect(Collectors.toList());
         this.physicalProcessors = Collections.unmodifiableList(physProcs);
      } else {
         this.physicalProcessors = Collections.unmodifiableList(processorLists.getB());
      }

      this.processorCaches = processorLists.getC() == null ? Collections.emptyList() : Collections.unmodifiableList(processorLists.getC());
      Set<Integer> physPkgs = new HashSet<>();

      for (CentralProcessor.LogicalProcessor logProc : this.logicalProcessors) {
         int pkg = logProc.getPhysicalPackageNumber();
         physPkgs.add(pkg);
      }

      this.logicalProcessorCount = this.logicalProcessors.size();
      this.physicalProcessorCount = this.physicalProcessors.size();
      this.physicalPackageCount = physPkgs.size();
   }

   protected abstract Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> initProcessorCounts();

   protected abstract CentralProcessor.ProcessorIdentifier queryProcessorId();

   @Override
   public CentralProcessor.ProcessorIdentifier getProcessorIdentifier() {
      return this.cpuid.get();
   }

   @Override
   public long getMaxFreq() {
      return this.maxFreq.get();
   }

   protected abstract long queryMaxFreq();

   @Override
   public long[] getCurrentFreq() {
      long[] freq = this.currentFreq.get();
      if (freq.length == this.getLogicalProcessorCount()) {
         return freq;
      } else {
         long[] freqs = new long[this.getLogicalProcessorCount()];
         Arrays.fill(freqs, freq[0]);
         return freqs;
      }
   }

   protected abstract long[] queryCurrentFreq();

   @Override
   public long getContextSwitches() {
      return this.contextSwitches.get();
   }

   protected abstract long queryContextSwitches();

   @Override
   public long getInterrupts() {
      return this.interrupts.get();
   }

   protected abstract long queryInterrupts();

   @Override
   public List<CentralProcessor.LogicalProcessor> getLogicalProcessors() {
      return this.logicalProcessors;
   }

   @Override
   public List<CentralProcessor.PhysicalProcessor> getPhysicalProcessors() {
      return this.physicalProcessors;
   }

   @Override
   public List<CentralProcessor.ProcessorCache> getProcessorCaches() {
      return this.processorCaches;
   }

   @Override
   public long[] getSystemCpuLoadTicks() {
      return this.systemCpuLoadTicks.get();
   }

   protected abstract long[] querySystemCpuLoadTicks();

   @Override
   public long[][] getProcessorCpuLoadTicks() {
      return this.processorCpuLoadTicks.get();
   }

   protected abstract long[][] queryProcessorCpuLoadTicks();

   @Override
   public double getSystemCpuLoadBetweenTicks(long[] oldTicks) {
      if (oldTicks.length != CentralProcessor.TickType.values().length) {
         throw new IllegalArgumentException("Tick array " + oldTicks.length + " should have " + CentralProcessor.TickType.values().length + " elements");
      } else {
         long[] ticks = this.getSystemCpuLoadTicks();
         long total = 0L;

         for (int i = 0; i < ticks.length; i++) {
            total += ticks[i] - oldTicks[i];
         }

         long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
            + ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
            - oldTicks[CentralProcessor.TickType.IDLE.getIndex()]
            - oldTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
         LOG.trace("Total ticks: {}  Idle ticks: {}", total, idle);
         return total > 0L ? (double)(total - idle) / total : 0.0;
      }
   }

   @Override
   public double[] getProcessorCpuLoadBetweenTicks(long[][] oldTicks) {
      if (oldTicks.length == this.logicalProcessorCount && oldTicks[0].length == CentralProcessor.TickType.values().length) {
         long[][] ticks = this.getProcessorCpuLoadTicks();
         double[] load = new double[this.logicalProcessorCount];

         for (int cpu = 0; cpu < this.logicalProcessorCount; cpu++) {
            long total = 0L;

            for (int i = 0; i < ticks[cpu].length; i++) {
               total += ticks[cpu][i] - oldTicks[cpu][i];
            }

            long idle = ticks[cpu][CentralProcessor.TickType.IDLE.getIndex()]
               + ticks[cpu][CentralProcessor.TickType.IOWAIT.getIndex()]
               - oldTicks[cpu][CentralProcessor.TickType.IDLE.getIndex()]
               - oldTicks[cpu][CentralProcessor.TickType.IOWAIT.getIndex()];
            LOG.trace("CPU: {}  Total ticks: {}  Idle ticks: {}", cpu, total, idle);
            load[cpu] = total > 0L && idle >= 0L ? (double)(total - idle) / total : 0.0;
         }

         return load;
      } else {
         throw new IllegalArgumentException(
            "Tick array "
               + oldTicks.length
               + " should have "
               + this.logicalProcessorCount
               + " arrays, each of which has "
               + CentralProcessor.TickType.values().length
               + " elements"
         );
      }
   }

   @Override
   public int getLogicalProcessorCount() {
      return this.logicalProcessorCount;
   }

   @Override
   public int getPhysicalProcessorCount() {
      return this.physicalProcessorCount;
   }

   @Override
   public int getPhysicalPackageCount() {
      return this.physicalPackageCount;
   }

   protected static String createProcessorID(String stepping, String model, String family, String[] flags) {
      long processorIdBytes = 0L;
      long steppingL = ParseUtil.parseLongOrDefault(stepping, 0L);
      long modelL = ParseUtil.parseLongOrDefault(model, 0L);
      long familyL = ParseUtil.parseLongOrDefault(family, 0L);
      processorIdBytes |= steppingL & 15L;
      processorIdBytes |= (modelL & 15L) << 4;
      processorIdBytes |= (modelL & 240L) << 16;
      processorIdBytes |= (familyL & 15L) << 8;
      processorIdBytes |= (familyL & 240L) << 20;
      long hwcap = 0L;
      if (Platform.isLinux()) {
         hwcap = Auxv.queryAuxv().getOrDefault(16, 0L);
      }

      if (hwcap > 0L) {
         processorIdBytes |= hwcap << 32;
      } else {
         for (String flag : flags) {
            switch (flag) {
               case "fpu":
                  processorIdBytes |= 4294967296L;
                  break;
               case "vme":
                  processorIdBytes |= 8589934592L;
                  break;
               case "de":
                  processorIdBytes |= 17179869184L;
                  break;
               case "pse":
                  processorIdBytes |= 34359738368L;
                  break;
               case "tsc":
                  processorIdBytes |= 68719476736L;
                  break;
               case "msr":
                  processorIdBytes |= 137438953472L;
                  break;
               case "pae":
                  processorIdBytes |= 274877906944L;
                  break;
               case "mce":
                  processorIdBytes |= 549755813888L;
                  break;
               case "cx8":
                  processorIdBytes |= 1099511627776L;
                  break;
               case "apic":
                  processorIdBytes |= 2199023255552L;
                  break;
               case "sep":
                  processorIdBytes |= 8796093022208L;
                  break;
               case "mtrr":
                  processorIdBytes |= 17592186044416L;
                  break;
               case "pge":
                  processorIdBytes |= 35184372088832L;
                  break;
               case "mca":
                  processorIdBytes |= 70368744177664L;
                  break;
               case "cmov":
                  processorIdBytes |= 140737488355328L;
                  break;
               case "pat":
                  processorIdBytes |= 281474976710656L;
                  break;
               case "pse-36":
                  processorIdBytes |= 562949953421312L;
                  break;
               case "psn":
                  processorIdBytes |= 1125899906842624L;
                  break;
               case "clfsh":
                  processorIdBytes |= 2251799813685248L;
                  break;
               case "ds":
                  processorIdBytes |= 9007199254740992L;
                  break;
               case "acpi":
                  processorIdBytes |= 18014398509481984L;
                  break;
               case "mmx":
                  processorIdBytes |= 36028797018963968L;
                  break;
               case "fxsr":
                  processorIdBytes |= 72057594037927936L;
                  break;
               case "sse":
                  processorIdBytes |= 144115188075855872L;
                  break;
               case "sse2":
                  processorIdBytes |= 288230376151711744L;
                  break;
               case "ss":
                  processorIdBytes |= 576460752303423488L;
                  break;
               case "htt":
                  processorIdBytes |= 1152921504606846976L;
                  break;
               case "tm":
                  processorIdBytes |= 2305843009213693952L;
                  break;
               case "ia64":
                  processorIdBytes |= 4611686018427387904L;
                  break;
               case "pbe":
                  processorIdBytes |= Long.MIN_VALUE;
            }
         }
      }

      return String.format("%016X", processorIdBytes);
   }

   protected List<CentralProcessor.PhysicalProcessor> createProcListFromDmesg(List<CentralProcessor.LogicalProcessor> logProcs, Map<Integer, String> dmesg) {
      boolean isHybrid = dmesg.values().stream().distinct().count() > 1L;
      List<CentralProcessor.PhysicalProcessor> physProcs = new ArrayList<>();
      Set<Integer> pkgCoreKeys = new HashSet<>();

      for (CentralProcessor.LogicalProcessor logProc : logProcs) {
         int pkgId = logProc.getPhysicalPackageNumber();
         int coreId = logProc.getPhysicalProcessorNumber();
         int pkgCoreKey = (pkgId << 16) + coreId;
         if (!pkgCoreKeys.contains(pkgCoreKey)) {
            pkgCoreKeys.add(pkgCoreKey);
            String idStr = dmesg.getOrDefault(logProc.getProcessorNumber(), "");
            int efficiency = 0;
            if (isHybrid
               && (
                  idStr.startsWith("ARM Cortex") && ParseUtil.getFirstIntValue(idStr) >= 70
                     || idStr.startsWith("Apple") && (idStr.contains("Firestorm") || idStr.contains("Avalanche"))
               )) {
               efficiency = 1;
            }

            physProcs.add(new CentralProcessor.PhysicalProcessor(pkgId, coreId, efficiency, idStr));
         }
      }

      physProcs.sort(
         Comparator.comparingInt(CentralProcessor.PhysicalProcessor::getPhysicalPackageNumber)
            .thenComparingInt(CentralProcessor.PhysicalProcessor::getPhysicalProcessorNumber)
      );
      return physProcs;
   }

   public static List<CentralProcessor.ProcessorCache> orderedProcCaches(Set<CentralProcessor.ProcessorCache> caches) {
      return caches.stream()
         .sorted(Comparator.comparing(c -> -1000 * c.getLevel() + 100 * c.getType().ordinal() - Integer.highestOneBit(c.getCacheSize())))
         .collect(Collectors.toList());
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder(this.getProcessorIdentifier().getName());
      sb.append("\n ").append(this.getPhysicalPackageCount()).append(" physical CPU package(s)");
      sb.append("\n ").append(this.getPhysicalProcessorCount()).append(" physical CPU core(s)");
      Map<Integer, Integer> efficiencyCount = new HashMap<>();
      int maxEfficiency = 0;

      for (CentralProcessor.PhysicalProcessor cpu : this.getPhysicalProcessors()) {
         int eff = cpu.getEfficiency();
         efficiencyCount.merge(eff, 1, Integer::sum);
         if (eff > maxEfficiency) {
            maxEfficiency = eff;
         }
      }

      int pCores = efficiencyCount.getOrDefault(maxEfficiency, 0);
      int eCores = this.getPhysicalProcessorCount() - pCores;
      if (eCores > 0) {
         sb.append(" (").append(pCores).append(" performance + ").append(eCores).append(" efficiency)");
      }

      sb.append("\n ").append(this.getLogicalProcessorCount()).append(" logical CPU(s)");
      sb.append('\n').append("Identifier: ").append(this.getProcessorIdentifier().getIdentifier());
      sb.append('\n').append("ProcessorID: ").append(this.getProcessorIdentifier().getProcessorID());
      sb.append('\n').append("Microarchitecture: ").append(this.getProcessorIdentifier().getMicroarchitecture());
      return sb.toString();
   }
}
