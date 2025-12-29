package oshi.hardware.platform.unix.openbsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;

@Immutable
public class OpenBsdUsbDevice extends AbstractUsbDevice {
   public OpenBsdUsbDevice(
      String name, String vendor, String vendorId, String productId, String serialNumber, String uniqueDeviceId, List<UsbDevice> connectedDevices
   ) {
      super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
   }

   public static List<UsbDevice> getUsbDevices(boolean tree) {
      List<UsbDevice> devices = getUsbDevices();
      if (tree) {
         return devices;
      } else {
         List<UsbDevice> deviceList = new ArrayList<>();

         for (UsbDevice device : devices) {
            deviceList.add(
               new OpenBsdUsbDevice(
                  device.getName(),
                  device.getVendor(),
                  device.getVendorId(),
                  device.getProductId(),
                  device.getSerialNumber(),
                  device.getUniqueDeviceId(),
                  Collections.emptyList()
               )
            );
            addDevicesToList(deviceList, device.getConnectedDevices());
         }

         return deviceList;
      }
   }

   private static List<UsbDevice> getUsbDevices() {
      Map<String, String> nameMap = new HashMap<>();
      Map<String, String> vendorMap = new HashMap<>();
      Map<String, String> vendorIdMap = new HashMap<>();
      Map<String, String> productIdMap = new HashMap<>();
      Map<String, String> serialMap = new HashMap<>();
      Map<String, List<String>> hubMap = new HashMap<>();
      List<String> rootHubs = new ArrayList<>();
      String key = "";
      String parent = "";

      for (String line : ExecutingCommand.runNative("usbdevs -v")) {
         if (line.startsWith("Controller ")) {
            parent = line.substring(11);
         } else if (line.startsWith("addr ")) {
            if (line.indexOf(58) == 7 && line.indexOf(44) >= 18) {
               key = parent + line.substring(0, 7);
               String[] split = line.substring(8).trim().split(",");
               if (split.length > 1) {
                  String vendorStr = split[0].trim();
                  int idx1 = vendorStr.indexOf(58);
                  int idx2 = vendorStr.indexOf(32);
                  if (idx1 >= 0 && idx2 >= 0) {
                     vendorIdMap.put(key, vendorStr.substring(0, idx1));
                     productIdMap.put(key, vendorStr.substring(idx1 + 1, idx2));
                     vendorMap.put(key, vendorStr.substring(idx2 + 1));
                  }

                  nameMap.put(key, split[1].trim());
                  hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(key);
                  if (!parent.contains("addr")) {
                     parent = key;
                     rootHubs.add(key);
                  }
               }
            }
         } else if (!key.isEmpty()) {
            int idx = line.indexOf("iSerial ");
            if (idx >= 0) {
               serialMap.put(key, line.substring(idx + 8).trim());
            }

            key = "";
         }
      }

      List<UsbDevice> controllerDevices = new ArrayList<>();

      for (String devusb : rootHubs) {
         controllerDevices.add(getDeviceAndChildren(devusb, "0000", "0000", nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap));
      }

      return controllerDevices;
   }

   private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
      for (UsbDevice device : list) {
         deviceList.add(device);
         addDevicesToList(deviceList, device.getConnectedDevices());
      }
   }

   private static OpenBsdUsbDevice getDeviceAndChildren(
      String devPath,
      String vid,
      String pid,
      Map<String, String> nameMap,
      Map<String, String> vendorMap,
      Map<String, String> vendorIdMap,
      Map<String, String> productIdMap,
      Map<String, String> serialMap,
      Map<String, List<String>> hubMap
   ) {
      String vendorId = vendorIdMap.getOrDefault(devPath, vid);
      String productId = productIdMap.getOrDefault(devPath, pid);
      List<String> childPaths = hubMap.getOrDefault(devPath, new ArrayList<>());
      List<UsbDevice> usbDevices = new ArrayList<>();

      for (String path : childPaths) {
         usbDevices.add(getDeviceAndChildren(path, vendorId, productId, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap));
      }

      Collections.sort(usbDevices);
      return new OpenBsdUsbDevice(
         nameMap.getOrDefault(devPath, vendorId + ":" + productId),
         vendorMap.getOrDefault(devPath, ""),
         vendorId,
         productId,
         serialMap.getOrDefault(devPath, ""),
         devPath,
         usbDevices
      );
   }
}
