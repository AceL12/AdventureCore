package oshi.hardware.platform.unix.solaris;

import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.hardware.platform.unix.UnixBaseboard;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;

@Immutable
final class SolarisComputerSystem extends AbstractComputerSystem {
   private final Supplier<SolarisComputerSystem.SmbiosStrings> smbiosStrings = Memoizer.memoize(SolarisComputerSystem::readSmbios);

   @Override
   public String getManufacturer() {
      return this.smbiosStrings.get().manufacturer;
   }

   @Override
   public String getModel() {
      return this.smbiosStrings.get().model;
   }

   @Override
   public String getSerialNumber() {
      return this.smbiosStrings.get().serialNumber;
   }

   @Override
   public String getHardwareUUID() {
      return this.smbiosStrings.get().uuid;
   }

   @Override
   public Firmware createFirmware() {
      return new SolarisFirmware(this.smbiosStrings.get().biosVendor, this.smbiosStrings.get().biosVersion, this.smbiosStrings.get().biosDate);
   }

   @Override
   public Baseboard createBaseboard() {
      return new UnixBaseboard(
         this.smbiosStrings.get().boardManufacturer,
         this.smbiosStrings.get().boardModel,
         this.smbiosStrings.get().boardSerialNumber,
         this.smbiosStrings.get().boardVersion
      );
   }

   private static SolarisComputerSystem.SmbiosStrings readSmbios() {
      String biosVendor = null;
      String biosVersion = null;
      String biosDate = null;
      String manufacturer = null;
      String model = null;
      String serialNumber = null;
      String uuid = null;
      String boardManufacturer = null;
      String boardModel = null;
      String boardVersion = null;
      String boardSerialNumber = null;
      String vendorMarker = "Vendor:";
      String biosDateMarker = "Release Date:";
      String biosVersionMarker = "VersionString:";
      String manufacturerMarker = "Manufacturer:";
      String productMarker = "Product:";
      String serialNumMarker = "Serial Number:";
      String uuidMarker = "UUID:";
      String versionMarker = "Version:";
      int smbTypeId = -1;

      for (String checkLine : ExecutingCommand.runNative("smbios")) {
         if (checkLine.contains("SMB_TYPE_") && (smbTypeId = getSmbType(checkLine)) == Integer.MAX_VALUE) {
            break;
         }

         switch (smbTypeId) {
            case 0:
               if (checkLine.contains("Vendor:")) {
                  biosVendor = checkLine.split("Vendor:")[1].trim();
               } else if (checkLine.contains("VersionString:")) {
                  biosVersion = checkLine.split("VersionString:")[1].trim();
               } else if (checkLine.contains("Release Date:")) {
                  biosDate = checkLine.split("Release Date:")[1].trim();
               }
               break;
            case 1:
               if (checkLine.contains("Manufacturer:")) {
                  manufacturer = checkLine.split("Manufacturer:")[1].trim();
               } else if (checkLine.contains("Product:")) {
                  model = checkLine.split("Product:")[1].trim();
               } else if (checkLine.contains("Serial Number:")) {
                  serialNumber = checkLine.split("Serial Number:")[1].trim();
               } else if (checkLine.contains("UUID:")) {
                  uuid = checkLine.split("UUID:")[1].trim();
               }
               break;
            case 2:
               if (checkLine.contains("Manufacturer:")) {
                  boardManufacturer = checkLine.split("Manufacturer:")[1].trim();
               } else if (checkLine.contains("Product:")) {
                  boardModel = checkLine.split("Product:")[1].trim();
               } else if (checkLine.contains("Version:")) {
                  boardVersion = checkLine.split("Version:")[1].trim();
               } else if (checkLine.contains("Serial Number:")) {
                  boardSerialNumber = checkLine.split("Serial Number:")[1].trim();
               }
         }
      }

      if (Util.isBlank(serialNumber)) {
         serialNumber = readSerialNumber();
      }

      return new SolarisComputerSystem.SmbiosStrings(
         biosVendor, biosVersion, biosDate, manufacturer, model, serialNumber, uuid, boardManufacturer, boardModel, boardVersion, boardSerialNumber
      );
   }

   private static int getSmbType(String checkLine) {
      if (checkLine.contains("SMB_TYPE_BIOS")) {
         return 0;
      } else if (checkLine.contains("SMB_TYPE_SYSTEM")) {
         return 1;
      } else {
         return checkLine.contains("SMB_TYPE_BASEBOARD") ? 2 : Integer.MAX_VALUE;
      }
   }

   private static String readSerialNumber() {
      String serialNumber = ExecutingCommand.getFirstAnswer("sneep");
      if (serialNumber.isEmpty()) {
         String marker = "chassis-sn:";

         for (String checkLine : ExecutingCommand.runNative("prtconf -pv")) {
            if (checkLine.contains(marker)) {
               serialNumber = ParseUtil.getSingleQuoteStringValue(checkLine);
               break;
            }
         }
      }

      return serialNumber;
   }

   private static final class SmbiosStrings {
      private final String biosVendor;
      private final String biosVersion;
      private final String biosDate;
      private final String manufacturer;
      private final String model;
      private final String serialNumber;
      private final String uuid;
      private final String boardManufacturer;
      private final String boardModel;
      private final String boardVersion;
      private final String boardSerialNumber;

      private SmbiosStrings(
         String biosVendor,
         String biosVersion,
         String biosDate,
         String manufacturer,
         String model,
         String serialNumber,
         String uuid,
         String boardManufacturer,
         String boardModel,
         String boardVersion,
         String boardSerialNumber
      ) {
         this.biosVendor = Util.isBlank(biosVendor) ? "unknown" : biosVendor;
         this.biosVersion = Util.isBlank(biosVersion) ? "unknown" : biosVersion;
         this.biosDate = Util.isBlank(biosDate) ? "unknown" : biosDate;
         this.manufacturer = Util.isBlank(manufacturer) ? "unknown" : manufacturer;
         this.model = Util.isBlank(model) ? "unknown" : model;
         this.serialNumber = Util.isBlank(serialNumber) ? "unknown" : serialNumber;
         this.uuid = Util.isBlank(uuid) ? "unknown" : uuid;
         this.boardManufacturer = Util.isBlank(boardManufacturer) ? "unknown" : boardManufacturer;
         this.boardModel = Util.isBlank(boardModel) ? "unknown" : boardModel;
         this.boardVersion = Util.isBlank(boardVersion) ? "unknown" : boardVersion;
         this.boardSerialNumber = Util.isBlank(boardSerialNumber) ? "unknown" : boardSerialNumber;
      }
   }
}
