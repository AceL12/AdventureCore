package oshi.driver.mac.net;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.unix.LibCAPI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;

@ThreadSafe
public final class NetStat {
   private static final Logger LOG = LoggerFactory.getLogger(NetStat.class);
   private static final int CTL_NET = 4;
   private static final int PF_ROUTE = 17;
   private static final int NET_RT_IFLIST2 = 6;
   private static final int RTM_IFINFO2 = 18;

   private NetStat() {
   }

   public static Map<Integer, NetStat.IFdata> queryIFdata(int index) {
      Map<Integer, NetStat.IFdata> data = new HashMap<>();
      int[] mib = new int[]{4, 17, 0, 0, 6, 0};
      ByRef.CloseableSizeTByReference len = new ByRef.CloseableSizeTByReference();

      Object var17;
      label85: {
         Object var18;
         label84: {
            Object var12;
            label83: {
               try {
                  if (0 != SystemB.INSTANCE.sysctl(mib, 6, null, len, null, LibCAPI.size_t.ZERO)) {
                     LOG.error("Didn't get buffer length for IFLIST2");
                     var17 = data;
                     break label85;
                  }

                  Memory buf = new Memory(len.longValue());

                  label79: {
                     label78: {
                        try {
                           if (0 != SystemB.INSTANCE.sysctl(mib, 6, buf, len, null, LibCAPI.size_t.ZERO)) {
                              LOG.error("Didn't get buffer for IFLIST2");
                              var18 = data;
                              break label78;
                           }

                           long now = System.currentTimeMillis();
                           int lim = (int)(buf.size() - new SystemB.IFmsgHdr().size());
                           int offset = 0;

                           while (true) {
                              if (offset >= lim) {
                                 break label79;
                              }

                              Pointer p = buf.share(offset);
                              SystemB.IFmsgHdr ifm = new SystemB.IFmsgHdr(p);
                              ifm.read();
                              offset += ifm.ifm_msglen;
                              if (ifm.ifm_type == 18) {
                                 SystemB.IFmsgHdr2 if2m = new SystemB.IFmsgHdr2(p);
                                 if2m.read();
                                 if (index < 0 || index == if2m.ifm_index) {
                                    data.put(
                                       Integer.valueOf(if2m.ifm_index),
                                       new NetStat.IFdata(
                                          255 & if2m.ifm_data.ifi_type,
                                          if2m.ifm_data.ifi_opackets,
                                          if2m.ifm_data.ifi_ipackets,
                                          if2m.ifm_data.ifi_obytes,
                                          if2m.ifm_data.ifi_ibytes,
                                          if2m.ifm_data.ifi_oerrors,
                                          if2m.ifm_data.ifi_ierrors,
                                          if2m.ifm_data.ifi_collisions,
                                          if2m.ifm_data.ifi_iqdrops,
                                          if2m.ifm_data.ifi_baudrate,
                                          now
                                       )
                                    );
                                    if (index >= 0) {
                                       var12 = data;
                                       break;
                                    }
                                 }
                              }
                           }
                        } catch (Throwable var15) {
                           try {
                              buf.close();
                           } catch (Throwable var14) {
                              var15.addSuppressed(var14);
                           }

                           throw var15;
                        }

                        buf.close();
                        break label83;
                     }

                     buf.close();
                     break label84;
                  }

                  buf.close();
               } catch (Throwable var16) {
                  try {
                     len.close();
                  } catch (Throwable var13) {
                     var16.addSuppressed(var13);
                  }

                  throw var16;
               }

               len.close();
               return data;
            }

            len.close();
            return (Map<Integer, NetStat.IFdata>)var12;
         }

         len.close();
         return (Map<Integer, NetStat.IFdata>)var18;
      }

      len.close();
      return (Map<Integer, NetStat.IFdata>)var17;
   }

   @Immutable
   public static class IFdata {
      private final int ifType;
      private final long oPackets;
      private final long iPackets;
      private final long oBytes;
      private final long iBytes;
      private final long oErrors;
      private final long iErrors;
      private final long collisions;
      private final long iDrops;
      private final long speed;
      private final long timeStamp;

      IFdata(
         int ifType,
         long oPackets,
         long iPackets,
         long oBytes,
         long iBytes,
         long oErrors,
         long iErrors,
         long collisions,
         long iDrops,
         long speed,
         long timeStamp
      ) {
         this.ifType = ifType;
         this.oPackets = oPackets & 4294967295L;
         this.iPackets = iPackets & 4294967295L;
         this.oBytes = oBytes & 4294967295L;
         this.iBytes = iBytes & 4294967295L;
         this.oErrors = oErrors & 4294967295L;
         this.iErrors = iErrors & 4294967295L;
         this.collisions = collisions & 4294967295L;
         this.iDrops = iDrops & 4294967295L;
         this.speed = speed & 4294967295L;
         this.timeStamp = timeStamp;
      }

      public int getIfType() {
         return this.ifType;
      }

      public long getOPackets() {
         return this.oPackets;
      }

      public long getIPackets() {
         return this.iPackets;
      }

      public long getOBytes() {
         return this.oBytes;
      }

      public long getIBytes() {
         return this.iBytes;
      }

      public long getOErrors() {
         return this.oErrors;
      }

      public long getIErrors() {
         return this.iErrors;
      }

      public long getCollisions() {
         return this.collisions;
      }

      public long getIDrops() {
         return this.iDrops;
      }

      public long getSpeed() {
         return this.speed;
      }

      public long getTimeStamp() {
         return this.timeStamp;
      }
   }
}
