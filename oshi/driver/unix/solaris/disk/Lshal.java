package oshi.driver.unix.solaris.disk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class Lshal {
   private static final String LSHAL_CMD = "lshal";

   private Lshal() {
   }

   public static Map<String, Integer> queryDiskToMajorMap() {
      Map<String, Integer> majorMap = new HashMap<>();
      List<String> lshal = ExecutingCommand.runNative("lshal");
      String diskName = null;

      for (String line : lshal) {
         if (line.startsWith("udi ")) {
            String udi = ParseUtil.getSingleQuoteStringValue(line);
            diskName = udi.substring(udi.lastIndexOf(47) + 1);
         } else {
            line = line.trim();
            if (line.startsWith("block.major") && diskName != null) {
               majorMap.put(diskName, ParseUtil.getFirstIntValue(line));
            }
         }
      }

      return majorMap;
   }
}
