package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32DiskDrive {
   private static final String WIN32_DISK_DRIVE = "Win32_DiskDrive";

   private Win32DiskDrive() {
   }

   public static WbemcliUtil.WmiResult<Win32DiskDrive.DiskDriveProperty> queryDiskDrive(WmiQueryHandler h) {
      WbemcliUtil.WmiQuery<Win32DiskDrive.DiskDriveProperty> diskDriveQuery = new WbemcliUtil.WmiQuery<>(
         "Win32_DiskDrive", Win32DiskDrive.DiskDriveProperty.class
      );
      return h.queryWMI(diskDriveQuery, false);
   }

   public static enum DiskDriveProperty {
      INDEX,
      MANUFACTURER,
      MODEL,
      NAME,
      SERIALNUMBER,
      SIZE;
   }
}
