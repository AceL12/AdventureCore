package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Objects;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32OperatingSystem {
   private static final String WIN32_OPERATING_SYSTEM = "Win32_OperatingSystem";

   private Win32OperatingSystem() {
   }

   public static WbemcliUtil.WmiResult<Win32OperatingSystem.OSVersionProperty> queryOsVersion() {
      WbemcliUtil.WmiQuery<Win32OperatingSystem.OSVersionProperty> osVersionQuery = new WbemcliUtil.WmiQuery<>(
         "Win32_OperatingSystem", Win32OperatingSystem.OSVersionProperty.class
      );
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(osVersionQuery);
   }

   public static enum OSVersionProperty {
      VERSION,
      PRODUCTTYPE,
      BUILDNUMBER,
      CSDVERSION,
      SUITEMASK;
   }
}
