package oshi.hardware.platform.linux;

import com.sun.jna.platform.linux.Udev;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

@ThreadSafe
public final class LinuxHWDiskStore extends AbstractHWDiskStore {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxHWDiskStore.class);
   private static final String BLOCK = "block";
   private static final String DISK = "disk";
   private static final String PARTITION = "partition";
   private static final String STAT = "stat";
   private static final String SIZE = "size";
   private static final String MINOR = "MINOR";
   private static final String MAJOR = "MAJOR";
   private static final String ID_FS_TYPE = "ID_FS_TYPE";
   private static final String ID_FS_UUID = "ID_FS_UUID";
   private static final String ID_MODEL = "ID_MODEL";
   private static final String ID_SERIAL_SHORT = "ID_SERIAL_SHORT";
   private static final String DM_UUID = "DM_UUID";
   private static final String DM_VG_NAME = "DM_VG_NAME";
   private static final String DM_LV_NAME = "DM_LV_NAME";
   private static final String LOGICAL_VOLUME_GROUP = "Logical Volume Group";
   private static final String DEV_LOCATION = "/dev/";
   private static final String DEV_MAPPER = "/dev/mapper/";
   private static final int SECTORSIZE = 512;
   private static final int[] UDEV_STAT_ORDERS = new int[LinuxHWDiskStore.UdevStat.values().length];
   private static final int UDEV_STAT_LENGTH;
   private long reads = 0L;
   private long readBytes = 0L;
   private long writes = 0L;
   private long writeBytes = 0L;
   private long currentQueueLength = 0L;
   private long transferTime = 0L;
   private long timeStamp = 0L;
   private List<HWPartition> partitionList = new ArrayList<>();

   private LinuxHWDiskStore(String name, String model, String serial, long size) {
      super(name, model, serial, size);
   }

   @Override
   public long getReads() {
      return this.reads;
   }

   @Override
   public long getReadBytes() {
      return this.readBytes;
   }

   @Override
   public long getWrites() {
      return this.writes;
   }

   @Override
   public long getWriteBytes() {
      return this.writeBytes;
   }

   @Override
   public long getCurrentQueueLength() {
      return this.currentQueueLength;
   }

   @Override
   public long getTransferTime() {
      return this.transferTime;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public List<HWPartition> getPartitions() {
      return this.partitionList;
   }

   public static List<HWDiskStore> getDisks() {
      return getDisks(null);
   }

   private static List<HWDiskStore> getDisks(LinuxHWDiskStore storeToUpdate) {
      if (!LinuxOperatingSystem.HAS_UDEV) {
         LOG.warn("Disk Store information requires libudev, which is not present.");
         return Collections.emptyList();
      } else {
         LinuxHWDiskStore store = null;
         List<HWDiskStore> result = new ArrayList<>();
         Map<String, String> mountsMap = readMountsMap();
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
                        if (devnode != null && !devnode.startsWith("/dev/loop") && !devnode.startsWith("/dev/ram")) {
                           if ("disk".equals(device.getDevtype())) {
                              String devModel = device.getPropertyValue("ID_MODEL");
                              String devSerial = device.getPropertyValue("ID_SERIAL_SHORT");
                              long devSize = ParseUtil.parseLongOrDefault(device.getSysattrValue("size"), 0L) * 512L;
                              if (devnode.startsWith("/dev/dm")) {
                                 devModel = "Logical Volume Group";
                                 devSerial = device.getPropertyValue("DM_UUID");
                                 store = new LinuxHWDiskStore(devnode, devModel, devSerial == null ? "unknown" : devSerial, devSize);
                                 String vgName = device.getPropertyValue("DM_VG_NAME");
                                 String lvName = device.getPropertyValue("DM_LV_NAME");
                                 store.partitionList
                                    .add(
                                       new HWPartition(
                                          getPartitionNameForDmDevice(vgName, lvName),
                                          device.getSysname(),
                                          device.getPropertyValue("ID_FS_TYPE") == null ? "partition" : device.getPropertyValue("ID_FS_TYPE"),
                                          device.getPropertyValue("ID_FS_UUID") == null ? "" : device.getPropertyValue("ID_FS_UUID"),
                                          ParseUtil.parseLongOrDefault(device.getSysattrValue("size"), 0L) * 512L,
                                          ParseUtil.parseIntOrDefault(device.getPropertyValue("MAJOR"), 0),
                                          ParseUtil.parseIntOrDefault(device.getPropertyValue("MINOR"), 0),
                                          getMountPointOfDmDevice(vgName, lvName)
                                       )
                                    );
                              } else {
                                 store = new LinuxHWDiskStore(
                                    devnode, devModel == null ? "unknown" : devModel, devSerial == null ? "unknown" : devSerial, devSize
                                 );
                              }

                              if (storeToUpdate == null) {
                                 computeDiskStats(store, device.getSysattrValue("stat"));
                                 result.add(store);
                              } else if (store.getName().equals(storeToUpdate.getName())
                                 && store.getModel().equals(storeToUpdate.getModel())
                                 && store.getSerial().equals(storeToUpdate.getSerial())
                                 && store.getSize() == storeToUpdate.getSize()) {
                                 computeDiskStats(storeToUpdate, device.getSysattrValue("stat"));
                                 result.add(storeToUpdate);
                                 break;
                              }
                           } else if (storeToUpdate == null && store != null && "partition".equals(device.getDevtype())) {
                              Udev.UdevDevice parent = device.getParentWithSubsystemDevtype("block", "disk");
                              if (parent != null && store.getName().equals(parent.getDevnode())) {
                                 String name = device.getDevnode();
                                 store.partitionList
                                    .add(
                                       new HWPartition(
                                          name,
                                          device.getSysname(),
                                          device.getPropertyValue("ID_FS_TYPE") == null ? "partition" : device.getPropertyValue("ID_FS_TYPE"),
                                          device.getPropertyValue("ID_FS_UUID") == null ? "" : device.getPropertyValue("ID_FS_UUID"),
                                          ParseUtil.parseLongOrDefault(device.getSysattrValue("size"), 0L) * 512L,
                                          ParseUtil.parseIntOrDefault(device.getPropertyValue("MAJOR"), 0),
                                          ParseUtil.parseIntOrDefault(device.getPropertyValue("MINOR"), 0),
                                          mountsMap.getOrDefault(name, getDependentNamesFromHoldersDirectory(device.getSysname()))
                                       )
                                    );
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

         for (HWDiskStore hwds : result) {
            ((LinuxHWDiskStore)hwds).partitionList = Collections.unmodifiableList(
               hwds.getPartitions().stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())
            );
         }

         return result;
      }
   }

   @Override
   public boolean updateAttributes() {
      return !getDisks(this).isEmpty();
   }

   private static Map<String, String> readMountsMap() {
      Map<String, String> mountsMap = new HashMap<>();

      for (String mount : FileUtil.readFile(ProcPath.MOUNTS)) {
         String[] split = ParseUtil.whitespaces.split(mount);
         if (split.length >= 2 && split[0].startsWith("/dev/")) {
            mountsMap.put(split[0], split[1]);
         }
      }

      return mountsMap;
   }

   private static void computeDiskStats(LinuxHWDiskStore store, String devstat) {
      long[] devstatArray = ParseUtil.parseStringToLongArray(devstat, UDEV_STAT_ORDERS, UDEV_STAT_LENGTH, ' ');
      store.timeStamp = System.currentTimeMillis();
      store.reads = devstatArray[LinuxHWDiskStore.UdevStat.READS.ordinal()];
      store.readBytes = devstatArray[LinuxHWDiskStore.UdevStat.READ_BYTES.ordinal()] * 512L;
      store.writes = devstatArray[LinuxHWDiskStore.UdevStat.WRITES.ordinal()];
      store.writeBytes = devstatArray[LinuxHWDiskStore.UdevStat.WRITE_BYTES.ordinal()] * 512L;
      store.currentQueueLength = devstatArray[LinuxHWDiskStore.UdevStat.QUEUE_LENGTH.ordinal()];
      store.transferTime = devstatArray[LinuxHWDiskStore.UdevStat.ACTIVE_MS.ordinal()];
   }

   private static String getPartitionNameForDmDevice(String vgName, String lvName) {
      return "/dev/" + vgName + '/' + lvName;
   }

   private static String getMountPointOfDmDevice(String vgName, String lvName) {
      return "/dev/mapper/" + vgName + '-' + lvName;
   }

   private static String getDependentNamesFromHoldersDirectory(String sysPath) {
      File holdersDir = new File(sysPath + "/holders");
      File[] holders = holdersDir.listFiles();
      return holders != null ? Arrays.stream(holders).map(File::getName).collect(Collectors.joining(" ")) : "";
   }

   static {
      for (LinuxHWDiskStore.UdevStat stat : LinuxHWDiskStore.UdevStat.values()) {
         UDEV_STAT_ORDERS[stat.ordinal()] = stat.getOrder();
      }

      String stat = FileUtil.getStringFromFile(ProcPath.DISKSTATS);
      int statLength = 11;
      if (!stat.isEmpty()) {
         statLength = ParseUtil.countStringToLongArray(stat, ' ');
      }

      UDEV_STAT_LENGTH = statLength;
   }

   static enum UdevStat {
      READS(0),
      READ_BYTES(2),
      WRITES(4),
      WRITE_BYTES(6),
      QUEUE_LENGTH(8),
      ACTIVE_MS(9);

      private int order;

      public int getOrder() {
         return this.order;
      }

      private UdevStat(int order) {
         this.order = order;
      }
   }
}
