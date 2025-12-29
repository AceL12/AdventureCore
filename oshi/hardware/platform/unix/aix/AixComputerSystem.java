package oshi.hardware.platform.unix.aix;

import java.util.List;
import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;

@Immutable
final class AixComputerSystem extends AbstractComputerSystem {
   private final Supplier<AixComputerSystem.LsattrStrings> lsattrStrings = Memoizer.memoize(AixComputerSystem::readLsattr);
   private final Supplier<List<String>> lscfg;

   AixComputerSystem(Supplier<List<String>> lscfg) {
      this.lscfg = lscfg;
   }

   @Override
   public String getManufacturer() {
      return this.lsattrStrings.get().manufacturer;
   }

   @Override
   public String getModel() {
      return this.lsattrStrings.get().model;
   }

   @Override
   public String getSerialNumber() {
      return this.lsattrStrings.get().serialNumber;
   }

   @Override
   public String getHardwareUUID() {
      return this.lsattrStrings.get().uuid;
   }

   @Override
   public Firmware createFirmware() {
      return new AixFirmware(this.lsattrStrings.get().biosVendor, this.lsattrStrings.get().biosPlatformVersion, this.lsattrStrings.get().biosVersion);
   }

   @Override
   public Baseboard createBaseboard() {
      return new AixBaseboard(this.lscfg);
   }

   private static AixComputerSystem.LsattrStrings readLsattr() {
      String fwVendor = "IBM";
      String fwVersion = null;
      String fwPlatformVersion = null;
      String manufacturer = fwVendor;
      String model = null;
      String serialNumber = null;
      String uuid = null;
      String fwVersionMarker = "fwversion";
      String modelMarker = "modelname";
      String systemIdMarker = "systemid";
      String uuidMarker = "os_uuid";
      String fwPlatformVersionMarker = "Platform Firmware level is";

      for (String checkLine : ExecutingCommand.runNative("lsattr -El sys0")) {
         if (checkLine.startsWith("fwversion")) {
            fwVersion = checkLine.split("fwversion")[1].trim();
            int comma = fwVersion.indexOf(44);
            if (comma > 0 && fwVersion.length() > comma) {
               fwVendor = fwVersion.substring(0, comma);
               fwVersion = fwVersion.substring(comma + 1);
            }

            fwVersion = ParseUtil.whitespaces.split(fwVersion)[0];
         } else if (checkLine.startsWith("modelname")) {
            model = checkLine.split("modelname")[1].trim();
            int comma = model.indexOf(44);
            if (comma > 0 && model.length() > comma) {
               manufacturer = model.substring(0, comma);
               model = model.substring(comma + 1);
            }

            model = ParseUtil.whitespaces.split(model)[0];
         } else if (checkLine.startsWith("systemid")) {
            serialNumber = checkLine.split("systemid")[1].trim();
            serialNumber = ParseUtil.whitespaces.split(serialNumber)[0];
         } else if (checkLine.startsWith("os_uuid")) {
            uuid = checkLine.split("os_uuid")[1].trim();
            uuid = ParseUtil.whitespaces.split(uuid)[0];
         }
      }

      for (String checkLinex : ExecutingCommand.runNative("lsmcode -c")) {
         if (checkLinex.startsWith("Platform Firmware level is")) {
            fwPlatformVersion = checkLinex.split("Platform Firmware level is")[1].trim();
            break;
         }
      }

      return new AixComputerSystem.LsattrStrings(fwVendor, fwPlatformVersion, fwVersion, manufacturer, model, serialNumber, uuid);
   }

   private static final class LsattrStrings {
      private final String biosVendor;
      private final String biosPlatformVersion;
      private final String biosVersion;
      private final String manufacturer;
      private final String model;
      private final String serialNumber;
      private final String uuid;

      private LsattrStrings(
         String biosVendor, String biosPlatformVersion, String biosVersion, String manufacturer, String model, String serialNumber, String uuid
      ) {
         this.biosVendor = Util.isBlank(biosVendor) ? "unknown" : biosVendor;
         this.biosPlatformVersion = Util.isBlank(biosPlatformVersion) ? "unknown" : biosPlatformVersion;
         this.biosVersion = Util.isBlank(biosVersion) ? "unknown" : biosVersion;
         this.manufacturer = Util.isBlank(manufacturer) ? "unknown" : manufacturer;
         this.model = Util.isBlank(model) ? "unknown" : model;
         this.serialNumber = Util.isBlank(serialNumber) ? "unknown" : serialNumber;
         this.uuid = Util.isBlank(uuid) ? "unknown" : uuid;
      }
   }
}
