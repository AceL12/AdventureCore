package oshi.driver.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class Dmidecode {
   private Dmidecode() {
   }

   public static String querySerialNumber() {
      String marker = "Serial Number:";

      for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
         if (checkLine.contains(marker)) {
            return checkLine.split(marker)[1].trim();
         }
      }

      return null;
   }

   public static String queryUUID() {
      String marker = "UUID:";

      for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
         if (checkLine.contains(marker)) {
            return checkLine.split(marker)[1].trim();
         }
      }

      return null;
   }

   public static Pair<String, String> queryBiosNameRev() {
      String biosName = null;
      String revision = null;
      String biosMarker = "SMBIOS";
      String revMarker = "Bios Revision:";

      for (String checkLine : ExecutingCommand.runNative("dmidecode -t bios")) {
         if (checkLine.contains("SMBIOS")) {
            String[] biosArr = ParseUtil.whitespaces.split(checkLine);
            if (biosArr.length >= 2) {
               biosName = biosArr[0] + " " + biosArr[1];
            }
         }

         if (checkLine.contains("Bios Revision:")) {
            revision = checkLine.split("Bios Revision:")[1].trim();
            break;
         }
      }

      return new Pair<>(biosName, revision);
   }
}
