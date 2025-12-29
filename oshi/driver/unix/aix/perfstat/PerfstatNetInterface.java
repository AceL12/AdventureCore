package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class PerfstatNetInterface {
   private static final Perfstat PERF = Perfstat.INSTANCE;

   private PerfstatNetInterface() {
   }

   public static Perfstat.perfstat_netinterface_t[] queryNetInterfaces() {
      Perfstat.perfstat_netinterface_t netinterface = new Perfstat.perfstat_netinterface_t();
      int total = PERF.perfstat_netinterface(null, null, netinterface.size(), 0);
      if (total > 0) {
         Perfstat.perfstat_netinterface_t[] statp = (Perfstat.perfstat_netinterface_t[])netinterface.toArray(total);
         Perfstat.perfstat_id_t firstnetinterface = new Perfstat.perfstat_id_t();
         int ret = PERF.perfstat_netinterface(firstnetinterface, statp, netinterface.size(), total);
         if (ret > 0) {
            return statp;
         }
      }

      return new Perfstat.perfstat_netinterface_t[0];
   }
}
