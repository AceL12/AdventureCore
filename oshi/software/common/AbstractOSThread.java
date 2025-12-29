package oshi.software.common;

import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSThread;
import oshi.util.Memoizer;

@ThreadSafe
public abstract class AbstractOSThread implements OSThread {
   private final Supplier<Double> cumulativeCpuLoad = Memoizer.memoize(this::queryCumulativeCpuLoad, Memoizer.defaultExpiration());
   private final int owningProcessId;

   protected AbstractOSThread(int processId) {
      this.owningProcessId = processId;
   }

   @Override
   public int getOwningProcessId() {
      return this.owningProcessId;
   }

   @Override
   public double getThreadCpuLoadCumulative() {
      return this.cumulativeCpuLoad.get();
   }

   private double queryCumulativeCpuLoad() {
      return this.getUpTime() > 0.0 ? (double)(this.getKernelTime() + this.getUserTime()) / this.getUpTime() : 0.0;
   }

   @Override
   public double getThreadCpuLoadBetweenTicks(OSThread priorSnapshot) {
      return priorSnapshot != null
            && this.owningProcessId == priorSnapshot.getOwningProcessId()
            && this.getThreadId() == priorSnapshot.getThreadId()
            && this.getUpTime() > priorSnapshot.getUpTime()
         ? (double)(this.getUserTime() - priorSnapshot.getUserTime() + this.getKernelTime() - priorSnapshot.getKernelTime())
            / (this.getUpTime() - priorSnapshot.getUpTime())
         : this.getThreadCpuLoadCumulative();
   }

   @Override
   public String toString() {
      return "OSThread [threadId="
         + this.getThreadId()
         + ", owningProcessId="
         + this.getOwningProcessId()
         + ", name="
         + this.getName()
         + ", state="
         + this.getState()
         + ", kernelTime="
         + this.getKernelTime()
         + ", userTime="
         + this.getUserTime()
         + ", upTime="
         + this.getUpTime()
         + ", startTime="
         + this.getStartTime()
         + ", startMemoryAddress=0x"
         + String.format("%x", this.getStartMemoryAddress())
         + ", contextSwitches="
         + this.getContextSwitches()
         + ", minorFaults="
         + this.getMinorFaults()
         + ", majorFaults="
         + this.getMajorFaults()
         + "]";
   }
}
