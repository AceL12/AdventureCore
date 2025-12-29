package oshi.hardware.platform.unix.solaris;

import com.sun.jna.platform.unix.solaris.LibKstat;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.NetworkIF;
import oshi.hardware.common.AbstractNetworkIF;
import oshi.software.os.unix.solaris.SolarisOperatingSystem;
import oshi.util.platform.unix.solaris.KstatUtil;

@ThreadSafe
public final class SolarisNetworkIF extends AbstractNetworkIF {
   private static final Logger LOG = LoggerFactory.getLogger(SolarisNetworkIF.class);
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

   public SolarisNetworkIF(NetworkInterface netint) throws InstantiationException {
      super(netint);
      this.updateAttributes();
   }

   public static List<NetworkIF> getNetworks(boolean includeLocalInterfaces) {
      List<NetworkIF> ifList = new ArrayList<>();

      for (NetworkInterface ni : getNetworkInterfaces(includeLocalInterfaces)) {
         try {
            ifList.add(new SolarisNetworkIF(ni));
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
      return this.speed;
   }

   @Override
   public long getTimeStamp() {
      return this.timeStamp;
   }

   @Override
   public boolean updateAttributes() {
      this.timeStamp = System.currentTimeMillis();
      if (SolarisOperatingSystem.HAS_KSTAT2) {
         return this.updateAttributes2();
      } else {
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         boolean var3;
         label50: {
            try {
               LibKstat.Kstat ksp = kc.lookup("link", -1, this.getName());
               if (ksp == null) {
                  ksp = kc.lookup(null, -1, this.getName());
               }

               if (ksp != null && kc.read(ksp)) {
                  this.bytesSent = KstatUtil.dataLookupLong(ksp, "obytes64");
                  this.bytesRecv = KstatUtil.dataLookupLong(ksp, "rbytes64");
                  this.packetsSent = KstatUtil.dataLookupLong(ksp, "opackets64");
                  this.packetsRecv = KstatUtil.dataLookupLong(ksp, "ipackets64");
                  this.outErrors = KstatUtil.dataLookupLong(ksp, "oerrors");
                  this.inErrors = KstatUtil.dataLookupLong(ksp, "ierrors");
                  this.collisions = KstatUtil.dataLookupLong(ksp, "collisions");
                  this.inDrops = KstatUtil.dataLookupLong(ksp, "dl_idrops");
                  this.speed = KstatUtil.dataLookupLong(ksp, "ifspeed");
                  this.timeStamp = ksp.ks_snaptime / 1000000L;
                  var3 = true;
                  break label50;
               }
            } catch (Throwable var5) {
               if (kc != null) {
                  try {
                     kc.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (kc != null) {
               kc.close();
            }

            return false;
         }

         if (kc != null) {
            kc.close();
         }

         return var3;
      }
   }

   private boolean updateAttributes2() {
      Object[] results = KstatUtil.queryKstat2(
         "kstat:/net/link/" + this.getName() + "/0",
         "obytes64",
         "rbytes64",
         "opackets64",
         "ipackets64",
         "oerrors",
         "ierrors",
         "collisions",
         "dl_idrops",
         "ifspeed",
         "snaptime"
      );
      if (results[results.length - 1] == null) {
         return false;
      } else {
         this.bytesSent = results[0] == null ? 0L : (Long)results[0];
         this.bytesRecv = results[1] == null ? 0L : (Long)results[1];
         this.packetsSent = results[2] == null ? 0L : (Long)results[2];
         this.packetsRecv = results[3] == null ? 0L : (Long)results[3];
         this.outErrors = results[4] == null ? 0L : (Long)results[4];
         this.collisions = results[5] == null ? 0L : (Long)results[5];
         this.inDrops = results[6] == null ? 0L : (Long)results[6];
         this.speed = results[7] == null ? 0L : (Long)results[7];
         this.timeStamp = (Long)results[8] / 1000000L;
         return true;
      }
   }
}
