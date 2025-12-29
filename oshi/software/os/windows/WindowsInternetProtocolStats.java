package oshi.software.os.windows;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.VersionHelpers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.software.os.InternetProtocolStats;
import oshi.util.ParseUtil;

@ThreadSafe
public class WindowsInternetProtocolStats extends AbstractInternetProtocolStats {
   private static final IPHlpAPI IPHLP = IPHlpAPI.INSTANCE;
   private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

   @Override
   public InternetProtocolStats.TcpStats getTCPv4Stats() {
      Struct.CloseableMibTcpStats stats = new Struct.CloseableMibTcpStats();

      InternetProtocolStats.TcpStats var2;
      try {
         IPHLP.GetTcpStatisticsEx(stats, 2);
         var2 = new InternetProtocolStats.TcpStats(
            stats.dwCurrEstab,
            stats.dwActiveOpens,
            stats.dwPassiveOpens,
            stats.dwAttemptFails,
            stats.dwEstabResets,
            stats.dwOutSegs,
            stats.dwInSegs,
            stats.dwRetransSegs,
            stats.dwInErrs,
            stats.dwOutRsts
         );
      } catch (Throwable var5) {
         try {
            stats.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      stats.close();
      return var2;
   }

   @Override
   public InternetProtocolStats.TcpStats getTCPv6Stats() {
      Struct.CloseableMibTcpStats stats = new Struct.CloseableMibTcpStats();

      InternetProtocolStats.TcpStats var2;
      try {
         IPHLP.GetTcpStatisticsEx(stats, 23);
         var2 = new InternetProtocolStats.TcpStats(
            stats.dwCurrEstab,
            stats.dwActiveOpens,
            stats.dwPassiveOpens,
            stats.dwAttemptFails,
            stats.dwEstabResets,
            stats.dwOutSegs,
            stats.dwInSegs,
            stats.dwRetransSegs,
            stats.dwInErrs,
            stats.dwOutRsts
         );
      } catch (Throwable var5) {
         try {
            stats.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      stats.close();
      return var2;
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv4Stats() {
      Struct.CloseableMibUdpStats stats = new Struct.CloseableMibUdpStats();

      InternetProtocolStats.UdpStats var2;
      try {
         IPHLP.GetUdpStatisticsEx(stats, 2);
         var2 = new InternetProtocolStats.UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
      } catch (Throwable var5) {
         try {
            stats.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      stats.close();
      return var2;
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv6Stats() {
      Struct.CloseableMibUdpStats stats = new Struct.CloseableMibUdpStats();

      InternetProtocolStats.UdpStats var2;
      try {
         IPHLP.GetUdpStatisticsEx(stats, 23);
         var2 = new InternetProtocolStats.UdpStats(stats.dwOutDatagrams, stats.dwInDatagrams, stats.dwNoPorts, stats.dwInErrors);
      } catch (Throwable var5) {
         try {
            stats.close();
         } catch (Throwable var4) {
            var5.addSuppressed(var4);
         }

         throw var5;
      }

      stats.close();
      return var2;
   }

   @Override
   public List<InternetProtocolStats.IPConnection> getConnections() {
      if (IS_VISTA_OR_GREATER) {
         List<InternetProtocolStats.IPConnection> conns = new ArrayList<>();
         conns.addAll(queryTCPv4Connections());
         conns.addAll(queryTCPv6Connections());
         conns.addAll(queryUDPv4Connections());
         conns.addAll(queryUDPv6Connections());
         return conns;
      } else {
         return Collections.emptyList();
      }
   }

   private static List<InternetProtocolStats.IPConnection> queryTCPv4Connections() {
      List<InternetProtocolStats.IPConnection> conns = new ArrayList<>();
      ByRef.CloseableIntByReference sizePtr = new ByRef.CloseableIntByReference();

      try {
         int ret = IPHLP.GetExtendedTcpTable(null, sizePtr, false, 2, 5, 0);
         if (ret == 0) {
            int size = sizePtr.getValue();
            Memory buf = new Memory(size);

            do {
               ret = IPHLP.GetExtendedTcpTable(buf, sizePtr, false, 2, 5, 0);
               if (ret == 122) {
                  size = sizePtr.getValue();
                  buf.close();
                  buf = new Memory(size);
               }
            } while (ret == 122);

            IPHlpAPI.MIB_TCPTABLE_OWNER_PID tcpTable = new IPHlpAPI.MIB_TCPTABLE_OWNER_PID(buf);

            for (int i = 0; i < tcpTable.dwNumEntries; i++) {
               IPHlpAPI.MIB_TCPROW_OWNER_PID row = tcpTable.table[i];
               conns.add(
                  new InternetProtocolStats.IPConnection(
                     "tcp4",
                     ParseUtil.parseIntToIP(row.dwLocalAddr),
                     ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort),
                     ParseUtil.parseIntToIP(row.dwRemoteAddr),
                     ParseUtil.bigEndian16ToLittleEndian(row.dwRemotePort),
                     stateLookup(row.dwState),
                     0,
                     0,
                     row.dwOwningPid
                  )
               );
            }

            buf.close();
         }
      } catch (Throwable var9) {
         try {
            sizePtr.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      sizePtr.close();
      return conns;
   }

   private static List<InternetProtocolStats.IPConnection> queryTCPv6Connections() {
      List<InternetProtocolStats.IPConnection> conns = new ArrayList<>();
      ByRef.CloseableIntByReference sizePtr = new ByRef.CloseableIntByReference();

      try {
         int ret = IPHLP.GetExtendedTcpTable(null, sizePtr, false, 23, 5, 0);
         if (ret == 0) {
            int size = sizePtr.getValue();
            Memory buf = new Memory(size);

            do {
               ret = IPHLP.GetExtendedTcpTable(buf, sizePtr, false, 23, 5, 0);
               if (ret == 122) {
                  size = sizePtr.getValue();
                  buf.close();
                  buf = new Memory(size);
               }
            } while (ret == 122);

            IPHlpAPI.MIB_TCP6TABLE_OWNER_PID tcpTable = new IPHlpAPI.MIB_TCP6TABLE_OWNER_PID(buf);

            for (int i = 0; i < tcpTable.dwNumEntries; i++) {
               IPHlpAPI.MIB_TCP6ROW_OWNER_PID row = tcpTable.table[i];
               conns.add(
                  new InternetProtocolStats.IPConnection(
                     "tcp6",
                     row.LocalAddr,
                     ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort),
                     row.RemoteAddr,
                     ParseUtil.bigEndian16ToLittleEndian(row.dwRemotePort),
                     stateLookup(row.State),
                     0,
                     0,
                     row.dwOwningPid
                  )
               );
            }

            buf.close();
         }
      } catch (Throwable var9) {
         try {
            sizePtr.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      sizePtr.close();
      return conns;
   }

   private static List<InternetProtocolStats.IPConnection> queryUDPv4Connections() {
      List<InternetProtocolStats.IPConnection> conns = new ArrayList<>();
      ByRef.CloseableIntByReference sizePtr = new ByRef.CloseableIntByReference();

      try {
         int ret = IPHLP.GetExtendedUdpTable(null, sizePtr, false, 2, 1, 0);
         if (ret == 0) {
            int size = sizePtr.getValue();
            Memory buf = new Memory(size);

            do {
               ret = IPHLP.GetExtendedUdpTable(buf, sizePtr, false, 2, 1, 0);
               if (ret == 122) {
                  size = sizePtr.getValue();
                  buf.close();
                  buf = new Memory(size);
               }
            } while (ret == 122);

            IPHlpAPI.MIB_UDPTABLE_OWNER_PID udpTable = new IPHlpAPI.MIB_UDPTABLE_OWNER_PID(buf);

            for (int i = 0; i < udpTable.dwNumEntries; i++) {
               IPHlpAPI.MIB_UDPROW_OWNER_PID row = udpTable.table[i];
               conns.add(
                  new InternetProtocolStats.IPConnection(
                     "udp4",
                     ParseUtil.parseIntToIP(row.dwLocalAddr),
                     ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort),
                     new byte[0],
                     0,
                     InternetProtocolStats.TcpState.NONE,
                     0,
                     0,
                     row.dwOwningPid
                  )
               );
            }

            buf.close();
         }
      } catch (Throwable var9) {
         try {
            sizePtr.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      sizePtr.close();
      return conns;
   }

   private static List<InternetProtocolStats.IPConnection> queryUDPv6Connections() {
      List<InternetProtocolStats.IPConnection> conns = new ArrayList<>();
      ByRef.CloseableIntByReference sizePtr = new ByRef.CloseableIntByReference();

      try {
         int ret = IPHLP.GetExtendedUdpTable(null, sizePtr, false, 23, 1, 0);
         if (ret == 0) {
            int size = sizePtr.getValue();
            Memory buf = new Memory(size);

            do {
               ret = IPHLP.GetExtendedUdpTable(buf, sizePtr, false, 23, 1, 0);
               if (ret == 122) {
                  size = sizePtr.getValue();
                  buf.close();
                  buf = new Memory(size);
               }
            } while (ret == 122);

            IPHlpAPI.MIB_UDP6TABLE_OWNER_PID udpTable = new IPHlpAPI.MIB_UDP6TABLE_OWNER_PID(buf);

            for (int i = 0; i < udpTable.dwNumEntries; i++) {
               IPHlpAPI.MIB_UDP6ROW_OWNER_PID row = udpTable.table[i];
               conns.add(
                  new InternetProtocolStats.IPConnection(
                     "udp6",
                     row.ucLocalAddr,
                     ParseUtil.bigEndian16ToLittleEndian(row.dwLocalPort),
                     new byte[0],
                     0,
                     InternetProtocolStats.TcpState.NONE,
                     0,
                     0,
                     row.dwOwningPid
                  )
               );
            }
         }
      } catch (Throwable var9) {
         try {
            sizePtr.close();
         } catch (Throwable var8) {
            var9.addSuppressed(var8);
         }

         throw var9;
      }

      sizePtr.close();
      return conns;
   }

   private static InternetProtocolStats.TcpState stateLookup(int state) {
      switch (state) {
         case 1:
         case 12:
            return InternetProtocolStats.TcpState.CLOSED;
         case 2:
            return InternetProtocolStats.TcpState.LISTEN;
         case 3:
            return InternetProtocolStats.TcpState.SYN_SENT;
         case 4:
            return InternetProtocolStats.TcpState.SYN_RECV;
         case 5:
            return InternetProtocolStats.TcpState.ESTABLISHED;
         case 6:
            return InternetProtocolStats.TcpState.FIN_WAIT_1;
         case 7:
            return InternetProtocolStats.TcpState.FIN_WAIT_2;
         case 8:
            return InternetProtocolStats.TcpState.CLOSE_WAIT;
         case 9:
            return InternetProtocolStats.TcpState.CLOSING;
         case 10:
            return InternetProtocolStats.TcpState.LAST_ACK;
         case 11:
            return InternetProtocolStats.TcpState.TIME_WAIT;
         default:
            return InternetProtocolStats.TcpState.UNKNOWN;
      }
   }
}
