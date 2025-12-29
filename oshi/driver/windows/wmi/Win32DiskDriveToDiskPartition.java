package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32DiskDriveToDiskPartition {
   private static final String WIN32_DISK_DRIVE_TO_DISK_PARTITION = "Win32_DiskDriveToDiskPartition";

   private Win32DiskDriveToDiskPartition() {
   }

   public static WbemcliUtil.WmiResult<Win32DiskDriveToDiskPartition.DriveToPartitionProperty> queryDriveToPartition(WmiQueryHandler h) {
      WbemcliUtil.WmiQuery<Win32DiskDriveToDiskPartition.DriveToPartitionProperty> driveToPartitionQuery = new WbemcliUtil.WmiQuery<>(
         "Win32_DiskDriveToDiskPartition", Win32DiskDriveToDiskPartition.DriveToPartitionProperty.class
      );
      return h.queryWMI(driveToPartitionQuery, false);
   }

   public static enum DriveToPartitionProperty {
      ANTECEDENT,
      DEPENDENT;
   }
}
