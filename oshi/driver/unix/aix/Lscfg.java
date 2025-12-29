package oshi.driver.unix.aix;

import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class Lscfg {
   private Lscfg() {
   }

   public static List<String> queryAllDevices() {
      return ExecutingCommand.runNative("lscfg -vp");
   }

   public static Triplet<String, String, String> queryBackplaneModelSerialVersion(List<String> lscfg) {
      String planeMarker = "WAY BACKPLANE";
      String modelMarker = "Part Number";
      String serialMarker = "Serial Number";
      String versionMarker = "Version";
      String locationMarker = "Physical Location";
      String model = null;
      String serialNumber = null;
      String version = null;
      boolean planeFlag = false;

      for (String checkLine : lscfg) {
         if (!planeFlag && checkLine.contains("WAY BACKPLANE")) {
            planeFlag = true;
         } else if (planeFlag) {
            if (checkLine.contains("Part Number")) {
               model = ParseUtil.removeLeadingDots(checkLine.split("Part Number")[1].trim());
            } else if (checkLine.contains("Serial Number")) {
               serialNumber = ParseUtil.removeLeadingDots(checkLine.split("Serial Number")[1].trim());
            } else if (checkLine.contains("Version")) {
               version = ParseUtil.removeLeadingDots(checkLine.split("Version")[1].trim());
            } else if (checkLine.contains("Physical Location")) {
               break;
            }
         }
      }

      return new Triplet<>(model, serialNumber, version);
   }

   public static Pair<String, String> queryModelSerial(String device) {
      String modelMarker = "Machine Type and Model";
      String serialMarker = "Serial Number";
      String model = null;
      String serial = null;

      for (String s : ExecutingCommand.runNative("lscfg -vl " + device)) {
         if (model == null && s.contains(device)) {
            String locDesc = s.split(device)[1].trim();
            int idx = locDesc.indexOf(32);
            if (idx > 0) {
               model = locDesc.substring(idx).trim();
            }
         }

         if (s.contains(modelMarker)) {
            model = ParseUtil.removeLeadingDots(s.split(modelMarker)[1].trim());
         } else if (s.contains(serialMarker)) {
            serial = ParseUtil.removeLeadingDots(s.split(serialMarker)[1].trim());
         }
      }

      return new Pair<>(model, serial);
   }
}
