package oshi.util.platform.unix.freebsd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class ProcstatUtil {
   private ProcstatUtil() {
   }

   public static Map<Integer, String> getCwdMap(int pid) {
      List<String> procstat = ExecutingCommand.runNative("procstat -f " + (pid < 0 ? "-a" : pid));
      Map<Integer, String> cwdMap = new HashMap<>();

      for (String line : procstat) {
         String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
         if (split.length == 10 && split[2].equals("cwd")) {
            cwdMap.put(ParseUtil.parseIntOrDefault(split[0], -1), split[9]);
         }
      }

      return cwdMap;
   }

   public static String getCwd(int pid) {
      for (String line : ExecutingCommand.runNative("procstat -f " + pid)) {
         String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
         if (split.length == 10 && split[2].equals("cwd")) {
            return split[9];
         }
      }

      return "";
   }

   public static long getOpenFiles(int pid) {
      long fd = 0L;

      for (String line : ExecutingCommand.runNative("procstat -f " + pid)) {
         String[] split = ParseUtil.whitespaces.split(line.trim(), 10);
         if (split.length == 10 && !"Vd-".contains(split[4])) {
            fd++;
         }
      }

      return fd;
   }
}
