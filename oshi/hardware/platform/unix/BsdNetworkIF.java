package oshi.hardware.platform.unix;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class BsdNetworkIF extends AbstractNetworkIF {
   private static final Logger LOG = LoggerFactory.getLogger(BsdNetworkIF.class);
   private long bytesRecv;
   private long bytesSent;
   private long packetsRecv;
   private long packetsSent;
   private long inErrors;
   private long outErrors;
   private long inDrops;
   private long collisions;
   private long timeStamp;

   public BsdNetworkIF(NetworkInterface netint) throws InstantiationException {
      super(netint);
      this.updateAttributes();
   }

   public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
      List<NetworkIF> ifList = new ArrayList<>();

      for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
         try {
            ifList.add(new BsdNetworkIF(ni));
         } catch (InstantiationException var5) {
            LOG.debug("Network Interface Instantiation failed: {}", var5.getMessage());
         }
      }

      return ifList;
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
      return 0L;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public boolean updateAttributes() {
      String stats = ExecutingCommand.getAnswerAt("netstat -bI " + this.getName(), 1);
      this.timeStamp = System.currentTimeMillis();
      String[] split = ParseUtil.whitespaces.split(stats);
      if (split.length < 12) {
         return false;
      } else {
         this.bytesSent = ParseUtil.parseUnsignedLongOrDefault(split[10], 0L);
         this.bytesRecv = ParseUtil.parseUnsignedLongOrDefault(split[7], 0L);
         this.packetsSent = ParseUtil.parseUnsignedLongOrDefault(split[8], 0L);
         this.packetsRecv = ParseUtil.parseUnsignedLongOrDefault(split[4], 0L);
         this.outErrors = ParseUtil.parseUnsignedLongOrDefault(split[9], 0L);
         this.inErrors = ParseUtil.parseUnsignedLongOrDefault(split[5], 0L);
         this.collisions = ParseUtil.parseUnsignedLongOrDefault(split[11], 0L);
         this.inDrops = ParseUtil.parseUnsignedLongOrDefault(split[6], 0L);
         return true;
      }
   }
}
