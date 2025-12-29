package oshi.util.platform.unix.openbsd;

import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class FstatUtil {
   private FstatUtil() {
   }

   public static String getCwd(int pid) {
      List<String> ps = ExecutingCommand.runNative("ps -axwwo cwd -p " + pid);
      return ps.size() > 1 ? ps.get(1) : "";
   }

   public static long getOpenFiles(int pid) {
      long fd = 0L;

      for (String line : ExecutingCommand.runNative("fstat -sp " + pid)) {
         String[] split = ParseUtil.whitespaces.split(line.trim(), 11);
         if (split.length == 11 && !"pipe".contains(split[4]) && !"unix".contains(split[4])) {
            fd++;
         }
      }

      return fd - 1L;
   }
}
