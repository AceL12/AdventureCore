package oshi.hardware.platform.mac;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.DiskArbitration;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.disk.Fsstat;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.platform.mac.CFUtil;

@ThreadSafe
public final class MacHWDiskStore extends AbstractHWDiskStore {
   private static final CoreFoundation CF = CoreFoundation.INSTANCE;
   private static final DiskArbitration DA = DiskArbitration.INSTANCE;
   private static final Logger LOG = LoggerFactory.getLogger(MacHWDiskStore.class);
   private long reads = 0L;
   private long readBytes = 0L;
   private long writes = 0L;
   private long writeBytes = 0L;
   private long currentQueueLength = 0L;
   private long transferTime = 0L;
   private long timeStamp = 0L;
   private List<HWPartition> partitionList;

   private MacHWDiskStore(
      String name,
      String model,
      String serial,
      long size,
      DiskArbitration.DASessionRef session,
      Map<String, String> mountPointMap,
      Map<MacHWDiskStore.CFKey, CoreFoundation.CFStringRef> cfKeyMap
   ) {
      super(name, model, serial, size);
      this.updateDiskStats(session, mountPointMap, cfKeyMap);
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
      DiskArbitration.DASessionRef session = DA.DASessionCreate(CF.CFAllocatorGetDefault());
      if (session == null) {
         LOG.error("Unable to open session to DiskArbitration framework.");
         return false;
      } else {
         Map<MacHWDiskStore.CFKey, CoreFoundation.CFStringRef> cfKeyMap = mapCFKeys();
         boolean diskFound = this.updateDiskStats(session, Fsstat.queryPartitionToMountMap(), cfKeyMap);
         session.release();

         for (CoreFoundation.CFTypeRef value : cfKeyMap.values()) {
            value.release();
         }

         return diskFound;
      }
   }

   private boolean updateDiskStats(
      DiskArbitration.DASessionRef session, Map<String, String> mountPointMap, Map<MacHWDiskStore.CFKey, CoreFoundation.CFStringRef> cfKeyMap
   ) {
      String bsdName = this.getName();
      CoreFoundation.CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
      if (matchingDict != null) {
         IOKit.IOIterator driveListIter = IOKitUtil.getMatchingServices(matchingDict);
         if (driveListIter != null) {
            IOKit.IORegistryEntry drive = driveListIter.next();
            if (drive != null) {
               if (!drive.conformsTo("IOMedia")) {
                  LOG.error("Unable to find IOMedia device or parent for {}", bsdName);
               } else {
                  IOKit.IORegistryEntry parent = drive.getParentEntry("IOService");
                  if (parent != null && (parent.conformsTo("IOBlockStorageDriver") || parent.conformsTo("AppleAPFSContainerScheme"))) {
                     CoreFoundation.CFMutableDictionaryRef properties = parent.createCFProperties();
                     Pointer result = properties.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.STATISTICS));
                     CoreFoundation.CFDictionaryRef statistics = new CoreFoundation.CFDictionaryRef(result);
                     this.timeStamp = System.currentTimeMillis();
                     result = statistics.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.READ_OPS));
                     CoreFoundation.CFNumberRef stat = new CoreFoundation.CFNumberRef(result);
                     this.reads = stat.longValue();
                     result = statistics.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.READ_BYTES));
                     stat.setPointer(result);
                     this.readBytes = stat.longValue();
                     result = statistics.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.WRITE_OPS));
                     stat.setPointer(result);
                     this.writes = stat.longValue();
                     result = statistics.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.WRITE_BYTES));
                     stat.setPointer(result);
                     this.writeBytes = stat.longValue();
                     Pointer readTimeResult = statistics.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.READ_TIME));
                     Pointer writeTimeResult = statistics.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.WRITE_TIME));
                     if (readTimeResult != null && writeTimeResult != null) {
                        stat.setPointer(readTimeResult);
                        long xferTime = stat.longValue();
                        stat.setPointer(writeTimeResult);
                        xferTime += stat.longValue();
                        this.transferTime = xferTime / 1000000L;
                     }

                     properties.release();
                  } else {
                     LOG.debug("Unable to find block storage driver properties for {}", bsdName);
                  }

                  List<HWPartition> partitions = new ArrayList<>();
                  CoreFoundation.CFMutableDictionaryRef properties = drive.createCFProperties();
                  Pointer result = properties.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.BSD_UNIT));
                  CoreFoundation.CFNumberRef bsdUnit = new CoreFoundation.CFNumberRef(result);
                  result = properties.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.LEAF));
                  CoreFoundation.CFBooleanRef cfFalse = new CoreFoundation.CFBooleanRef(result);
                  CoreFoundation.CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(
                     CF.CFAllocatorGetDefault(), new CoreFoundation.CFIndex(0L), null, null
                  );
                  propertyDict.setValue(cfKeyMap.get(MacHWDiskStore.CFKey.BSD_UNIT), bsdUnit);
                  propertyDict.setValue(cfKeyMap.get(MacHWDiskStore.CFKey.WHOLE), cfFalse);
                  matchingDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CoreFoundation.CFIndex(0L), null, null);
                  matchingDict.setValue(cfKeyMap.get(MacHWDiskStore.CFKey.IO_PROPERTY_MATCH), propertyDict);
                  IOKit.IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
                  properties.release();
                  propertyDict.release();
                  if (serviceIterator != null) {
                     for (IOKit.IORegistryEntry sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator);
                        sdService != null;
                        sdService = IOKit.INSTANCE.IOIteratorNext(serviceIterator)
                     ) {
                        String partBsdName = sdService.getStringProperty("BSD Name");
                        String name = partBsdName;
                        String type = "";
                        DiskArbitration.DADiskRef disk = DA.DADiskCreateFromBSDName(CF.CFAllocatorGetDefault(), session, partBsdName);
                        if (disk != null) {
                           CoreFoundation.CFDictionaryRef diskInfo = DA.DADiskCopyDescription(disk);
                           if (diskInfo != null) {
                              result = diskInfo.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.DA_MEDIA_NAME));
                              type = CFUtil.cfPointerToString(result);
                              result = diskInfo.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.DA_VOLUME_NAME));
                              if (result == null) {
                                 name = type;
                              } else {
                                 name = CFUtil.cfPointerToString(result);
                              }

                              diskInfo.release();
                           }

                           disk.release();
                        }

                        String mountPoint = mountPointMap.getOrDefault(partBsdName, "");
                        Long size = sdService.getLongProperty("Size");
                        Integer bsdMajor = sdService.getIntegerProperty("BSD Major");
                        Integer bsdMinor = sdService.getIntegerProperty("BSD Minor");
                        String uuid = sdService.getStringProperty("UUID");
                        partitions.add(
                           new HWPartition(
                              partBsdName,
                              name,
                              type,
                              uuid == null ? "unknown" : uuid,
                              size == null ? 0L : size,
                              bsdMajor == null ? 0 : bsdMajor,
                              bsdMinor == null ? 0 : bsdMinor,
                              mountPoint
                           )
                        );
                        sdService.release();
                     }

                     serviceIterator.release();
                  }

                  this.partitionList = Collections.unmodifiableList(
                     partitions.stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())
                  );
                  if (parent != null) {
                     parent.release();
                  }
               }

               drive.release();
            }

            driveListIter.release();
            return true;
         }
      }

      return false;
   }

   public static List<HWDiskStore> getDisks() {
      Map<String, String> mountPointMap = Fsstat.queryPartitionToMountMap();
      Map<MacHWDiskStore.CFKey, CoreFoundation.CFStringRef> cfKeyMap = mapCFKeys();
      List<HWDiskStore> diskList = new ArrayList<>();
      DiskArbitration.DASessionRef session = DA.DASessionCreate(CF.CFAllocatorGetDefault());
      if (session == null) {
         LOG.error("Unable to open session to DiskArbitration framework.");
         return Collections.emptyList();
      } else {
         List<String> bsdNames = new ArrayList<>();
         IOKit.IOIterator iter = IOKitUtil.getMatchingServices("IOMedia");
         if (iter != null) {
            for (IOKit.IORegistryEntry media = iter.next(); media != null; media = iter.next()) {
               Boolean whole = media.getBooleanProperty("Whole");
               if (whole != null && whole) {
                  DiskArbitration.DADiskRef disk = DA.DADiskCreateFromIOMedia(CF.CFAllocatorGetDefault(), session, media);
                  bsdNames.add(DA.DADiskGetBSDName(disk));
                  disk.release();
               }

               media.release();
            }

            iter.release();
         }

         for (String bsdName : bsdNames) {
            String model = "";
            String serial = "";
            long size = 0L;
            String path = "/dev/" + bsdName;
            DiskArbitration.DADiskRef disk = DA.DADiskCreateFromBSDName(CF.CFAllocatorGetDefault(), session, path);
            if (disk != null) {
               CoreFoundation.CFDictionaryRef diskInfo = DA.DADiskCopyDescription(disk);
               if (diskInfo != null) {
                  Pointer result = diskInfo.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.DA_DEVICE_MODEL));
                  model = CFUtil.cfPointerToString(result);
                  result = diskInfo.getValue(cfKeyMap.get(MacHWDiskStore.CFKey.DA_MEDIA_SIZE));
                  CoreFoundation.CFNumberRef sizePtr = new CoreFoundation.CFNumberRef(result);
                  size = sizePtr.longValue();
                  diskInfo.release();
                  if (!"Disk Image".equals(model)) {
                     CoreFoundation.CFStringRef modelNameRef = CoreFoundation.CFStringRef.createCFString(model);
                     CoreFoundation.CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(
                        CF.CFAllocatorGetDefault(), new CoreFoundation.CFIndex(0L), null, null
                     );
                     propertyDict.setValue(cfKeyMap.get(MacHWDiskStore.CFKey.MODEL), modelNameRef);
                     CoreFoundation.CFMutableDictionaryRef matchingDict = CF.CFDictionaryCreateMutable(
                        CF.CFAllocatorGetDefault(), new CoreFoundation.CFIndex(0L), null, null
                     );
                     matchingDict.setValue(cfKeyMap.get(MacHWDiskStore.CFKey.IO_PROPERTY_MATCH), propertyDict);
                     IOKit.IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
                     modelNameRef.release();
                     propertyDict.release();
                     if (serviceIterator != null) {
                        for (IOKit.IORegistryEntry sdService = serviceIterator.next(); sdService != null; sdService = serviceIterator.next()) {
                           serial = sdService.getStringProperty("Serial Number");
                           sdService.release();
                           if (serial != null) {
                              break;
                           }

                           sdService.release();
                        }

                        serviceIterator.release();
                     }

                     if (serial == null) {
                        serial = "";
                     }
                  }
               }

               disk.release();
               if (size > 0L) {
                  HWDiskStore diskStore = new MacHWDiskStore(bsdName, model.trim(), serial.trim(), size, session, mountPointMap, cfKeyMap);
                  diskList.add(diskStore);
               }
            }
         }

         session.release();

         for (CoreFoundation.CFTypeRef value : cfKeyMap.values()) {
            value.release();
         }

         return diskList;
      }
   }

   private static Map<MacHWDiskStore.CFKey, CoreFoundation.CFStringRef> mapCFKeys() {
      Map<MacHWDiskStore.CFKey, CoreFoundation.CFStringRef> keyMap = new EnumMap<>(MacHWDiskStore.CFKey.class);

      for (MacHWDiskStore.CFKey cfKey : MacHWDiskStore.CFKey.values()) {
         keyMap.put(cfKey, CoreFoundation.CFStringRef.createCFString(cfKey.getKey()));
      }

      return keyMap;
   }

   private static enum CFKey {
      IO_PROPERTY_MATCH("IOPropertyMatch"),
      STATISTICS("Statistics"),
      READ_OPS("Operations (Read)"),
      READ_BYTES("Bytes (Read)"),
      READ_TIME("Total Time (Read)"),
      WRITE_OPS("Operations (Write)"),
      WRITE_BYTES("Bytes (Write)"),
      WRITE_TIME("Total Time (Write)"),
      BSD_UNIT("BSD Unit"),
      LEAF("Leaf"),
      WHOLE("Whole"),
      DA_MEDIA_NAME("DAMediaName"),
      DA_VOLUME_NAME("DAVolumeName"),
      DA_MEDIA_SIZE("DAMediaSize"),
      DA_DEVICE_MODEL("DADeviceModel"),
      MODEL("Model");

      private final String key;

      private CFKey(String key) {
         this.key = key;
      }

      public String getKey() {
         return this.key;
      }
   }
}
