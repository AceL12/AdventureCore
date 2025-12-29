package oshi.software.os.linux;

import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

@ThreadSafe
public class LinuxOSThread extends AbstractOSThread {
   private static final int[] PROC_TASK_STAT_ORDERS = new int[LinuxOSThread.ThreadPidStat.values().length];
   private final int threadId;
   private String name;
   private OSProcess.State state = OSProcess.State.INVALID;
   private long minorFaults;
   private long majorFaults;
   private long startMemoryAddress;
   private long contextSwitches;
   private long kernelTime;
   private long userTime;
   private long startTime;
   private long upTime;
   private int priority;

   public LinuxOSThread(int processId, int tid) {
      super(processId);
      this.threadId = tid;
      this.updateAttributes();
   }

   @Override
   public int getThreadId() {
      return this.threadId;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public OSProcess.State getState() {
      return this.state;
   }

   @Override
   public long getStartTime() {
      return this.startTime;
   }

   @Override
   public long getStartMemoryAddress() {
      return this.startMemoryAddress;
   }

   @Override
   public long getContextSwitches() {
      return this.contextSwitches;
   }

   @Override
   public long getMinorFaults() {
      return this.minorFaults;
   }

   @Override
   public long getMajorFaults() {
      return this.majorFaults;
   }

   @Override
   public long getKernelTime() {
      return this.kernelTime;
   }

   @Override
   public long getUserTime() {
      return this.userTime;
   }

   @Override
   public long getUpTime() {
      return this.upTime;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public boolean updateAttributes() {
      this.name = FileUtil.getStringFromFile(String.format(ProcPath.TASK_COMM, this.getOwningProcessId(), this.threadId));
      Map<String, String> status = FileUtil.getKeyValueMapFromFile(String.format(ProcPath.TASK_STATUS, this.getOwningProcessId(), this.threadId), ":");
      String stat = FileUtil.getStringFromFile(String.format(ProcPath.TASK_STAT, this.getOwningProcessId(), this.threadId));
      if (stat.isEmpty()) {
         this.state = OSProcess.State.INVALID;
         return false;
      } else {
         long now = System.currentTimeMillis();
         long[] statArray = ParseUtil.parseStringToLongArray(stat, PROC_TASK_STAT_ORDERS, ProcessStat.PROC_PID_STAT_LENGTH, ' ');
         this.startTime = (LinuxOperatingSystem.BOOTTIME * LinuxOperatingSystem.getHz() + statArray[LinuxOSThread.ThreadPidStat.START_TIME.ordinal()])
            * 1000L
            / LinuxOperatingSystem.getHz();
         if (this.startTime >= now) {
            this.startTime = now - 1L;
         }

         this.minorFaults = statArray[LinuxOSThread.ThreadPidStat.MINOR_FAULTS.ordinal()];
         this.majorFaults = statArray[LinuxOSThread.ThreadPidStat.MAJOR_FAULT.ordinal()];
         this.startMemoryAddress = statArray[LinuxOSThread.ThreadPidStat.START_CODE.ordinal()];
         long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("voluntary_ctxt_switches"), 0L);
         long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(status.get("nonvoluntary_ctxt_switches"), 0L);
         this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
         this.state = ProcessStat.getState(status.getOrDefault("State", "U").charAt(0));
         this.kernelTime = statArray[LinuxOSThread.ThreadPidStat.KERNEL_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
         this.userTime = statArray[LinuxOSThread.ThreadPidStat.USER_TIME.ordinal()] * 1000L / LinuxOperatingSystem.getHz();
         this.upTime = now - this.startTime;
         this.priority = (int)statArray[LinuxOSThread.ThreadPidStat.PRIORITY.ordinal()];
         return true;
      }
   }

   static {
      for (LinuxOSThread.ThreadPidStat stat : LinuxOSThread.ThreadPidStat.values()) {
         PROC_TASK_STAT_ORDERS[stat.ordinal()] = stat.getOrder() - 1;
      }
   }

   private static enum ThreadPidStat {
      PPID(4),
      MINOR_FAULTS(10),
      MAJOR_FAULT(12),
      USER_TIME(14),
      KERNEL_TIME(15),
      PRIORITY(18),
      THREAD_COUNT(20),
      START_TIME(22),
      VSZ(23),
      RSS(24),
      START_CODE(26);

      private final int order;

      private ThreadPidStat(int order) {
         this.order = order;
      }

      public int getOrder() {
         return this.order;
      }
   }
}
