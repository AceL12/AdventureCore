package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Objects;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32PhysicalMemory {
   private static final String WIN32_PHYSICAL_MEMORY = "Win32_PhysicalMemory";

   private Win32PhysicalMemory() {
   }

   public static WbemcliUtil.WmiResult<Win32PhysicalMemory.PhysicalMemoryProperty> queryphysicalMemory() {
      WbemcliUtil.WmiQuery<Win32PhysicalMemory.PhysicalMemoryProperty> physicalMemoryQuery = new WbemcliUtil.WmiQuery<>(
         "Win32_PhysicalMemory", Win32PhysicalMemory.PhysicalMemoryProperty.class
      );
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(physicalMemoryQuery);
   }

   public static WbemcliUtil.WmiResult<Win32PhysicalMemory.PhysicalMemoryPropertyWin8> queryphysicalMemoryWin8() {
      WbemcliUtil.WmiQuery<Win32PhysicalMemory.PhysicalMemoryPropertyWin8> physicalMemoryQuery = new WbemcliUtil.WmiQuery<>(
         "Win32_PhysicalMemory", Win32PhysicalMemory.PhysicalMemoryPropertyWin8.class
      );
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(physicalMemoryQuery);
   }

   public static enum PhysicalMemoryProperty {
      BANKLABEL,
      CAPACITY,
      SPEED,
      MANUFACTURER,
      SMBIOSMEMORYTYPE;
   }

   public static enum PhysicalMemoryPropertyWin8 {
      BANKLABEL,
      CAPACITY,
      SPEED,
      MANUFACTURER,
      MEMORYTYPE;
   }
}
