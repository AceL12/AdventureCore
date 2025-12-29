package oshi.hardware.platform.linux;

import com.sun.jna.platform.linux.Udev;
import java.io.File;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.FileUtil;
import oshi.util.Util;

@ThreadSafe
public final class LinuxNetworkIF extends AbstractNetworkIF {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkIF.class);
   private int ifType;
   private boolean connectorPresent;
   private long bytesRecv;
   private long bytesSent;
   private long packetsRecv;
   private long packetsSent;
   private long inErrors;
   private long outErrors;
   private long inDrops;
   private long collisions;
   private long speed;
   private long timeStamp;
   private String ifAlias = "";
   private NetworkIF.IfOperStatus ifOperStatus = NetworkIF.IfOperStatus.UNKNOWN;

   public LinuxNetworkIF(NetworkInterface netint) throws InstantiationException {
      super(netint, queryIfModel(netint));
      this.updateAttributes();
   }

   private static String queryIfModel(NetworkInterface netint) {
      String name = netint.getName();
      if (!LinuxOperatingSystem.HAS_UDEV) {
         return queryIfModelFromSysfs(name);
      } else {
         Udev.UdevContext udev = Udev.INSTANCE.udev_new();
         if (udev != null) {
            String var6;
            try {
               Udev.UdevDevice device = udev.deviceNewFromSyspath("/sys/class/net/" + name);
               if (device == null) {
                  return name;
               }

               try {
                  String devVendor = device.getPropertyValue("ID_VENDOR_FROM_DATABASE");
                  String devModel = device.getPropertyValue("ID_MODEL_FROM_DATABASE");
                  if (Util.isBlank(devModel)) {
                     return name;
                  }

                  if (!Util.isBlank(devVendor)) {
                     return devVendor + " " + devModel;
                  }

                  var6 = devModel;
               } finally {
                  device.unref();
               }
            } finally {
               udev.unref();
            }

            return var6;
         } else {
            return name;
         }
      }
   }

   private static String queryIfModelFromSysfs(String name) {
      Map<String, String> uevent = FileUtil.getKeyValueMapFromFile("/sys/class/net/" + name + "/uevent", "=");
      String devVendor = uevent.get("ID_VENDOR_FROM_DATABASE");
      String devModel = uevent.get("ID_MODEL_FROM_DATABASE");
      if (!Util.isBlank(devModel)) {
         return !Util.isBlank(devVendor) ? devVendor + " " + devModel : devModel;
      } else {
         return name;
      }
   }

   public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
      List<NetworkIF> ifList = new ArrayList<>();

      for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
         try {
            ifList.add(new LinuxNetworkIF(ni));
         } catch (InstantiationException var5) {
            LOG.debug("Network Interface Instantiation failed: {}", var5.getMessage());
         }
      }

      return ifList;
   }

   @Override
   public int getIfType() {
      return this.ifType;
   }

   @Override
   public boolean isConnectorPresent() {
      return this.connectorPresent;
   }

   @Override
   public long getBytesRecv() {
      return this.bytesRecv;
   }

   @Override
   public long getBytesSent() {
      return this.bytesSent;
   }

   @Override
   public long getPacketsRecv() {
      return this.packetsRecv;
   }

   @Override
   public long getPacketsSent() {
      return this.packetsSent;
   }

   @Override
   public long getInErrors() {
      return this.inErrors;
   }

   @Override
   public long getOutErrors() {
      return this.outErrors;
   }

   @Override
   public long getInDrops() {
      return this.inDrops;
   }

   @Override
   public long getCollisions() {
      return this.collisions;
   }

   @Override
   public long getSpeed() {
      return this.speed;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public String getIfAlias() {
      return this.ifAlias;
   }

   @Override
   public NetworkIF.IfOperStatus getIfOperStatus() {
      return this.ifOperStatus;
   }

   @Override
   public boolean updateAttributes() {
      try {
         File ifDir = new File(String.format("/sys/class/net/%s/statistics", this.getName()));
         if (!ifDir.isDirectory()) {
            return false;
         }
      } catch (SecurityException var16) {
         return false;
      }

      String ifTypePath = String.format("/sys/class/net/%s/type", this.getName());
      String carrierPath = String.format("/sys/class/net/%s/carrier", this.getName());
      String txBytesPath = String.format("/sys/class/net/%s/statistics/tx_bytes", this.getName());
      String rxBytesPath = String.format("/sys/class/net/%s/statistics/rx_bytes", this.getName());
      String txPacketsPath = String.format("/sys/class/net/%s/statistics/tx_packets", this.getName());
      String rxPacketsPath = String.format("/sys/class/net/%s/statistics/rx_packets", this.getName());
      String txErrorsPath = String.format("/sys/class/net/%s/statistics/tx_errors", this.getName());
      String rxErrorsPath = String.format("/sys/class/net/%s/statistics/rx_errors", this.getName());
      String collisionsPath = String.format("/sys/class/net/%s/statistics/collisions", this.getName());
      String rxDropsPath = String.format("/sys/class/net/%s/statistics/rx_dropped", this.getName());
      String ifSpeed = String.format("/sys/class/net/%s/speed", this.getName());
      String ifAliasPath = String.format("/sys/class/net/%s/ifalias", this.getName());
      String ifOperStatusPath = String.format("/sys/class/net/%s/operstate", this.getName());
      this.timeStamp = System.currentTimeMillis();
      this.ifType = FileUtil.getIntFromFile(ifTypePath);
      this.connectorPresent = FileUtil.getIntFromFile(carrierPath) > 0;
      this.bytesSent = FileUtil.getUnsignedLongFromFile(txBytesPath);
      this.bytesRecv = FileUtil.getUnsignedLongFromFile(rxBytesPath);
      this.packetsSent = FileUtil.getUnsignedLongFromFile(txPacketsPath);
      this.packetsRecv = FileUtil.getUnsignedLongFromFile(rxPacketsPath);
      this.outErrors = FileUtil.getUnsignedLongFromFile(txErrorsPath);
      this.inErrors = FileUtil.getUnsignedLongFromFile(rxErrorsPath);
      this.collisions = FileUtil.getUnsignedLongFromFile(collisionsPath);
      this.inDrops = FileUtil.getUnsignedLongFromFile(rxDropsPath);
      long speedMiB = FileUtil.getUnsignedLongFromFile(ifSpeed);
      this.speed = speedMiB < 0L ? 0L : speedMiB << 20;
      this.ifAlias = FileUtil.getStringFromFile(ifAliasPath);
      this.ifOperStatus = parseIfOperStatus(FileUtil.getStringFromFile(ifOperStatusPath));
      return true;
   }

   private static NetworkIF.IfOperStatus parseIfOperStatus(String operState) {
      switch (operState) {
         case "up":
            return NetworkIF.IfOperStatus.UP;
         case "down":
            return NetworkIF.IfOperStatus.DOWN;
         case "testing":
            return NetworkIF.IfOperStatus.TESTING;
         case "dormant":
            return NetworkIF.IfOperStatus.DORMANT;
         case "notpresent":
            return NetworkIF.IfOperStatus.NOT_PRESENT;
         case "lowerlayerdown":
            return NetworkIF.IfOperStatus.LOWER_LAYER_DOWN;
         case "unknown":
         default:
            return NetworkIF.IfOperStatus.UNKNOWN;
      }
   }
}
