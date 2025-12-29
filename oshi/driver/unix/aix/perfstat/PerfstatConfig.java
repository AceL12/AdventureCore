package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class PerfstatConfig {
   private static final Perfstat PERF = Perfstat.INSTANCE;

   private PerfstatConfig() {
   }

   public static Perfstat.perfstat_partition_config_t queryConfig() {
      Perfstat.perfstat_partition_config_t config = new Perfstat.perfstat_partition_config_t();
      int ret = PERF.perfstat_partition_config(null, config, config.size(), 1);
      return ret > 0 ? config : new Perfstat.perfstat_partition_config_t();
   }
}
