package oshi.driver.unix.solaris.disk;

import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class Prtvtoc {
   private static final String PRTVTOC_DEV_DSK = "prtvtoc /dev/dsk/";

   private Prtvtoc() {
   }

   public static List<HWPartition> queryPartitions(String mount, int major) {
      List<HWPartition> partList = new ArrayList<>();
      List<String> prtvotc = ExecutingCommand.runNative("prtvtoc /dev/dsk/" + mount);
      if (prtvotc.size() > 1) {
         int bytesPerSector = 0;

         for (String line : prtvotc) {
            if (line.startsWith("*")) {
               if (line.endsWith("bytes/sector")) {
                  String[] split = ParseUtil.whitespaces.split(line);
                  if (split.length > 0) {
                     bytesPerSector = ParseUtil.parseIntOrDefault(split[1], 0);
                  }
               }
            } else if (bytesPerSector > 0) {
               String[] split = ParseUtil.whitespaces.split(line.trim());
               if (split.length >= 6 && !"2".equals(split[0])) {
                  String identification = mount + "s" + split[0];
                  int minor = ParseUtil.parseIntOrDefault(split[0], 0);
                  String name;
                  switch (ParseUtil.parseIntOrDefault(split[1], 0)) {
                     case 1:
                     case 24:
                        name = "boot";
                        break;
                     case 2:
                        name = "root";
                        break;
                     case 3:
                        name = "swap";
                        break;
                     case 4:
                        name = "usr";
                        break;
                     case 5:
                        name = "backup";
                        break;
                     case 6:
                        name = "stand";
                        break;
                     case 7:
                        name = "var";
                        break;
                     case 8:
                        name = "home";
                        break;
                     case 9:
                        name = "altsctr";
                        break;
                     case 10:
                        name = "cache";
                        break;
                     case 11:
                        name = "reserved";
                        break;
                     case 12:
                        name = "system";
                        break;
                     case 13:
                     case 16:
                     case 17:
                     case 18:
                     case 19:
                     case 20:
                     case 21:
                     case 22:
                     case 23:
                     default:
                        name = "unknown";
                        break;
                     case 14:
                        name = "public region";
                        break;
                     case 15:
                        name = "private region";
                  }

                  String partSize = split[2];
                  String type;
                  switch (partSize) {
                     case "00":
                        type = "wm";
                        break;
                     case "10":
                        type = "rm";
                        break;
                     case "01":
                        type = "wu";
                        break;
                     default:
                        type = "ru";
                  }

                  long partSize = bytesPerSector * ParseUtil.parseLongOrDefault(split[4], 0L);
                  String mountPoint = "";
                  if (split.length > 6) {
                     mountPoint = split[6];
                  }

                  partList.add(new HWPartition(identification, name, type, "", partSize, major, minor, mountPoint));
               }
            }
         }
      }

      return partList;
   }
}
