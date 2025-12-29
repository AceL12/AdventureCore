package oshi.hardware.platform.unix.openbsd;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.openbsd.disk.Disklabel;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Quartet;

@ThreadSafe
public final class OpenBsdHWDiskStore extends AbstractHWDiskStore {
   private final Supplier<List<String>> iostat = Memoizer.memoize(OpenBsdHWDiskStore::querySystatIostat, Memoizer.defaultExpiration());
   private long reads = 0L;
   private long readBytes = 0L;
   private long writes = 0L;
   private long writeBytes = 0L;
   private long currentQueueLength = 0L;
   private long transferTime = 0L;
   private long timeStamp = 0L;
   private List<HWPartition> partitionList;

   private OpenBsdHWDiskStore(String name, String model, String serial, long size) {
      super(name, model, serial, size);
   }

   public static List<HWDiskStore> getDisks() {
      List<HWDiskStore> diskList = new ArrayList<>();
      List<String> dmesg = null;
      String[] devices = OpenBsdSysctlUtil.sysctl("hw.disknames", "").split(",");

      for (String device : devices) {
         String diskName = device.split(":")[0];
         Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
         String model = diskdata.getA();
         long size = diskdata.getC();
         if (size <= 1L) {
            if (dmesg == null) {
               dmesg = ExecutingCommand.runNative("dmesg");
            }

            Pattern diskAt = Pattern.compile(diskName + " at .*<(.+)>.*");
            Pattern diskMB = Pattern.compile(diskName + ":.* (\\d+)MB, (?:(\\d+) bytes\\/sector, )?(?:(\\d+) sectors).*");

            for (String line : dmesg) {
               Matcher m = diskAt.matcher(line);
               if (m.matches()) {
                  model = m.group(1);
               }

               m = diskMB.matcher(line);
               if (m.matches()) {
                  long sectors = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                  long bytesPerSector = ParseUtil.parseLongOrDefault(m.group(2), 0L);
                  if (bytesPerSector == 0L && sectors > 0L) {
                     size = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 20;
                     bytesPerSector = size / sectors;
                     bytesPerSector = Long.highestOneBit(bytesPerSector + bytesPerSector >> 1);
                  }

                  size = bytesPerSector * sectors;
                  break;
               }
            }
         }

         OpenBsdHWDiskStore store = new OpenBsdHWDiskStore(diskName, model, diskdata.getB(), size);
         store.partitionList = diskdata.getD();
         store.updateAttributes();
         diskList.add(store);
      }

      return diskList;
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
      long now = System.currentTimeMillis();
      boolean diskFound = false;

      for (String line : this.iostat.get()) {
         String[] split = ParseUtil.whitespaces.split(line);
         if (split.length < 7 && split[0].equals(this.getName())) {
            diskFound = true;
            this.readBytes = ParseUtil.parseMultipliedToLongs(split[1]);
            this.writeBytes = ParseUtil.parseMultipliedToLongs(split[2]);
            this.reads = (long)ParseUtil.parseDoubleOrDefault(split[3], 0.0);
            this.writes = (long)ParseUtil.parseDoubleOrDefault(split[4], 0.0);
            this.transferTime = (long)(ParseUtil.parseDoubleOrDefault(split[5], 0.0) * 1000.0);
            this.timeStamp = now;
         }
      }

      return diskFound;
   }

   private static List<String> querySystatIostat() {
      return ExecutingCommand.runNative("systat -ab iostat");
   }
}
