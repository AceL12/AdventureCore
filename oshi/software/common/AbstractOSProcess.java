package oshi.software.common;

import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSProcess;
import oshi.util.Memoizer;

@ThreadSafe
public abstract class AbstractOSProcess implements OSProcess {
   private final Supplier<Double> cumulativeCpuLoad = Memoizer.memoize(this::queryCumulativeCpuLoad, Memoizer.defaultExpiration());
   private int processID;

   protected AbstractOSProcess(int pid) {
      this.processID = pid;
   }

   @Override
   public int getProcessID() {
      return this.processID;
   }

   @Override
   public double getProcessCpuLoadCumulative() {
      return this.cumulativeCpuLoad.get();
   }

   private double queryCumulativeCpuLoad() {
      return this.getUpTime() > 0.0 ? (double)(this.getKernelTime() + this.getUserTime()) / this.getUpTime() : 0.0;
   }

   @Override
   public double getProcessCpuLoadBetweenTicks(OSProcess priorSnapshot) {
      return priorSnapshot != null && this.processID == priorSnapshot.getProcessID() && this.getUpTime() > priorSnapshot.getUpTime()
         ? (double)(this.getUserTime() - priorSnapshot.getUserTime() + this.getKernelTime() - priorSnapshot.getKernelTime())
            / (this.getUpTime() - priorSnapshot.getUpTime())
         : this.getProcessCpuLoadCumulative();
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder("OSProcess@");
      builder.append(Integer.toHexString(this.hashCode()));
      builder.append("[processID=").append(this.processID);
      builder.append(", name=").append(this.getName()).append(']');
      return builder.toString();
   }
}
