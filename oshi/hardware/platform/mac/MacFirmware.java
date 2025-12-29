package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.Memoizer;
import oshi.util.Util;
import oshi.util.tuples.Quintet;

@Immutable
final class MacFirmware extends AbstractFirmware {
   private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = Memoizer.memoize(MacFirmware::queryEfi);

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

   private static Quintet<String, String, String, String, String> queryEfi() {
      String manufacturer = null;
      String name = null;
      String description = null;
      String version = null;
      String releaseDate = null;
      IOKit.IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
      if (platformExpert != null) {
         IOKit.IOIterator iter = platformExpert.getChildIterator("IODeviceTree");
         if (iter != null) {
            for (IOKit.IORegistryEntry entry = iter.next(); entry != null; entry = iter.next()) {
               String var9 = entry.getName();
               switch (var9) {
                  case "rom":
                     byte[] dataxx = entry.getByteArrayProperty("vendor");
                     if (dataxx != null) {
                        manufacturer = Native.toString(dataxx, StandardCharsets.UTF_8);
                     }

                     dataxx = entry.getByteArrayProperty("version");
                     if (dataxx != null) {
                        version = Native.toString(dataxx, StandardCharsets.UTF_8);
                     }

                     dataxx = entry.getByteArrayProperty("release-date");
                     if (dataxx != null) {
                        releaseDate = Native.toString(dataxx, StandardCharsets.UTF_8);
                     }
                     break;
                  case "chosen":
                     byte[] datax = entry.getByteArrayProperty("booter-name");
                     if (datax != null) {
                        name = Native.toString(datax, StandardCharsets.UTF_8);
                     }
                     break;
                  case "efi":
                     byte[] data = entry.getByteArrayProperty("firmware-abi");
                     if (data != null) {
                        description = Native.toString(data, StandardCharsets.UTF_8);
                     }
                     break;
                  default:
                     if (Util.isBlank(name)) {
                        name = entry.getStringProperty("IONameMatch");
                     }
               }

               entry.release();
            }

            iter.release();
         }

         if (Util.isBlank(manufacturer)) {
            byte[] datax = platformExpert.getByteArrayProperty("manufacturer");
            if (datax != null) {
               manufacturer = Native.toString(datax, StandardCharsets.UTF_8);
            }
         }

         if (Util.isBlank(version)) {
            byte[] datax = platformExpert.getByteArrayProperty("target-type");
            if (datax != null) {
               version = Native.toString(datax, StandardCharsets.UTF_8);
            }
         }

         if (Util.isBlank(name)) {
            byte[] datax = platformExpert.getByteArrayProperty("device_type");
            if (datax != null) {
               name = Native.toString(datax, StandardCharsets.UTF_8);
            }
         }

         platformExpert.release();
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
