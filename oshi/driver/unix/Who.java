package oshi.driver.unix;

import com.sun.jna.Platform;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;
import oshi.util.ExecutingCommand;

@ThreadSafe
public final class Who {
   private static final Pattern WHO_FORMAT_LINUX = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
   private static final DateTimeFormatter WHO_DATE_FORMAT_LINUX = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
   private static final Pattern WHO_FORMAT_UNIX = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\d{2}:\\d{2})\\s*(?:\\((.+)\\))?");
   private static final DateTimeFormatter WHO_DATE_FORMAT_UNIX = new DateTimeFormatterBuilder()
      .appendPattern("MMM d HH:mm")
      .parseDefaulting(ChronoField.YEAR, Year.now().getValue())
      .toFormatter(Locale.US);

   private Who() {
   }

   public static synchronized List<OSSession> queryWho() {
      List<OSSession> whoList = new ArrayList<>();

      for (String s : ExecutingCommand.runNative("who")) {
         boolean matched = false;
         if (Platform.isLinux()) {
            matched = matchLinux(whoList, s);
         }

         if (!matched) {
            matchUnix(whoList, s);
         }
      }

      return whoList;
   }

   private static boolean matchLinux(List<OSSession> whoList, String s) {
      Matcher m = WHO_FORMAT_LINUX.matcher(s);
      if (m.matches()) {
         try {
            whoList.add(
               new OSSession(
                  m.group(1),
                  m.group(2),
                  LocalDateTime.parse(m.group(3) + " " + m.group(4), WHO_DATE_FORMAT_LINUX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                  m.group(5) == null ? "unknown" : m.group(5)
               )
            );
            return true;
         } catch (NullPointerException | DateTimeParseException var4) {
         }
      }

      return false;
   }

   private static boolean matchUnix(List<OSSession> whoList, String s) {
      Matcher m = WHO_FORMAT_UNIX.matcher(s);
      if (m.matches()) {
         try {
            LocalDateTime login = LocalDateTime.parse(m.group(3) + " " + m.group(4) + " " + m.group(5), WHO_DATE_FORMAT_UNIX);
            if (login.isAfter(LocalDateTime.now())) {
               login = login.minus(1L, ChronoUnit.YEARS);
            }

            long millis = login.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            whoList.add(new OSSession(m.group(1), m.group(2), millis, m.group(6) == null ? "" : m.group(6)));
            return true;
         } catch (NullPointerException | DateTimeParseException var6) {
         }
      }

      return false;
   }
}
