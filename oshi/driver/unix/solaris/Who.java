package oshi.driver.unix.solaris;

import com.sun.jna.Native;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.os.OSSession;

@ThreadSafe
public final class Who {
   private static final SolarisLibc LIBC = SolarisLibc.INSTANCE;

   private Who() {
   }

   public static synchronized List<OSSession> queryUtxent() {
      List<OSSession> whoList = new ArrayList<>();
      LIBC.setutxent();

      SolarisLibc.SolarisUtmpx ut;
      try {
         while ((ut = LIBC.getutxent()) != null) {
            if (ut.ut_type == 7 || ut.ut_type == 6) {
               String user = Native.toString(ut.ut_user, StandardCharsets.US_ASCII);
               String device = Native.toString(ut.ut_line, StandardCharsets.US_ASCII);
               String host = Native.toString(ut.ut_host, StandardCharsets.US_ASCII);
               long loginTime = ut.ut_tv.tv_sec.longValue() * 1000L + ut.ut_tv.tv_usec.longValue() / 1000L;
               if (user.isEmpty() || device.isEmpty() || loginTime < 0L || loginTime > System.currentTimeMillis()) {
                  return oshi.driver.unix.Who.queryWho();
               }

               whoList.add(new OSSession(user, device, loginTime, host));
            }
         }
      } finally {
         LIBC.endutxent();
      }

      return whoList;
   }
}
