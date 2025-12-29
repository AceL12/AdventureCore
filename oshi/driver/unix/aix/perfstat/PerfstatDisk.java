package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class PerfstatDisk {
   private static final Perfstat PERF = Perfstat.INSTANCE;

   private PerfstatDisk() {
   }

   public static Perfstat.perfstat_disk_t[] queryDiskStats() {
      Perfstat.perfstat_disk_t diskStats = new Perfstat.perfstat_disk_t();
      int total = PERF.perfstat_disk(null, null, diskStats.size(), 0);
      if (total > 0) {
         Perfstat.perfstat_disk_t[] statp = (Perfstat.perfstat_disk_t[])diskStats.toArray(total);
         Perfstat.perfstat_id_t firstdiskStats = new Perfstat.perfstat_id_t();
         int ret = PERF.perfstat_disk(firstdiskStats, statp, diskStats.size(), total);
         if (ret > 0) {
            return statp;
         }
      }

      return new Perfstat.perfstat_disk_t[0];
   }
}
