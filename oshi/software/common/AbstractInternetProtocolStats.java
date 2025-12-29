package oshi.software.common;

import java.util.List;
import oshi.driver.unix.NetStat;
import oshi.software.os.InternetProtocolStats;

public abstract class AbstractInternetProtocolStats implements InternetProtocolStats {
   @Override
   public InternetProtocolStats.TcpStats getTCPv6Stats() {
      return new InternetProtocolStats.TcpStats(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv6Stats() {
      return new InternetProtocolStats.UdpStats(0L, 0L, 0L, 0L);
   }

   @Override
   public List<InternetProtocolStats.IPConnection> getConnections() {
      return NetStat.queryNetstat();
   }
}
