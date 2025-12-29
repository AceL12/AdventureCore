package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class PerfstatMemory {
   private static final Perfstat PERF = Perfstat.INSTANCE;

   private PerfstatMemory() {
   }

   public static Perfstat.perfstat_memory_total_t queryMemoryTotal() {
      Perfstat.perfstat_memory_total_t memory = new Perfstat.perfstat_memory_total_t();
      int ret = PERF.perfstat_memory_total(null, memory, memory.size(), 1);
      return ret > 0 ? memory : new Perfstat.perfstat_memory_total_t();
   }
}
