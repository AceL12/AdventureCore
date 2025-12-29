package oshi.driver.unix.openbsd.disk;

import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

@ThreadSafe
public final class Disklabel {
   private Disklabel() {
   }

   public static Quartet<String, String, Long, List<HWPartition>> getDiskParams(String diskName) {
      List<HWPartition> partitions = new ArrayList<>();
      String totalMarker = "total sectors:";
      long totalSectors = 1L;
      String bpsMarker = "bytes/sector:";
      int bytesPerSector = 1;
      String labelMarker = "label:";
      String label = "";
      String duidMarker = "duid:";
      String duid = "";

      for (String line : ExecutingCommand.runNative("disklabel -n " + diskName)) {
         if (line.contains(totalMarker)) {
            totalSectors = ParseUtil.getFirstIntValue(line);
         } else if (line.contains(bpsMarker)) {
            bytesPerSector = ParseUtil.getFirstIntValue(line);
         } else if (line.contains(labelMarker)) {
            label = line.split(labelMarker)[1].trim();
         } else if (line.contains(duidMarker)) {
            duid = line.split(duidMarker)[1].trim();
         }

         if (line.trim().indexOf(58) == 1) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), 9);
            String name = split[0].substring(0, 1);
            Pair<Integer, Integer> majorMinor = getMajorMinor(diskName, name);
            if (split.length > 4) {
               partitions.add(
                  new HWPartition(
                     diskName + name,
                     name,
                     split[3],
                     duid + "." + name,
                     ParseUtil.parseLongOrDefault(split[1], 0L) * bytesPerSector,
                     majorMinor.getA(),
                     majorMinor.getB(),
                     split.length > 5 ? split[split.length - 1] : ""
                  )
               );
            }
         }
      }

      return partitions.isEmpty() ? getDiskParamsNoRoot(diskName) : new Quartet<>(label, duid, totalSectors * bytesPerSector, partitions);
   }

   private static Quartet<String, String, Long, List<HWPartition>> getDiskParamsNoRoot(String diskName) {
      List<HWPartition> partitions = new ArrayList<>();

      for (String line : ExecutingCommand.runNative("df")) {
         if (line.startsWith("/dev/" + diskName)) {
            String[] split = ParseUtil.whitespaces.split(line);
            String name = split[0].substring(5 + diskName.length());
            Pair<Integer, Integer> majorMinor = getMajorMinor(diskName, name);
            if (split.length > 5) {
               long partSize = ParseUtil.parseLongOrDefault(split[1], 1L) * 512L;
               partitions.add(new HWPartition(split[0], split[0].substring(5), "unknown", "unknown", partSize, majorMinor.getA(), majorMinor.getB(), split[5]));
            }
         }
      }

      return new Quartet<>("unknown", "unknown", 0L, partitions);
   }

   private static Pair<Integer, Integer> getMajorMinor(String diskName, String name) {
      int major = 0;
      int minor = 0;
      String majorMinor = ExecutingCommand.getFirstAnswer("stat -f %Hr,%Lr /dev/" + diskName + name);
      int comma = majorMinor.indexOf(44);
      if (comma > 0 && comma < majorMinor.length()) {
         major = ParseUtil.parseIntOrDefault(majorMinor.substring(0, comma), 0);
         minor = ParseUtil.parseIntOrDefault(majorMinor.substring(comma + 1), 0);
      }

      return new Pair<>(major, minor);
   }
}
