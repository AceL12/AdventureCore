package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.Memoizer;
import oshi.util.Util;
import oshi.util.tuples.Quartet;

@Immutable
final class MacComputerSystem extends AbstractComputerSystem {
   private final Supplier<Quartet<String, String, String, String>> manufacturerModelSerialUUID = Memoizer.memoize(MacComputerSystem::platformExpert);

   @Override
   public String getManufacturer() {
      return this.manufacturerModelSerialUUID.get().getA();
   }

   @Override
   public String getModel() {
      return this.manufacturerModelSerialUUID.get().getB();
   }

   @Override
   public String getSerialNumber() {
      return this.manufacturerModelSerialUUID.get().getC();
   }

   @Override
   public String getHardwareUUID() {
      return this.manufacturerModelSerialUUID.get().getD();
   }

   @Override
   public Firmware createFirmware() {
      return new MacFirmware();
   }

   @Override
   public Baseboard createBaseboard() {
      return new MacBaseboard();
   }

   private static Quartet<String, String, String, String> platformExpert() {
      String manufacturer = null;
      String model = null;
      String serialNumber = null;
      String uuid = null;
      IOKit.IORegistryEntry platformExpert = IOKitUtil.getMatchingService("IOPlatformExpertDevice");
      if (platformExpert != null) {
         byte[] data = platformExpert.getByteArrayProperty("manufacturer");
         if (data != null) {
            manufacturer = Native.toString(data, StandardCharsets.UTF_8);
         }

         data = platformExpert.getByteArrayProperty("model");
         if (data != null) {
            model = Native.toString(data, StandardCharsets.UTF_8);
         }

         serialNumber = platformExpert.getStringProperty("IOPlatformSerialNumber");
         uuid = platformExpert.getStringProperty("IOPlatformUUID");
         platformExpert.release();
      }

      return new Quartet<>(
         Util.isBlank(manufacturer) ? "Apple Inc." : manufacturer,
         Util.isBlank(model) ? "unknown" : model,
         Util.isBlank(serialNumber) ? "unknown" : serialNumber,
         Util.isBlank(uuid) ? "unknown" : uuid
      );
   }
}
