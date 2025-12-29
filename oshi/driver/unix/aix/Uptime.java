package oshi.driver.unix.aix;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class Uptime {
   private static final long MINUTE_MS = 60000L;
   private static final long HOUR_MS = 3600000L;
   private static final long DAY_MS = 86400000L;
   private static final Pattern UPTIME_FORMAT_AIX = Pattern.compile(
      ".*\\sup\\s+((\\d+)\\s+days?,?\\s+)?\\b((\\d+):)?(\\d+)(\\s+min(utes?)?)?,\\s+\\d+\\s+user.+"
   );

   private Uptime() {
   }

   public static long queryUpTime() {
      long uptime = 0L;
      String s = ExecutingCommand.getFirstAnswer("uptime");
      if (s.isEmpty()) {
         s = ExecutingCommand.getFirstAnswer("w");
      }

      if (s.isEmpty()) {
         s = ExecutingCommand.getFirstAnswer("/usr/bin/uptime");
      }

      Matcher m = UPTIME_FORMAT_AIX.matcher(s);
      if (m.matches()) {
         if (m.group(2) != null) {
            uptime += ParseUtil.parseLongOrDefault(m.group(2), 0L) * 86400000L;
         }

         if (m.group(4) != null) {
            uptime += ParseUtil.parseLongOrDefault(m.group(4), 0L) * 3600000L;
         }

         uptime += ParseUtil.parseLongOrDefault(m.group(5), 0L) * 60000L;
      }

      return uptime;
   }
}
