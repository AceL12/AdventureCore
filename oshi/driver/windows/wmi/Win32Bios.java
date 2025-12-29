package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Objects;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32Bios {
   private static final String WIN32_BIOS_WHERE_PRIMARY_BIOS_TRUE = "Win32_BIOS where PrimaryBIOS=true";

   private Win32Bios() {
   }

   public static WbemcliUtil.WmiResult<Win32Bios.BiosSerialProperty> querySerialNumber() {
      WbemcliUtil.WmiQuery<Win32Bios.BiosSerialProperty> serialNumQuery = new WbemcliUtil.WmiQuery<>(
         "Win32_BIOS where PrimaryBIOS=true", Win32Bios.BiosSerialProperty.class
      );
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(serialNumQuery);
   }

   public static WbemcliUtil.WmiResult<Win32Bios.BiosProperty> queryBiosInfo() {
      WbemcliUtil.WmiQuery<Win32Bios.BiosProperty> biosQuery = new WbemcliUtil.WmiQuery<>("Win32_BIOS where PrimaryBIOS=true", Win32Bios.BiosProperty.class);
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(biosQuery);
   }

   public static enum BiosProperty {
      MANUFACTURER,
      NAME,
      DESCRIPTION,
      VERSION,
      RELEASEDATE;
   }

   public static enum BiosSerialProperty {
      SERIALNUMBER;
   }
}
