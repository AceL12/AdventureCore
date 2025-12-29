package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.PhysicalDisk;
import oshi.driver.windows.wmi.Win32DiskDrive;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartition;
import oshi.driver.windows.wmi.Win32DiskPartition;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartition;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class WindowsHWDiskStore extends AbstractHWDiskStore {
   private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStore.class);
   private static final String PHYSICALDRIVE_PREFIX = "\\\\.\\PHYSICALDRIVE";
   private static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");
   private static final int GUID_BUFSIZE = 100;
   private long reads = 0L;
   private long readBytes = 0L;
   private long writes = 0L;
   private long writeBytes = 0L;
   private long currentQueueLength = 0L;
   private long transferTime = 0L;
   private long timeStamp = 0L;
   private List<HWPartition> partitionList;

   private WindowsHWDiskStore(String name, String model, String serial, long size) {
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

   @Override
   public boolean updateAttributes() {
      String index = null;
      List<HWPartition> partitions = this.getPartitions();
      if (!partitions.isEmpty()) {
         index = Integer.toString(partitions.get(0).getMajor());
      } else {
         if (!this.getName().startsWith("\\\\.\\PHYSICALDRIVE")) {
            LOG.warn("Couldn't match index for {}", this.getName());
            return false;
         }

         index = this.getName().substring("\\\\.\\PHYSICALDRIVE".length(), this.getName().length());
      }

      WindowsHWDiskStore.DiskStats stats = queryReadWriteStats(index);
      if (stats.readMap.containsKey(index)) {
         this.reads = stats.readMap.getOrDefault(index, 0L);
         this.readBytes = stats.readByteMap.getOrDefault(index, 0L);
         this.writes = stats.writeMap.getOrDefault(index, 0L);
         this.writeBytes = stats.writeByteMap.getOrDefault(index, 0L);
         this.currentQueueLength = stats.queueLengthMap.getOrDefault(index, 0L);
         this.transferTime = stats.diskTimeMap.getOrDefault(index, 0L);
         this.timeStamp = stats.timeStamp;
         return true;
      } else {
         return false;
      }
   }

   public static List<HWDiskStore> getDisks() {
      WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
      boolean comInit = false;

      List stats;
      try {
         comInit = h.initCOM();
         List<HWDiskStore> result = new ArrayList<>();
         WindowsHWDiskStore.DiskStats statsx = queryReadWriteStats(null);
         WindowsHWDiskStore.PartitionMaps maps = queryPartitionMaps(h);
         WbemcliUtil.WmiResult<Win32DiskDrive.DiskDriveProperty> vals = Win32DiskDrive.queryDiskDrive(h);

         for (int i = 0; i < vals.getResultCount(); i++) {
            WindowsHWDiskStore ds = new WindowsHWDiskStore(
               WmiUtil.getString(vals, Win32DiskDrive.DiskDriveProperty.NAME, i),
               String.format(
                     "%s %s",
                     WmiUtil.getString(vals, Win32DiskDrive.DiskDriveProperty.MODEL, i),
                     WmiUtil.getString(vals, Win32DiskDrive.DiskDriveProperty.MANUFACTURER, i)
                  )
                  .trim(),
               ParseUtil.hexStringToString(WmiUtil.getString(vals, Win32DiskDrive.DiskDriveProperty.SERIALNUMBER, i)),
               WmiUtil.getUint64(vals, Win32DiskDrive.DiskDriveProperty.SIZE, i)
            );
            String index = Integer.toString(WmiUtil.getUint32(vals, Win32DiskDrive.DiskDriveProperty.INDEX, i));
            ds.reads = statsx.readMap.getOrDefault(index, 0L);
            ds.readBytes = statsx.readByteMap.getOrDefault(index, 0L);
            ds.writes = statsx.writeMap.getOrDefault(index, 0L);
            ds.writeBytes = statsx.writeByteMap.getOrDefault(index, 0L);
            ds.currentQueueLength = statsx.queueLengthMap.getOrDefault(index, 0L);
            ds.transferTime = statsx.diskTimeMap.getOrDefault(index, 0L);
            ds.timeStamp = statsx.timeStamp;
            List<HWPartition> partitions = new ArrayList<>();
            List<String> partList = maps.driveToPartitionMap.get(ds.getName());
            if (partList != null && !partList.isEmpty()) {
               for (String part : partList) {
                  if (maps.partitionMap.containsKey(part)) {
                     partitions.addAll(maps.partitionMap.get(part));
                  }
               }
            }

            ds.partitionList = Collections.unmodifiableList(partitions.stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
            result.add(ds);
         }

         return result;
      } catch (COMException var16) {
         LOG.warn("COM exception: {}", var16.getMessage());
         stats = Collections.emptyList();
      } finally {
         if (comInit) {
            h.unInitCOM();
         }
      }

      return stats;
   }

   private static WindowsHWDiskStore.DiskStats queryReadWriteStats(String index) {
      WindowsHWDiskStore.DiskStats stats = new WindowsHWDiskStore.DiskStats();
      Pair<List<String>, Map<PhysicalDisk.PhysicalDiskProperty, List<Long>>> instanceValuePair = PhysicalDisk.queryDiskCounters();
      List<String> instances = instanceValuePair.getA();
      Map<PhysicalDisk.PhysicalDiskProperty, List<Long>> valueMap = instanceValuePair.getB();
      stats.timeStamp = System.currentTimeMillis();
      List<Long> readList = valueMap.get(PhysicalDisk.PhysicalDiskProperty.DISKREADSPERSEC);
      List<Long> readByteList = valueMap.get(PhysicalDisk.PhysicalDiskProperty.DISKREADBYTESPERSEC);
      List<Long> writeList = valueMap.get(PhysicalDisk.PhysicalDiskProperty.DISKWRITESPERSEC);
      List<Long> writeByteList = valueMap.get(PhysicalDisk.PhysicalDiskProperty.DISKWRITEBYTESPERSEC);
      List<Long> queueLengthList = valueMap.get(PhysicalDisk.PhysicalDiskProperty.CURRENTDISKQUEUELENGTH);
      List<Long> diskTimeList = valueMap.get(PhysicalDisk.PhysicalDiskProperty.PERCENTDISKTIME);
      if (!instances.isEmpty()
         && readList != null
         && readByteList != null
         && writeList != null
         && writeByteList != null
         && queueLengthList != null
         && diskTimeList != null) {
         for (int i = 0; i < instances.size(); i++) {
            String name = getIndexFromName(instances.get(i));
            if (index == null || index.equals(name)) {
               stats.readMap.put(name, readList.get(i));
               stats.readByteMap.put(name, readByteList.get(i));
               stats.writeMap.put(name, writeList.get(i));
               stats.writeByteMap.put(name, writeByteList.get(i));
               stats.queueLengthMap.put(name, queueLengthList.get(i));
               stats.diskTimeMap.put(name, diskTimeList.get(i) / 10000L);
            }
         }

         return stats;
      } else {
         return stats;
      }
   }

   private static WindowsHWDiskStore.PartitionMaps queryPartitionMaps(WmiQueryHandler h) {
      WindowsHWDiskStore.PartitionMaps maps = new WindowsHWDiskStore.PartitionMaps();
      WbemcliUtil.WmiResult<Win32DiskDriveToDiskPartition.DriveToPartitionProperty> drivePartitionMap = Win32DiskDriveToDiskPartition.queryDriveToPartition(h);

      for (int i = 0; i < drivePartitionMap.getResultCount(); i++) {
         Matcher mAnt = DEVICE_ID.matcher(WmiUtil.getRefString(drivePartitionMap, Win32DiskDriveToDiskPartition.DriveToPartitionProperty.ANTECEDENT, i));
         Matcher mDep = DEVICE_ID.matcher(WmiUtil.getRefString(drivePartitionMap, Win32DiskDriveToDiskPartition.DriveToPartitionProperty.DEPENDENT, i));
         if (mAnt.matches() && mDep.matches()) {
            maps.driveToPartitionMap.computeIfAbsent(mAnt.group(1).replace("\\\\", "\\"), x -> new ArrayList<>()).add(mDep.group(1));
         }
      }

      WbemcliUtil.WmiResult<Win32LogicalDiskToPartition.DiskToPartitionProperty> diskPartitionMap = Win32LogicalDiskToPartition.queryDiskToPartition(h);

      for (int ix = 0; ix < diskPartitionMap.getResultCount(); ix++) {
         Matcher mAnt = DEVICE_ID.matcher(WmiUtil.getRefString(diskPartitionMap, Win32LogicalDiskToPartition.DiskToPartitionProperty.ANTECEDENT, ix));
         Matcher mDep = DEVICE_ID.matcher(WmiUtil.getRefString(diskPartitionMap, Win32LogicalDiskToPartition.DiskToPartitionProperty.DEPENDENT, ix));
         long size = WmiUtil.getUint64(diskPartitionMap, Win32LogicalDiskToPartition.DiskToPartitionProperty.ENDINGADDRESS, ix)
            - WmiUtil.getUint64(diskPartitionMap, Win32LogicalDiskToPartition.DiskToPartitionProperty.STARTINGADDRESS, ix)
            + 1L;
         if (mAnt.matches() && mDep.matches()) {
            if (maps.partitionToLogicalDriveMap.containsKey(mAnt.group(1))) {
               maps.partitionToLogicalDriveMap.get(mAnt.group(1)).add(new Pair<>(mDep.group(1) + "\\", size));
            } else {
               List<Pair<String, Long>> list = new ArrayList<>();
               list.add(new Pair<>(mDep.group(1) + "\\", size));
               maps.partitionToLogicalDriveMap.put(mAnt.group(1), list);
            }
         }
      }

      WbemcliUtil.WmiResult<Win32DiskPartition.DiskPartitionProperty> hwPartitionQueryMap = Win32DiskPartition.queryPartition(h);

      for (int ixx = 0; ixx < hwPartitionQueryMap.getResultCount(); ixx++) {
         String deviceID = WmiUtil.getString(hwPartitionQueryMap, Win32DiskPartition.DiskPartitionProperty.DEVICEID, ixx);
         List<Pair<String, Long>> logicalDrives = maps.partitionToLogicalDriveMap.get(deviceID);
         if (logicalDrives != null) {
            for (int j = 0; j < logicalDrives.size(); j++) {
               Pair<String, Long> logicalDrive = logicalDrives.get(j);
               if (logicalDrive != null && !logicalDrive.getA().isEmpty()) {
                  char[] volumeChr = new char[100];
                  Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive.getA(), volumeChr, 100);
                  String uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
                  HWPartition pt = new HWPartition(
                     WmiUtil.getString(hwPartitionQueryMap, Win32DiskPartition.DiskPartitionProperty.NAME, ixx),
                     WmiUtil.getString(hwPartitionQueryMap, Win32DiskPartition.DiskPartitionProperty.TYPE, ixx),
                     WmiUtil.getString(hwPartitionQueryMap, Win32DiskPartition.DiskPartitionProperty.DESCRIPTION, ixx),
                     uuid,
                     logicalDrive.getB(),
                     WmiUtil.getUint32(hwPartitionQueryMap, Win32DiskPartition.DiskPartitionProperty.DISKINDEX, ixx),
                     WmiUtil.getUint32(hwPartitionQueryMap, Win32DiskPartition.DiskPartitionProperty.INDEX, ixx),
                     logicalDrive.getA()
                  );
                  if (maps.partitionMap.containsKey(deviceID)) {
                     maps.partitionMap.get(deviceID).add(pt);
                  } else {
                     List<HWPartition> ptlist = new ArrayList<>();
                     ptlist.add(pt);
                     maps.partitionMap.put(deviceID, ptlist);
                  }
               }
            }
         }
      }

      return maps;
   }

   private static String getIndexFromName(String s) {
      return s.isEmpty() ? s : s.split("\\s")[0];
   }

   private static final class DiskStats {
      private final Map<String, Long> readMap = new HashMap<>();
      private final Map<String, Long> readByteMap = new HashMap<>();
      private final Map<String, Long> writeMap = new HashMap<>();
      private final Map<String, Long> writeByteMap = new HashMap<>();
      private final Map<String, Long> queueLengthMap = new HashMap<>();
      private final Map<String, Long> diskTimeMap = new HashMap<>();
      private long timeStamp;

      private DiskStats() {
      }
   }

   private static final class PartitionMaps {
      private final Map<String, List<String>> driveToPartitionMap = new HashMap<>();
      private final Map<String, List<Pair<String, Long>>> partitionToLogicalDriveMap = new HashMap<>();
      private final Map<String, List<HWPartition>> partitionMap = new HashMap<>();

      private PartitionMaps() {
      }
   }
}
