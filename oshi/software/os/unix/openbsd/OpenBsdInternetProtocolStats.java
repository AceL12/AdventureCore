package oshi.software.os.unix.openbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStat;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.software.os.InternetProtocolStats;

@ThreadSafe
public class OpenBsdInternetProtocolStats extends AbstractInternetProtocolStats {
   @Override
   public InternetProtocolStats.TcpStats getTCPv4Stats() {
      return NetStat.queryTcpStats("netstat -s -p tcp");
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv4Stats() {
      return NetStat.queryUdpStats("netstat -s -p udp");
   }
}
