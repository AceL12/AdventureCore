package oshi.software.os.unix.aix;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.PsInfo;
import oshi.jna.platform.unix.AixLibc;
import oshi.software.common.AbstractOSThread;
import oshi.software.os.OSProcess;

@ThreadSafe
public class AixOSThread extends AbstractOSThread {
   private int threadId;
   private OSProcess.State state = OSProcess.State.INVALID;
   private long startMemoryAddress;
   private long contextSwitches;
   private long kernelTime;
   private long userTime;
   private long startTime;
   private long upTime;
   private int priority;

   public AixOSThread(int pid, int tid) {
      super(pid);
      this.threadId = tid;
      this.updateAttributes();
   }

   @Override
   public int getThreadId() {
      return this.threadId;
   }

   @Override
   public OSProcess.State getState() {
      return this.state;
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
   public long getStartTime() {
      return this.startTime;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public boolean updateAttributes() {
      AixLibc.AixLwpsInfo lwpsinfo = PsInfo.queryLwpsInfo(this.getOwningProcessId(), this.getThreadId());
      if (lwpsinfo == null) {
         this.state = OSProcess.State.INVALID;
         return false;
      } else {
         this.threadId = (int)lwpsinfo.pr_lwpid;
         this.startMemoryAddress = lwpsinfo.pr_addr;
         this.state = AixOSProcess.getStateFromOutput((char)lwpsinfo.pr_sname);
         this.priority = lwpsinfo.pr_pri;
         return true;
      }
   }
}
