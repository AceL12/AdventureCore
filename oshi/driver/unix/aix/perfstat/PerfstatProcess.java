package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import java.util.Arrays;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class PerfstatProcess {
   private static final Perfstat PERF = Perfstat.INSTANCE;

   private PerfstatProcess() {
   }

   public static Perfstat.perfstat_process_t[] queryProcesses() {
      Perfstat.perfstat_process_t process = new Perfstat.perfstat_process_t();
      int procCount = PERF.perfstat_process(null, null, process.size(), 0);
      if (procCount > 0) {
         Perfstat.perfstat_process_t[] proct = (Perfstat.perfstat_process_t[])process.toArray(procCount);
         Perfstat.perfstat_id_t firstprocess = new Perfstat.perfstat_id_t();
         int ret = PERF.perfstat_process(firstprocess, proct, process.size(), procCount);
         if (ret > 0) {
            return Arrays.copyOf(proct, ret);
         }
      }

      return new Perfstat.perfstat_process_t[0];
   }
}
