package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class Lshal {
   private Lshal() {
   }

   public static String querySerialNumber() {
      String marker = "system.hardware.serial =";

      for (String checkLine : ExecutingCommand.runNative("lshal")) {
         if (checkLine.contains(marker)) {
            return ParseUtil.getSingleQuoteStringValue(checkLine);
         }
      }

      return null;
   }

   public static String queryUUID() {
      String marker = "system.hardware.uuid =";

      for (String checkLine : ExecutingCommand.runNative("lshal")) {
         if (checkLine.contains(marker)) {
            return ParseUtil.getSingleQuoteStringValue(checkLine);
         }
      }

      return null;
   }
}
