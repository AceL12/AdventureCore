package oshi.software.os;

import java.util.function.Predicate;

public interface OSThread {
   int getThreadId();

   default String getName() {
      return "";
   }

   OSProcess.State getState();

   double getThreadCpuLoadCumulative();

   double getThreadCpuLoadBetweenTicks(OSThread var1);

   int getOwningProcessId();

   default long getStartMemoryAddress() {
      return 0L;
   }

   default long getContextSwitches() {
      return 0L;
   }

   default long getMinorFaults() {
      return 0L;
   }

   default long getMajorFaults() {
      return 0L;
   }

   long getKernelTime();

   long getUserTime();

   long getUpTime();

   long getStartTime();

   int getPriority();

   default boolean updateAttributes() {
      return false;
   }

   public static final class ThreadFiltering {
      public static final Predicate<OSThread> VALID_THREAD = p -> !p.getState().equals(OSProcess.State.INVALID);

      private ThreadFiltering() {
      }
   }
}
