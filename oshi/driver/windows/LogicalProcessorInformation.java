package oshi.driver.windows;

import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32Processor;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class LogicalProcessorInformation {
   private static final boolean IS_WIN10_OR_GREATER = VersionHelpers.IsWindows10OrGreater();

   private LogicalProcessorInformation() {
   }

   public static Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> getLogicalProcessorInformationEx() {
      WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX[] procInfo = Kernel32Util.getLogicalProcessorInformationEx(65535);
      List<WinNT.GROUP_AFFINITY[]> packages = new ArrayList<>();
      Set<CentralProcessor.ProcessorCache> caches = new HashSet<>();
      List<WinNT.GROUP_AFFINITY> cores = new ArrayList<>();
      List<WinNT.NUMA_NODE_RELATIONSHIP> numaNodes = new ArrayList<>();
      Map<WinNT.GROUP_AFFINITY, Integer> coreEfficiencyMap = new HashMap<>();

      for (WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX info : procInfo) {
         switch (info.relationship) {
            case 0:
               WinNT.PROCESSOR_RELATIONSHIP core = (WinNT.PROCESSOR_RELATIONSHIP)info;
               cores.add(core.groupMask[0]);
               if (IS_WIN10_OR_GREATER) {
                  coreEfficiencyMap.put(core.groupMask[0], Integer.valueOf(core.efficiencyClass));
               }
               break;
            case 1:
               numaNodes.add((WinNT.NUMA_NODE_RELATIONSHIP)info);
               break;
            case 2:
               WinNT.CACHE_RELATIONSHIP cache = (WinNT.CACHE_RELATIONSHIP)info;
               caches.add(
                  new CentralProcessor.ProcessorCache(
                     cache.level, cache.associativity, cache.lineSize, cache.size, CentralProcessor.ProcessorCache.Type.values()[cache.type]
                  )
               );
               break;
            case 3:
               packages.add(((WinNT.PROCESSOR_RELATIONSHIP)info).groupMask);
         }
      }

      cores.sort(Comparator.comparing(c -> c.group * 64L + Long.numberOfTrailingZeros(c.mask.longValue())));
      packages.sort(Comparator.comparing(p -> p[0].group * 64L + Long.numberOfTrailingZeros(p[0].mask.longValue())));
      numaNodes.sort(Comparator.comparing(n -> n.nodeNumber));
      Map<Integer, String> processorIdMap = new HashMap<>();
      WbemcliUtil.WmiResult<Win32Processor.ProcessorIdProperty> processorId = Win32Processor.queryProcessorId();

      for (int pkg = 0; pkg < processorId.getResultCount(); pkg++) {
         processorIdMap.put(pkg, WmiUtil.getString(processorId, Win32Processor.ProcessorIdProperty.PROCESSORID, pkg));
      }

      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
      Map<Integer, Integer> corePkgMap = new HashMap<>();
      Map<Integer, String> pkgCpuidMap = new HashMap<>();

      for (WinNT.NUMA_NODE_RELATIONSHIP node : numaNodes) {
         int nodeNum = node.nodeNumber;
         int group = node.groupMask.group;
         long mask = node.groupMask.mask.longValue();
         int lowBit = Long.numberOfTrailingZeros(mask);
         int hiBit = 63 - Long.numberOfLeadingZeros(mask);

         for (int lp = lowBit; lp <= hiBit; lp++) {
            if ((mask & 1L << lp) != 0L) {
               int coreId = getMatchingCore(cores, group, lp);
               int pkgId = getMatchingPackage(packages, group, lp);
               corePkgMap.put(coreId, pkgId);
               pkgCpuidMap.put(coreId, processorIdMap.getOrDefault(pkgId, ""));
               CentralProcessor.LogicalProcessor logProc = new CentralProcessor.LogicalProcessor(lp, coreId, pkgId, nodeNum, group);
               logProcs.add(logProc);
            }
         }
      }

      List<CentralProcessor.PhysicalProcessor> physProcs = getPhysProcs(cores, coreEfficiencyMap, corePkgMap, pkgCpuidMap);
      return new Triplet<>(logProcs, physProcs, AbstractCentralProcessor.orderedProcCaches(caches));
   }

   private static List<CentralProcessor.PhysicalProcessor> getPhysProcs(
      List<WinNT.GROUP_AFFINITY> cores,
      Map<WinNT.GROUP_AFFINITY, Integer> coreEfficiencyMap,
      Map<Integer, Integer> corePkgMap,
      Map<Integer, String> coreCpuidMap
   ) {
      List<CentralProcessor.PhysicalProcessor> physProcs = new ArrayList<>();

      for (int coreId = 0; coreId < cores.size(); coreId++) {
         int efficiency = coreEfficiencyMap.getOrDefault(cores.get(coreId), 0);
         String cpuid = coreCpuidMap.getOrDefault(coreId, "");
         int pkgId = corePkgMap.getOrDefault(coreId, 0);
         physProcs.add(new CentralProcessor.PhysicalProcessor(pkgId, coreId, efficiency, cpuid));
      }

      return physProcs;
   }

   private static int getMatchingPackage(List<WinNT.GROUP_AFFINITY[]> packages, int g, int lp) {
      for (int i = 0; i < packages.size(); i++) {
         for (int j = 0; j < ((WinNT.GROUP_AFFINITY[])packages.get(i)).length; j++) {
            if ((packages.get(i)[j].mask.longValue() & 1L << lp) != 0L && packages.get(i)[j].group == g) {
               return i;
            }
         }
      }

      return 0;
   }

   private static int getMatchingCore(List<WinNT.GROUP_AFFINITY> cores, int g, int lp) {
      for (int j = 0; j < cores.size(); j++) {
         if ((cores.get(j).mask.longValue() & 1L << lp) != 0L && cores.get(j).group == g) {
            return j;
         }
      }

      return 0;
   }

   public static Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> getLogicalProcessorInformation() {
      List<Long> packageMaskList = new ArrayList<>();
      List<Long> coreMaskList = new ArrayList<>();
      WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION[] processors = Kernel32Util.getLogicalProcessorInformation();

      for (WinNT.SYSTEM_LOGICAL_PROCESSOR_INFORMATION proc : processors) {
         if (proc.relationship == 3) {
            packageMaskList.add(proc.processorMask.longValue());
         } else if (proc.relationship == 0) {
            coreMaskList.add(proc.processorMask.longValue());
         }
      }

      coreMaskList.sort(null);
      packageMaskList.sort(null);
      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();

      for (int core = 0; core < coreMaskList.size(); core++) {
         long coreMask = coreMaskList.get(core);
         int lowBit = Long.numberOfTrailingZeros(coreMask);
         int hiBit = 63 - Long.numberOfLeadingZeros(coreMask);

         for (int i = lowBit; i <= hiBit; i++) {
            if ((coreMask & 1L << i) != 0L) {
               CentralProcessor.LogicalProcessor logProc = new CentralProcessor.LogicalProcessor(i, core, getBitMatchingPackageNumber(packageMaskList, i));
               logProcs.add(logProc);
            }
         }
      }

      return new Triplet<>(logProcs, null, null);
   }

   private static int getBitMatchingPackageNumber(List<Long> packageMaskList, int logProc) {
      for (int i = 0; i < packageMaskList.size(); i++) {
         if ((packageMaskList.get(i) & 1L << logProc) != 0L) {
            return i;
         }
      }

      return 0;
   }
}
