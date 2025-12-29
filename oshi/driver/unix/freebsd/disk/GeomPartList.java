package oshi.driver.unix.freebsd.disk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWPartition;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class GeomPartList {
   private static final String GEOM_PART_LIST = "geom part list";
   private static final String STAT_FILESIZE = "stat -f %i /dev/";

   private GeomPartList() {
   }

   public static Map<String, List<HWPartition>> queryPartitions() {
      Map<String, String> mountMap = Mount.queryPartitionToMountMap();
      Map<String, List<HWPartition>> partitionMap = new HashMap<>();
      String diskName = null;
      List<HWPartition> partList = new ArrayList<>();
      String partName = null;
      String identification = "unknown";
      String type = "unknown";
      String uuid = "unknown";
      long size = 0L;
      String mountPoint = "";

      for (String line : ExecutingCommand.runNative("geom part list")) {
         line = line.trim();
         if (line.startsWith("Geom name:")) {
            if (diskName != null && !partList.isEmpty()) {
               partitionMap.put(diskName, partList);
               partList = new ArrayList<>();
            }

            diskName = line.substring(line.lastIndexOf(32) + 1);
         }

         if (diskName != null) {
            if (line.contains("Name:")) {
               if (partName != null) {
                  int minor = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("stat -f %i /dev/" + partName), 0);
                  partList.add(new HWPartition(identification, partName, type, uuid, size, 0, minor, mountPoint));
                  partName = null;
                  identification = "unknown";
                  type = "unknown";
                  uuid = "unknown";
                  size = 0L;
               }

               String part = line.substring(line.lastIndexOf(32) + 1);
               if (part.startsWith(diskName)) {
                  partName = part;
                  identification = part;
                  mountPoint = mountMap.getOrDefault(part, "");
               }
            }

            if (partName != null) {
               String[] split = ParseUtil.whitespaces.split(line);
               if (split.length >= 2) {
                  if (line.startsWith("Mediasize:")) {
                     size = ParseUtil.parseLongOrDefault(split[1], 0L);
                  } else if (line.startsWith("rawuuid:")) {
                     uuid = split[1];
                  } else if (line.startsWith("type:")) {
                     type = split[1];
                  }
               }
            }
         }
      }

      if (diskName != null) {
         if (partName != null) {
            int minor = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("stat -f %i /dev/" + partName), 0);
            partList.add(new HWPartition(identification, partName, type, uuid, size, 0, minor, mountPoint));
         }

         if (!partList.isEmpty()) {
            partList = partList.stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList());
            partitionMap.put(diskName, partList);
         }
      }

      return partitionMap;
   }
}
