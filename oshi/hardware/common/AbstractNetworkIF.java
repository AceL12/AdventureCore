package oshi.hardware.common;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.util.FileUtil;
import oshi.util.FormatUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;

@ThreadSafe
public abstract class AbstractNetworkIF implements NetworkIF {
   private static final Logger LOG = LoggerFactory.getLogger(AbstractNetworkIF.class);
   private static final String OSHI_VM_MAC_ADDR_PROPERTIES = "oshi.vmmacaddr.properties";
   private NetworkInterface networkInterface;
   private String name;
   private String displayName;
   private int index;
   private long mtu;
   private String mac;
   private String[] ipv4;
   private Short[] subnetMasks;
   private String[] ipv6;
   private Short[] prefixLengths;
   private final Supplier<Properties> vmMacAddrProps = Memoizer.memoize(AbstractNetworkIF::queryVmMacAddrProps);

   protected AbstractNetworkIF(NetworkInterface netint) throws InstantiationException {
      this(netint, netint.getDisplayName());
   }

   protected AbstractNetworkIF(NetworkInterface netint, String displayName) throws InstantiationException {
      this.networkInterface = netint;

      try {
         this.name = this.networkInterface.getName();
         this.displayName = displayName;
         this.index = this.networkInterface.getIndex();
         this.mtu = ParseUtil.unsignedIntToLong(this.networkInterface.getMTU());
         byte[] hwmac = this.networkInterface.getHardwareAddress();
         if (hwmac != null) {
            List<String> octets = new ArrayList<>(6);

            for (byte b : hwmac) {
               octets.add(String.format("%02x", b));
            }

            this.mac = String.join(":", octets);
         } else {
            this.mac = "unknown";
         }

         ArrayList<String> ipv4list = new ArrayList<>();
         ArrayList<Short> subnetMaskList = new ArrayList<>();
         ArrayList<String> ipv6list = new ArrayList<>();
         ArrayList<Short> prefixLengthList = new ArrayList<>();

         for (InterfaceAddress interfaceAddress : this.networkInterface.getInterfaceAddresses()) {
            InetAddress address = interfaceAddress.getAddress();
            if (address.getHostAddress().length() > 0) {
               if (address.getHostAddress().contains(":")) {
                  ipv6list.add(address.getHostAddress().split("%")[0]);
                  prefixLengthList.add(interfaceAddress.getNetworkPrefixLength());
               } else {
                  ipv4list.add(address.getHostAddress());
                  subnetMaskList.add(interfaceAddress.getNetworkPrefixLength());
               }
            }
         }

         this.ipv4 = ipv4list.toArray(new String[0]);
         this.subnetMasks = subnetMaskList.toArray(new Short[0]);
         this.ipv6 = ipv6list.toArray(new String[0]);
         this.prefixLengths = prefixLengthList.toArray(new Short[0]);
      } catch (SocketException var11) {
         throw new InstantiationException(var11.getMessage());
      }
   }

   protected static List<NetworkInterface> getNetworkInterfaces(boolean includeLocalInterfaces) {
      List<NetworkInterface> interfaces = getAllNetworkInterfaces();
      return includeLocalInterfaces
         ? interfaces
         : getAllNetworkInterfaces().stream().parallel().filter(ni -> !isLocalInterface(ni)).collect(Collectors.toList());
   }

   private static List<NetworkInterface> getAllNetworkInterfaces() {
      try {
         Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
         return (List<NetworkInterface>)(interfaces == null ? Collections.emptyList() : Collections.list(interfaces));
      } catch (SocketException var1) {
         LOG.error("Socket exception when retrieving interfaces: {}", var1.getMessage());
         return Collections.emptyList();
      }
   }

   private static boolean isLocalInterface(NetworkInterface networkInterface) {
      try {
         return networkInterface.getHardwareAddress() == null;
      } catch (SocketException var2) {
         LOG.error("Socket exception when retrieving interface information for {}: {}", networkInterface, var2.getMessage());
         return false;
      }
   }

   @Override
   public NetworkInterface queryNetworkInterface() {
      return this.networkInterface;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public int getIndex() {
      return this.index;
   }

   @Override
   public String getDisplayName() {
      return this.displayName;
   }

   @Override
   public long getMTU() {
      return this.mtu;
   }

   @Override
   public String getMacaddr() {
      return this.mac;
   }

   @Override
   public String[] getIPv4addr() {
      return Arrays.copyOf(this.ipv4, this.ipv4.length);
   }

   @Override
   public Short[] getSubnetMasks() {
      return Arrays.copyOf(this.subnetMasks, this.subnetMasks.length);
   }

   @Override
   public String[] getIPv6addr() {
      return Arrays.copyOf(this.ipv6, this.ipv6.length);
   }

   @Override
   public Short[] getPrefixLengths() {
      return Arrays.copyOf(this.prefixLengths, this.prefixLengths.length);
   }

   @Override
   public boolean isKnownVmMacAddr() {
      String oui = this.getMacaddr().length() > 7 ? this.getMacaddr().substring(0, 8) : this.getMacaddr();
      return this.vmMacAddrProps.get().containsKey(oui.toUpperCase());
   }

   private static Properties queryVmMacAddrProps() {
      return FileUtil.readPropertiesFromFilename("oshi.vmmacaddr.properties");
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Name: ").append(this.getName());
      if (!this.getName().equals(this.getDisplayName())) {
         sb.append(" (").append(this.getDisplayName()).append(")");
      }

      if (!this.getIfAlias().isEmpty()) {
         sb.append(" [IfAlias=").append(this.getIfAlias()).append("]");
      }

      sb.append("\n");
      sb.append("  MAC Address: ").append(this.getMacaddr()).append("\n");
      sb.append("  MTU: ").append(this.getMTU()).append(", ").append("Speed: ").append(this.getSpeed()).append("\n");
      String[] ipv4withmask = this.getIPv4addr();
      if (this.ipv4.length == this.subnetMasks.length) {
         for (int i = 0; i < this.subnetMasks.length; i++) {
            ipv4withmask[i] = ipv4withmask[i] + "/" + this.subnetMasks[i];
         }
      }

      sb.append("  IPv4: ").append(Arrays.toString((Object[])ipv4withmask)).append("\n");
      String[] ipv6withprefixlength = this.getIPv6addr();
      if (this.ipv6.length == this.prefixLengths.length) {
         for (int j = 0; j < this.prefixLengths.length; j++) {
            ipv6withprefixlength[j] = ipv6withprefixlength[j] + "/" + this.prefixLengths[j];
         }
      }

      sb.append("  IPv6: ").append(Arrays.toString((Object[])ipv6withprefixlength)).append("\n");
      sb.append("  Traffic: received ")
         .append(this.getPacketsRecv())
         .append(" packets/")
         .append(FormatUtil.formatBytes(this.getBytesRecv()))
         .append(" (" + this.getInErrors() + " err, ")
         .append(this.getInDrops() + " drop);");
      sb.append(" transmitted ")
         .append(this.getPacketsSent())
         .append(" packets/")
         .append(FormatUtil.formatBytes(this.getBytesSent()))
         .append(" (" + this.getOutErrors() + " err, ")
         .append(this.getCollisions() + " coll);");
      return sb.toString();
   }
}
