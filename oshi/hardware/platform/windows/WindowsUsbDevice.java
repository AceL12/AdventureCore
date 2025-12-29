package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.Guid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.DeviceTree;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quintet;
import oshi.util.tuples.Triplet;

@Immutable
public class WindowsUsbDevice extends AbstractUsbDevice {
   private static final Guid.GUID GUID_DEVINTERFACE_USB_HOST_CONTROLLER = new Guid.GUID("{3ABF6F2D-71C4-462A-8A92-1E6861E6AF27}");

   public WindowsUsbDevice(
      String name, String vendor, String vendorId, String productId, String serialNumber, String uniqueDeviceId, List<UsbDevice> connectedDevices
   ) {
      super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
   }

   public static List<UsbDevice> getUsbDevices(boolean tree) {
      List<UsbDevice> devices = queryUsbDevices();
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

   private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
      for (UsbDevice device : list) {
         deviceList.add(
            new WindowsUsbDevice(
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

   private static List<UsbDevice> queryUsbDevices() {
      Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> controllerDevices = DeviceTree.queryDeviceTree(
         GUID_DEVINTERFACE_USB_HOST_CONTROLLER
      );
      Map<Integer, Integer> parentMap = controllerDevices.getB();
      Map<Integer, String> nameMap = controllerDevices.getC();
      Map<Integer, String> deviceIdMap = controllerDevices.getD();
      Map<Integer, String> mfgMap = controllerDevices.getE();
      List<UsbDevice> usbDevices = new ArrayList<>();

      for (Integer controllerDevice : controllerDevices.getA()) {
         WindowsUsbDevice deviceAndChildren = queryDeviceAndChildren(controllerDevice, parentMap, nameMap, deviceIdMap, mfgMap, "0000", "0000", "");
         if (deviceAndChildren != null) {
            usbDevices.add(deviceAndChildren);
         }
      }

      return usbDevices;
   }

   private static WindowsUsbDevice queryDeviceAndChildren(
      Integer device,
      Map<Integer, Integer> parentMap,
      Map<Integer, String> nameMap,
      Map<Integer, String> deviceIdMap,
      Map<Integer, String> mfgMap,
      String vid,
      String pid,
      String parentSerial
   ) {
      String vendorId = vid;
      String productId = pid;
      String serial = parentSerial;
      Triplet<String, String, String> idsAndSerial = ParseUtil.parseDeviceIdToVendorProductSerial(deviceIdMap.get(device));
      if (idsAndSerial != null) {
         vendorId = idsAndSerial.getA();
         productId = idsAndSerial.getB();
         serial = idsAndSerial.getC();
         if (serial.isEmpty() && vendorId.equals(vid) && productId.equals(pid)) {
            serial = parentSerial;
         }
      }

      Set<Integer> childDeviceSet = parentMap.entrySet().stream().filter(e -> e.getValue().equals(device)).map(Entry::getKey).collect(Collectors.toSet());
      List<UsbDevice> childDevices = new ArrayList<>();

      for (Integer child : childDeviceSet) {
         WindowsUsbDevice deviceAndChildren = queryDeviceAndChildren(child, parentMap, nameMap, deviceIdMap, mfgMap, vendorId, productId, serial);
         if (deviceAndChildren != null) {
            childDevices.add(deviceAndChildren);
         }
      }

      Collections.sort(childDevices);
      if (nameMap.containsKey(device)) {
         String name = nameMap.get(device);
         if (name.isEmpty()) {
            name = vendorId + ":" + productId;
         }

         String deviceId = deviceIdMap.get(device);
         String mfg = mfgMap.get(device);
         return new WindowsUsbDevice(name, mfg, vendorId, productId, serial, deviceId, childDevices);
      } else {
         return null;
      }
   }
}
