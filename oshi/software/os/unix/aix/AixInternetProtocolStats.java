package oshi.software.os.unix.aix;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat;
import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.perfstat.PerfstatProtocol;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.software.os.InternetProtocolStats;
import oshi.util.Memoizer;

@ThreadSafe
public class AixInternetProtocolStats extends AbstractInternetProtocolStats {
   private Supplier<Perfstat.perfstat_protocol_t[]> ipstats = Memoizer.memoize(PerfstatProtocol::queryProtocols, Memoizer.defaultExpiration());

   @Override
   public InternetProtocolStats.TcpStats getTCPv4Stats() {
      for (Perfstat.perfstat_protocol_t stat : this.ipstats.get()) {
         if ("tcp".equals(Native.toString(stat.name))) {
            return new InternetProtocolStats.TcpStats(
               stat.u.tcp.established,
               stat.u.tcp.initiated,
               stat.u.tcp.accepted,
               stat.u.tcp.dropped,
               stat.u.tcp.dropped,
               stat.u.tcp.opackets,
               stat.u.tcp.ipackets,
               0L,
               stat.u.tcp.ierrors,
               0L
            );
         }
      }

      return new InternetProtocolStats.TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv4Stats() {
      for (Perfstat.perfstat_protocol_t stat : this.ipstats.get()) {
         if ("udp".equals(Native.toString(stat.name))) {
            return new InternetProtocolStats.UdpStats(stat.u.udp.opackets, stat.u.udp.ipackets, stat.u.udp.no_socket, stat.u.udp.ierrors);
         }
      }

      return new InternetProtocolStats.UdpStats(0L, 0L, 0L, 0L);
   }
}
