package oshi.hardware;

import java.net.NetworkInterface;
import java.util.Arrays;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface NetworkIF {
   NetworkInterface queryNetworkInterface();

   String getName();

   int getIndex();

   String getDisplayName();

   default String getIfAlias() {
      return "";
   }

   default NetworkIF.IfOperStatus getIfOperStatus() {
      return NetworkIF.IfOperStatus.UNKNOWN;
   }

   long getMTU();

   String getMacaddr();

   String[] getIPv4addr();

   Short[] getSubnetMasks();

   String[] getIPv6addr();

   Short[] getPrefixLengths();

   default int getIfType() {
      return 0;
   }

   default int getNdisPhysicalMediumType() {
      return 0;
   }

   default boolean isConnectorPresent() {
      return false;
   }

   long getBytesRecv();

   long getBytesSent();

   long getPacketsRecv();

   long getPacketsSent();

   long getInErrors();

   long getOutErrors();

   long getInDrops();

   long getCollisions();

   long getSpeed();

   long getTimeStamp();

   boolean isKnownVmMacAddr();

   boolean updateAttributes();

   public static enum IfOperStatus {
      UP(1),
      DOWN(2),
      TESTING(3),
      UNKNOWN(4),
      DORMANT(5),
      NOT_PRESENT(6),
      LOWER_LAYER_DOWN(7);

      private final int value;

      private IfOperStatus(int value) {
         this.value = value;
      }

      public int getValue() {
         return this.value;
      }

      public static NetworkIF.IfOperStatus byValue(int value) {
         return Arrays.stream(values()).filter(st -> st.getValue() == value).findFirst().orElse(UNKNOWN);
      }
   }
}
