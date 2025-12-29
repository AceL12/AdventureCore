package oshi.hardware.platform.mac;

import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

@Immutable
public class MacUsbDevice extends AbstractUsbDevice {
   private static final CoreFoundation CF = CoreFoundation.INSTANCE;
   private static final String IOUSB = "IOUSB";
   private static final String IOSERVICE = "IOService";

   public MacUsbDevice(
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
            addDevicesToList(deviceList, device.getConnectedDevices());
         }

         return deviceList;
      }
   }

   private static List<UsbDevice> getUsbDevices() {
      Map<Long, String> nameMap = new HashMap<>();
      Map<Long, String> vendorMap = new HashMap<>();
      Map<Long, String> vendorIdMap = new HashMap<>();
      Map<Long, String> productIdMap = new HashMap<>();
      Map<Long, String> serialMap = new HashMap<>();
      Map<Long, List<Long>> hubMap = new HashMap<>();
      List<Long> usbControllers = new ArrayList<>();
      IOKit.IORegistryEntry root = IOKitUtil.getRoot();
      IOKit.IOIterator iter = root.getChildIterator("IOUSB");
      if (iter != null) {
         CoreFoundation.CFStringRef locationIDKey = CoreFoundation.CFStringRef.createCFString("locationID");
         CoreFoundation.CFStringRef ioPropertyMatchKey = CoreFoundation.CFStringRef.createCFString("IOPropertyMatch");

         for (IOKit.IORegistryEntry device = iter.next(); device != null; device = iter.next()) {
            long id = 0L;
            IOKit.IORegistryEntry controller = device.getParentEntry("IOService");
            if (controller != null) {
               id = controller.getRegistryEntryID();
               nameMap.put(id, controller.getName());
               CoreFoundation.CFTypeRef ref = controller.createCFProperty(locationIDKey);
               if (ref != null) {
                  getControllerIdByLocation(id, ref, locationIDKey, ioPropertyMatchKey, vendorIdMap, productIdMap);
                  ref.release();
               }

               controller.release();
            }

            usbControllers.add(id);
            addDeviceAndChildrenToMaps(device, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap);
            device.release();
         }

         locationIDKey.release();
         ioPropertyMatchKey.release();
         iter.release();
      }

      root.release();
      List<UsbDevice> controllerDevices = new ArrayList<>();

      for (Long controller : usbControllers) {
         controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap));
      }

      return controllerDevices;
   }

   private static void addDeviceAndChildrenToMaps(
      IOKit.IORegistryEntry device,
      long parentId,
      Map<Long, String> nameMap,
      Map<Long, String> vendorMap,
      Map<Long, String> vendorIdMap,
      Map<Long, String> productIdMap,
      Map<Long, String> serialMap,
      Map<Long, List<Long>> hubMap
   ) {
      long id = device.getRegistryEntryID();
      hubMap.computeIfAbsent(parentId, x -> new ArrayList<>()).add(id);
      nameMap.put(id, device.getName().trim());
      String vendor = device.getStringProperty("USB Vendor Name");
      if (vendor != null) {
         vendorMap.put(id, vendor.trim());
      }

      Long vendorId = device.getLongProperty("idVendor");
      if (vendorId != null) {
         vendorIdMap.put(id, String.format("%04x", 65535L & vendorId));
      }

      Long productId = device.getLongProperty("idProduct");
      if (productId != null) {
         productIdMap.put(id, String.format("%04x", 65535L & productId));
      }

      String serial = device.getStringProperty("USB Serial Number");
      if (serial != null) {
         serialMap.put(id, serial.trim());
      }

      IOKit.IOIterator childIter = device.getChildIterator("IOUSB");

      for (IOKit.IORegistryEntry childDevice = childIter.next(); childDevice != null; childDevice = childIter.next()) {
         addDeviceAndChildrenToMaps(childDevice, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap);
         childDevice.release();
      }

      childIter.release();
   }

   private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
      for (UsbDevice device : list) {
         deviceList.add(
            new MacUsbDevice(
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
   }

   private static void getControllerIdByLocation(
      long id,
      CoreFoundation.CFTypeRef locationId,
      CoreFoundation.CFStringRef locationIDKey,
      CoreFoundation.CFStringRef ioPropertyMatchKey,
      Map<Long, String> vendorIdMap,
      Map<Long, String> productIdMap
   ) {
      CoreFoundation.CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CoreFoundation.CFIndex(0L), null, null);
      propertyDict.setValue(locationIDKey, locationId);
      CoreFoundation.CFMutableDictionaryRef matchingDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CoreFoundation.CFIndex(0L), null, null);
      matchingDict.setValue(ioPropertyMatchKey, propertyDict);
      IOKit.IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
      propertyDict.release();
      boolean found = false;
      if (serviceIterator != null) {
         for (IOKit.IORegistryEntry matchingService = serviceIterator.next(); matchingService != null && !found; matchingService = serviceIterator.next()) {
            IOKit.IORegistryEntry parent = matchingService.getParentEntry("IOService");
            if (parent != null) {
               byte[] vid = parent.getByteArrayProperty("vendor-id");
               if (vid != null && vid.length >= 2) {
                  vendorIdMap.put(id, String.format("%02x%02x", vid[1], vid[0]));
                  found = true;
               }

               byte[] pid = parent.getByteArrayProperty("device-id");
               if (pid != null && pid.length >= 2) {
                  productIdMap.put(id, String.format("%02x%02x", pid[1], pid[0]));
                  found = true;
               }

               parent.release();
            }

            matchingService.release();
         }

         serviceIterator.release();
      }
   }

   private static MacUsbDevice getDeviceAndChildren(
      Long registryEntryId,
      String vid,
      String pid,
      Map<Long, String> nameMap,
      Map<Long, String> vendorMap,
      Map<Long, String> vendorIdMap,
      Map<Long, String> productIdMap,
      Map<Long, String> serialMap,
      Map<Long, List<Long>> hubMap
   ) {
      String vendorId = vendorIdMap.getOrDefault(registryEntryId, vid);
      String productId = productIdMap.getOrDefault(registryEntryId, pid);
      List<Long> childIds = hubMap.getOrDefault(registryEntryId, new ArrayList<>());
      List<UsbDevice> usbDevices = new ArrayList<>();

      for (Long id : childIds) {
         usbDevices.add(getDeviceAndChildren(id, vendorId, productId, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap));
      }

      Collections.sort(usbDevices);
      return new MacUsbDevice(
         nameMap.getOrDefault(registryEntryId, vendorId + ":" + productId),
         vendorMap.getOrDefault(registryEntryId, ""),
         vendorId,
         productId,
         serialMap.getOrDefault(registryEntryId, ""),
         "0x" + Long.toHexString(registryEntryId),
         usbDevices
      );
   }
}
