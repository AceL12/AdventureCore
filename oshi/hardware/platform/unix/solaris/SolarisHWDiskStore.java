package oshi.hardware.platform.unix.solaris;

import com.sun.jna.platform.unix.solaris.LibKstat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.disk.Iostat;
import oshi.driver.unix.solaris.disk.Lshal;
import oshi.driver.unix.solaris.disk.Prtvtoc;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.tuples.Quintet;

@ThreadSafe
public final class SolarisHWDiskStore extends AbstractHWDiskStore {
   private long reads = 0L;
   private long readBytes = 0L;
   private long writes = 0L;
   private long writeBytes = 0L;
   private long currentQueueLength = 0L;
   private long transferTime = 0L;
   private long timeStamp = 0L;
   private List<HWPartition> partitionList;

   private SolarisHWDiskStore(String name, String model, String serial, long size) {
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
      this.timeStamp = System.currentTimeMillis();
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return this.updateAttributes2();
      } else {
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         boolean var4;
         label46: {
            try {
               LibKstat.Kstat ksp = kc.lookup(null, 0, this.getName());
               if (ksp != null && kc.read(ksp)) {
                  LibKstat.KstatIO data = new LibKstat.KstatIO(ksp.ks_data);
                  this.reads = data.reads;
                  this.writes = data.writes;
                  this.readBytes = data.nread;
                  this.writeBytes = data.nwritten;
                  this.currentQueueLength = (long)data.wcnt + data.rcnt;
                  this.transferTime = data.rtime / 1000000L;
                  this.timeStamp = ksp.ks_snaptime / 1000000L;
                  var4 = true;
                  break label46;
               }
            } catch (Throwable var6) {
               if (kc != null) {
                  try {
                     kc.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (kc != null) {
               kc.close();
            }

            return false;
         }

         if (kc != null) {
            kc.close();
         }

         return var4;
      }
   }

   private boolean updateAttributes2() {
      String fullName = this.getName();
      String alpha = fullName;
      String numeric = "";

      for (int c = 0; c < fullName.length(); c++) {
         if (fullName.charAt(c) >= '0' && fullName.charAt(c) <= '9') {
            alpha = fullName.substring(0, c);
            numeric = fullName.substring(c);
            break;
         }
      }

      Object[] results = KstatUtil.queryKstat2(
         "kstat:/disk/" + alpha + "/" + this.getName() + "/0", "reads", "writes", "nread", "nwritten", "wcnt", "rcnt", "rtime", "snaptime"
      );
      if (results[results.length - 1] == null) {
         results = KstatUtil.queryKstat2(
            "kstat:/disk/" + alpha + "/" + numeric + "/io", "reads", "writes", "nread", "nwritten", "wcnt", "rcnt", "rtime", "snaptime"
         );
      }

      if (results[results.length - 1] == null) {
         return false;
      } else {
         this.reads = results[0] == null ? 0L : (Long)results[0];
         this.writes = results[1] == null ? 0L : (Long)results[1];
         this.readBytes = results[2] == null ? 0L : (Long)results[2];
         this.writeBytes = results[3] == null ? 0L : (Long)results[3];
         this.currentQueueLength = results[4] == null ? 0L : (Long)results[4];
         this.currentQueueLength = this.currentQueueLength + (results[5] == null ? 0L : (Long)results[5]);
         this.transferTime = results[6] == null ? 0L : (Long)results[6] / 1000000L;
         this.timeStamp = (Long)results[7] / 1000000L;
         return true;
      }
   }

   public static List<HWDiskStore> getDisks() {
      Map<String, String> deviceMap = Iostat.queryPartitionToMountMap();
      Map<String, Integer> majorMap = Lshal.queryDiskToMajorMap();
      Map<String, Quintet<String, String, String, String, Long>> deviceStringMap = Iostat.queryDeviceStrings(deviceMap.keySet());
      List<HWDiskStore> storeList = new ArrayList<>();

      for (Entry<String, Quintet<String, String, String, String, Long>> entry : deviceStringMap.entrySet()) {
         String storeName = entry.getKey();
         Quintet<String, String, String, String, Long> val = entry.getValue();
         storeList.add(
            createStore(
               storeName,
               val.getA(),
               val.getB(),
               val.getC(),
               val.getD(),
               val.getE(),
               deviceMap.getOrDefault(storeName, ""),
               majorMap.getOrDefault(storeName, 0)
            )
         );
      }

      return storeList;
   }

   private static SolarisHWDiskStore createStore(
      String diskName, String model, String vendor, String product, String serial, long size, String mount, int major
   ) {
      SolarisHWDiskStore store = new SolarisHWDiskStore(diskName, model.isEmpty() ? (vendor + " " + product).trim() : model, serial, size);
      store.partitionList = Collections.unmodifiableList(
         Prtvtoc.queryPartitions(mount, major).stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())
      );
      store.updateAttributes();
      return store;
   }
}
