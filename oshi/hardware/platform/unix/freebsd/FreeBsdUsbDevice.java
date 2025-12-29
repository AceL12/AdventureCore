package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@Immutable
public class FreeBsdUsbDevice extends AbstractUsbDevice {
   public FreeBsdUsbDevice(
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
               new FreeBsdUsbDevice(
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
      Map<String, String> parentMap = new HashMap<>();
      Map<String, List<String>> hubMap = new HashMap<>();
      List<String> devices = ExecutingCommand.runNative("lshal");
      if (devices.isEmpty()) {
         return Collections.emptyList();
      } else {
         String key = "";
         List<String> usBuses = new ArrayList<>();

         for (String line : devices) {
            if (line.startsWith("udi =")) {
               key = ParseUtil.getSingleQuoteStringValue(line);
            } else if (!key.isEmpty()) {
               line = line.trim();
               if (!line.isEmpty()) {
                  if (line.startsWith("freebsd.driver =") && "usbus".equals(ParseUtil.getSingleQuoteStringValue(line))) {
                     usBuses.add(key);
                  } else if (line.contains(".parent =")) {
                     String parent = ParseUtil.getSingleQuoteStringValue(line);
                     if (!key.replace(parent, "").startsWith("_if")) {
                        parentMap.put(key, parent);
                        hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(key);
                     }
                  } else if (line.contains(".product =")) {
                     nameMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
                  } else if (line.contains(".vendor =")) {
                     vendorMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
                  } else if (line.contains(".serial =")) {
                     String serial = ParseUtil.getSingleQuoteStringValue(line);
                     serialMap.put(key, serial.startsWith("0x") ? ParseUtil.hexStringToString(serial.replace("0x", "")) : serial);
                  } else if (line.contains(".vendor_id =")) {
                     vendorIdMap.put(key, String.format("%04x", ParseUtil.getFirstIntValue(line)));
                  } else if (line.contains(".product_id =")) {
                     productIdMap.put(key, String.format("%04x", ParseUtil.getFirstIntValue(line)));
                  }
               }
            }
         }

         List<UsbDevice> controllerDevices = new ArrayList<>();

         for (String usbus : usBuses) {
            String parent = parentMap.get(usbus);
            hubMap.put(parent, hubMap.get(usbus));
            controllerDevices.add(getDeviceAndChildren(parent, "0000", "0000", nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap));
         }

         return controllerDevices;
      }
   }

   private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
      for (UsbDevice device : list) {
         deviceList.add(device);
         addDevicesToList(deviceList, device.getConnectedDevices());
      }
   }

   private static FreeBsdUsbDevice getDeviceAndChildren(
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
      return new FreeBsdUsbDevice(
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
