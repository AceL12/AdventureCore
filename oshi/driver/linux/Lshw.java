package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class Lshw {
   private Lshw() {
   }

   public static String queryModel() {
      String modelMarker = "product:";

      for (String checkLine : ExecutingCommand.runNative("lshw -C system")) {
         if (checkLine.contains(modelMarker)) {
            return checkLine.split(modelMarker)[1].trim();
         }
      }

      return null;
   }

   public static String querySerialNumber() {
      String serialMarker = "serial:";

      for (String checkLine : ExecutingCommand.runNative("lshw -C system")) {
         if (checkLine.contains(serialMarker)) {
            return checkLine.split(serialMarker)[1].trim();
         }
      }

      return null;
   }

   public static String queryUUID() {
      String uuidMarker = "uuid:";

      for (String checkLine : ExecutingCommand.runNative("lshw -C system")) {
         if (checkLine.contains(uuidMarker)) {
            return checkLine.split(uuidMarker)[1].trim();
         }
      }

      return null;
   }

   public static long queryCpuCapacity() {
      String capacityMarker = "capacity:";

      for (String checkLine : ExecutingCommand.runNative("lshw -class processor")) {
         if (checkLine.contains(capacityMarker)) {
            return ParseUtil.parseHertz(checkLine.split(capacityMarker)[1].trim());
         }
      }

      return -1L;
   }
}
