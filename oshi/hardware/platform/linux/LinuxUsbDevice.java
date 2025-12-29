package oshi.hardware.platform.linux;

import com.sun.jna.platform.linux.Udev;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.software.os.linux.LinuxOperatingSystem;

@Immutable
public class LinuxUsbDevice extends AbstractUsbDevice {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxUsbDevice.class);
   private static final String SUBSYSTEM_USB = "usb";
   private static final String DEVTYPE_USB_DEVICE = "usb_device";
   private static final String ATTR_PRODUCT = "product";
   private static final String ATTR_MANUFACTURER = "manufacturer";
   private static final String ATTR_VENDOR_ID = "idVendor";
   private static final String ATTR_PRODUCT_ID = "idProduct";
   private static final String ATTR_SERIAL = "serial";

   public LinuxUsbDevice(
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
               new LinuxUsbDevice(
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
      if (!LinuxOperatingSystem.HAS_UDEV) {
         LOG.warn("USB Device information requires libudev, which is not present.");
         return Collections.emptyList();
      } else {
         List<String> usbControllers = new ArrayList<>();
         Map<String, String> nameMap = new HashMap<>();
         Map<String, String> vendorMap = new HashMap<>();
         Map<String, String> vendorIdMap = new HashMap<>();
         Map<String, String> productIdMap = new HashMap<>();
         Map<String, String> serialMap = new HashMap<>();
         Map<String, List<String>> hubMap = new HashMap<>();
         Udev.UdevContext udev = Udev.INSTANCE.udev_new();

         try {
            Udev.UdevEnumerate enumerate = udev.enumerateNew();

            try {
               enumerate.addMatchSubsystem("usb");
               enumerate.scanDevices();

               for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                  String syspath = entry.getName();
                  Udev.UdevDevice device = udev.deviceNewFromSyspath(syspath);
                  if (device != null) {
                     try {
                        if ("usb_device".equals(device.getDevtype())) {
                           String value = device.getSysattrValue("product");
                           if (value != null) {
                              nameMap.put(syspath, value);
                           }

                           value = device.getSysattrValue("manufacturer");
                           if (value != null) {
                              vendorMap.put(syspath, value);
                           }

                           value = device.getSysattrValue("idVendor");
                           if (value != null) {
                              vendorIdMap.put(syspath, value);
                           }

                           value = device.getSysattrValue("idProduct");
                           if (value != null) {
                              productIdMap.put(syspath, value);
                           }

                           value = device.getSysattrValue("serial");
                           if (value != null) {
                              serialMap.put(syspath, value);
                           }

                           Udev.UdevDevice parent = device.getParentWithSubsystemDevtype("usb", "usb_device");
                           if (parent == null) {
                              usbControllers.add(syspath);
                           } else {
                              String parentPath = parent.getSyspath();
                              hubMap.computeIfAbsent(parentPath, x -> new ArrayList<>()).add(syspath);
                           }
                        }
                     } finally {
                        device.unref();
                     }
                  }
               }
            } finally {
               enumerate.unref();
            }
         } finally {
            udev.unref();
         }

         ArrayList var30 = new ArrayList();

         for (String controller : usbControllers) {
            var30.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap));
         }

         return var30;
      }
   }

   private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
      for (UsbDevice device : list) {
         deviceList.add(device);
         addDevicesToList(deviceList, device.getConnectedDevices());
      }
   }

   private static LinuxUsbDevice getDeviceAndChildren(
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
      return new LinuxUsbDevice(
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
