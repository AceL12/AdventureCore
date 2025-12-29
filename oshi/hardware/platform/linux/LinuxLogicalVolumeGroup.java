package oshi.hardware.platform.linux;

import com.sun.jna.platform.linux.Udev;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

final class LinuxLogicalVolumeGroup extends AbstractLogicalVolumeGroup {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxLogicalVolumeGroup.class);
   private static final String BLOCK = "block";
   private static final String DM_UUID = "DM_UUID";
   private static final String DM_VG_NAME = "DM_VG_NAME";
   private static final String DM_LV_NAME = "DM_LV_NAME";
   private static final String DEV_LOCATION = "/dev/";

   LinuxLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
      super(name, lvMap, pvSet);
   }

   static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
      if (!LinuxOperatingSystem.HAS_UDEV) {
         LOG.warn("Logical Volume Group information requires libudev, which is not present.");
         return Collections.emptyList();
      } else {
         Map<String, Map<String, Set<String>>> logicalVolumesMap = new HashMap<>();
         Map<String, Set<String>> physicalVolumesMap = new HashMap<>();

         for (String s : ExecutingCommand.runNative("pvs -o vg_name,pv_name")) {
            String[] split = ParseUtil.whitespaces.split(s.trim());
            if (split.length == 2 && split[1].startsWith("/dev/")) {
               physicalVolumesMap.computeIfAbsent(split[0], k -> new HashSet<>()).add(split[1]);
            }
         }

         Udev.UdevContext udev = Udev.INSTANCE.udev_new();

         try {
            Udev.UdevEnumerate enumerate = udev.enumerateNew();

            try {
               enumerate.addMatchSubsystem("block");
               enumerate.scanDevices();

               for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                  String syspath = entry.getName();
                  Udev.UdevDevice device = udev.deviceNewFromSyspath(syspath);
                  if (device != null) {
                     try {
                        String devnode = device.getDevnode();
                        if (devnode != null && devnode.startsWith("/dev/dm")) {
                           String uuid = device.getPropertyValue("DM_UUID");
                           if (uuid != null && uuid.startsWith("LVM-")) {
                              String vgName = device.getPropertyValue("DM_VG_NAME");
                              String lvName = device.getPropertyValue("DM_LV_NAME");
                              if (!Util.isBlank(vgName) && !Util.isBlank(lvName)) {
                                 logicalVolumesMap.computeIfAbsent(vgName, k -> new HashMap<>());
                                 Map<String, Set<String>> lvMapForGroup = logicalVolumesMap.get(vgName);
                                 physicalVolumesMap.computeIfAbsent(vgName, k -> new HashSet<>());
                                 Set<String> pvSetForGroup = physicalVolumesMap.get(vgName);
                                 File slavesDir = new File(syspath + "/slaves");
                                 File[] slaves = slavesDir.listFiles();
                                 if (slaves != null) {
                                    for (File f : slaves) {
                                       String pvName = f.getName();
                                       lvMapForGroup.computeIfAbsent(lvName, k -> new HashSet<>()).add("/dev/" + pvName);
                                       pvSetForGroup.add("/dev/" + pvName);
                                    }
                                 }
                              }
                           }
                        }
                     } finally {
                        device.unref();
                     }
                  }
               }
            } finally {
               enumerate.unref();
            }
         } finally {
            udev.unref();
         }

         return logicalVolumesMap.entrySet()
            .stream()
            .map(e -> new LinuxLogicalVolumeGroup(e.getKey(), e.getValue(), physicalVolumesMap.get(e.getKey())))
            .collect(Collectors.toList());
      }
   }
}
