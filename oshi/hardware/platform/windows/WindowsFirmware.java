package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.wmi.Win32Bios;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Memoizer;
import oshi.util.Util;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Quintet;

@Immutable
final class WindowsFirmware extends AbstractFirmware {
   private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = Memoizer.memoize(
      WindowsFirmware::queryManufNameDescVersRelease
   );

   @Override
   public String getManufacturer() {
      return this.manufNameDescVersRelease.get().getA();
   }

   @Override
   public String getName() {
      return this.manufNameDescVersRelease.get().getB();
   }

   @Override
   public String getDescription() {
      return this.manufNameDescVersRelease.get().getC();
   }

   @Override
   public String getVersion() {
      return this.manufNameDescVersRelease.get().getD();
   }

   @Override
   public String getReleaseDate() {
      return this.manufNameDescVersRelease.get().getE();
   }

   private static Quintet<String, String, String, String, String> queryManufNameDescVersRelease() {
      String manufacturer = null;
      String name = null;
      String description = null;
      String version = null;
      String releaseDate = null;
      WbemcliUtil.WmiResult<Win32Bios.BiosProperty> win32BIOS = Win32Bios.queryBiosInfo();
      if (win32BIOS.getResultCount() > 0) {
         manufacturer = WmiUtil.getString(win32BIOS, Win32Bios.BiosProperty.MANUFACTURER, 0);
         name = WmiUtil.getString(win32BIOS, Win32Bios.BiosProperty.NAME, 0);
         description = WmiUtil.getString(win32BIOS, Win32Bios.BiosProperty.DESCRIPTION, 0);
         version = WmiUtil.getString(win32BIOS, Win32Bios.BiosProperty.VERSION, 0);
         releaseDate = WmiUtil.getDateString(win32BIOS, Win32Bios.BiosProperty.RELEASEDATE, 0);
      }

      return new Quintet<>(
         Util.isBlank(manufacturer) ? "unknown" : manufacturer,
         Util.isBlank(name) ? "unknown" : name,
         Util.isBlank(description) ? "unknown" : description,
         Util.isBlank(version) ? "unknown" : version,
         Util.isBlank(releaseDate) ? "unknown" : releaseDate
      );
   }
}
