package oshi.hardware.platform.unix.freebsd;

import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Triplet;

@Immutable
final class FreeBsdFirmware extends AbstractFirmware {
   private final Supplier<Triplet<String, String, String>> manufVersRelease = Memoizer.memoize(FreeBsdFirmware::readDmiDecode);

   @Override
   public String getManufacturer() {
      return this.manufVersRelease.get().getA();
   }

   @Override
   public String getVersion() {
      return this.manufVersRelease.get().getB();
   }

   @Override
   public String getReleaseDate() {
      return this.manufVersRelease.get().getC();
   }

   private static Triplet<String, String, String> readDmiDecode() {
      String manufacturer = null;
      String version = null;
      String releaseDate = "";
      String manufacturerMarker = "Vendor:";
      String versionMarker = "Version:";
      String releaseDateMarker = "Release Date:";

      for (String checkLine : ExecutingCommand.runNative("dmidecode -t bios")) {
         if (checkLine.contains("Vendor:")) {
            manufacturer = checkLine.split("Vendor:")[1].trim();
         } else if (checkLine.contains("Version:")) {
            version = checkLine.split("Version:")[1].trim();
         } else if (checkLine.contains("Release Date:")) {
            releaseDate = checkLine.split("Release Date:")[1].trim();
         }
      }

      releaseDate = ParseUtil.parseMmDdYyyyToYyyyMmDD(releaseDate);
      return new Triplet<>(
         Util.isBlank(manufacturer) ? "unknown" : manufacturer,
         Util.isBlank(version) ? "unknown" : version,
         Util.isBlank(releaseDate) ? "unknown" : releaseDate
      );
   }
}
