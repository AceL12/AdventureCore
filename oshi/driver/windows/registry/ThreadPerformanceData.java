package oshi.driver.windows.registry;

import com.sun.jna.platform.win32.WinBase;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.ThreadInformation;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class ThreadPerformanceData {
   private static final String THREAD = "Thread";

   private ThreadPerformanceData() {
   }

   public static Map<Integer, ThreadPerformanceData.PerfCounterBlock> buildThreadMapFromRegistry(Collection<Integer> pids) {
      Triplet<List<Map<ThreadInformation.ThreadPerformanceProperty, Object>>, Long, Long> threadData = HkeyPerformanceDataUtil.readPerfDataFromRegistry(
         "Thread", ThreadInformation.ThreadPerformanceProperty.class
      );
      if (threadData == null) {
         return null;
      } else {
         List<Map<ThreadInformation.ThreadPerformanceProperty, Object>> threadInstanceMaps = threadData.getA();
         long perfTime100nSec = threadData.getB();
         long now = threadData.getC();
         Map<Integer, ThreadPerformanceData.PerfCounterBlock> threadMap = new HashMap<>();

         for (Map<ThreadInformation.ThreadPerformanceProperty, Object> threadInstanceMap : threadInstanceMaps) {
            int pid = (Integer)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.IDPROCESS);
            if ((pids == null || pids.contains(pid)) && pid > 0) {
               int tid = (Integer)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.IDTHREAD);
               String name = (String)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.NAME);
               long upTime = (perfTime100nSec - (Long)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.ELAPSEDTIME)) / 10000L;
               if (upTime < 1L) {
                  upTime = 1L;
               }

               long user = (Long)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.PERCENTUSERTIME) / 10000L;
               long kernel = (Long)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.PERCENTPRIVILEGEDTIME) / 10000L;
               int priority = (Integer)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.PRIORITYCURRENT);
               int threadState = (Integer)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.THREADSTATE);
               int threadWaitReason = (Integer)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.THREADWAITREASON);
               Object addr = threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.STARTADDRESS);
               long startAddr = addr.getClass().equals(Long.class) ? (Long)addr : Integer.toUnsignedLong((Integer)addr);
               int contextSwitches = (Integer)threadInstanceMap.get(ThreadInformation.ThreadPerformanceProperty.CONTEXTSWITCHESPERSEC);
               threadMap.put(
                  tid,
                  new ThreadPerformanceData.PerfCounterBlock(
                     name, tid, pid, now - upTime, user, kernel, priority, threadState, threadWaitReason, startAddr, contextSwitches
                  )
               );
            }
         }

         return threadMap;
      }
   }

   public static Map<Integer, ThreadPerformanceData.PerfCounterBlock> buildThreadMapFromPerfCounters(Collection<Integer> pids) {
      Map<Integer, ThreadPerformanceData.PerfCounterBlock> threadMap = new HashMap<>();
      Pair<List<String>, Map<ThreadInformation.ThreadPerformanceProperty, List<Long>>> instanceValues = ThreadInformation.queryThreadCounters();
      long now = System.currentTimeMillis();
      List<String> instances = instanceValues.getA();
      Map<ThreadInformation.ThreadPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
      List<Long> tidList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.IDTHREAD);
      List<Long> pidList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.IDPROCESS);
      List<Long> userList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.PERCENTUSERTIME);
      List<Long> kernelList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.PERCENTPRIVILEGEDTIME);
      List<Long> startTimeList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.ELAPSEDTIME);
      List<Long> priorityList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.PRIORITYCURRENT);
      List<Long> stateList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.THREADSTATE);
      List<Long> waitReasonList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.THREADWAITREASON);
      List<Long> startAddrList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.STARTADDRESS);
      List<Long> contextSwitchesList = valueMap.get(ThreadInformation.ThreadPerformanceProperty.CONTEXTSWITCHESPERSEC);
      int nameIndex = 0;

      for (int inst = 0; inst < instances.size(); inst++) {
         int pid = pidList.get(inst).intValue();
         if (pids == null || pids.contains(pid)) {
            int tid = tidList.get(inst).intValue();
            String name = Integer.toString(nameIndex++);
            long startTime = startTimeList.get(inst);
            startTime = WinBase.FILETIME.filetimeToDate((int)(startTime >> 32), (int)(startTime & 4294967295L)).getTime();
            if (startTime > now) {
               startTime = now - 1L;
            }

            long user = userList.get(inst) / 10000L;
            long kernel = kernelList.get(inst) / 10000L;
            int priority = priorityList.get(inst).intValue();
            int threadState = stateList.get(inst).intValue();
            int threadWaitReason = waitReasonList.get(inst).intValue();
            long startAddr = startAddrList.get(inst);
            int contextSwitches = contextSwitchesList.get(inst).intValue();
            threadMap.put(
               tid,
               new ThreadPerformanceData.PerfCounterBlock(
                  name, tid, pid, startTime, user, kernel, priority, threadState, threadWaitReason, startAddr, contextSwitches
               )
            );
         }
      }

      return threadMap;
   }

   @Immutable
   public static class PerfCounterBlock {
      private final String name;
      private final int threadID;
      private final int owningProcessID;
      private final long startTime;
      private final long userTime;
      private final long kernelTime;
      private final int priority;
      private final int threadState;
      private final int threadWaitReason;
      private final long startAddress;
      private final int contextSwitches;

      public PerfCounterBlock(
         String name,
         int threadID,
         int owningProcessID,
         long startTime,
         long userTime,
         long kernelTime,
         int priority,
         int threadState,
         int threadWaitReason,
         long startAddress,
         int contextSwitches
      ) {
         this.name = name;
         this.threadID = threadID;
         this.owningProcessID = owningProcessID;
         this.startTime = startTime;
         this.userTime = userTime;
         this.kernelTime = kernelTime;
         this.priority = priority;
         this.threadState = threadState;
         this.threadWaitReason = threadWaitReason;
         this.startAddress = startAddress;
         this.contextSwitches = contextSwitches;
      }

      public String getName() {
         return this.name;
      }

      public int getThreadID() {
         return this.threadID;
      }

      public int getOwningProcessID() {
         return this.owningProcessID;
      }

      public long getStartTime() {
         return this.startTime;
      }

      public long getUserTime() {
         return this.userTime;
      }

      public long getKernelTime() {
         return this.kernelTime;
      }

      public int getPriority() {
         return this.priority;
      }

      public int getThreadState() {
         return this.threadState;
      }

      public int getThreadWaitReason() {
         return this.threadWaitReason;
      }

      public long getStartAddress() {
         return this.startAddress;
      }

      public int getContextSwitches() {
         return this.contextSwitches;
      }
   }
}
