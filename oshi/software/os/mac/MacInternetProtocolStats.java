package oshi.software.os.mac;

import com.sun.jna.Memory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.NetStat;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.unix.CLibrary;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.software.os.InternetProtocolStats;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class MacInternetProtocolStats extends AbstractInternetProtocolStats {
   private boolean isElevated;
   private Supplier<Pair<Long, Long>> establishedv4v6 = Memoizer.memoize(NetStat::queryTcpnetstat, Memoizer.defaultExpiration());
   private Supplier<CLibrary.BsdTcpstat> tcpstat = Memoizer.memoize(MacInternetProtocolStats::queryTcpstat, Memoizer.defaultExpiration());
   private Supplier<CLibrary.BsdUdpstat> udpstat = Memoizer.memoize(MacInternetProtocolStats::queryUdpstat, Memoizer.defaultExpiration());
   private Supplier<CLibrary.BsdIpstat> ipstat = Memoizer.memoize(MacInternetProtocolStats::queryIpstat, Memoizer.defaultExpiration());
   private Supplier<CLibrary.BsdIp6stat> ip6stat = Memoizer.memoize(MacInternetProtocolStats::queryIp6stat, Memoizer.defaultExpiration());

   public MacInternetProtocolStats(boolean elevated) {
      this.isElevated = elevated;
   }

   @Override
   public InternetProtocolStats.TcpStats getTCPv4Stats() {
      CLibrary.BsdTcpstat tcp = this.tcpstat.get();
      if (this.isElevated) {
         return new InternetProtocolStats.TcpStats(
            this.establishedv4v6.get().getA(),
            ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
            ParseUtil.unsignedIntToLong(tcp.tcps_accepts),
            ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
            ParseUtil.unsignedIntToLong(tcp.tcps_drops),
            ParseUtil.unsignedIntToLong(tcp.tcps_sndpack),
            ParseUtil.unsignedIntToLong(tcp.tcps_rcvpack),
            ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
            ParseUtil.unsignedIntToLong(tcp.tcps_rcvbadsum + tcp.tcps_rcvbadoff + tcp.tcps_rcvmemdrop + tcp.tcps_rcvshort),
            0L
         );
      } else {
         CLibrary.BsdIpstat ip = this.ipstat.get();
         CLibrary.BsdUdpstat udp = this.udpstat.get();
         return new InternetProtocolStats.TcpStats(
            this.establishedv4v6.get().getA(),
            ParseUtil.unsignedIntToLong(tcp.tcps_connattempt),
            ParseUtil.unsignedIntToLong(tcp.tcps_accepts),
            ParseUtil.unsignedIntToLong(tcp.tcps_conndrops),
            ParseUtil.unsignedIntToLong(tcp.tcps_drops),
            Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_delivered - udp.udps_opackets)),
            Math.max(0L, ParseUtil.unsignedIntToLong(ip.ips_total - udp.udps_ipackets)),
            ParseUtil.unsignedIntToLong(tcp.tcps_sndrexmitpack),
            Math.max(
               0L,
               ParseUtil.unsignedIntToLong(
                  ip.ips_badsum + ip.ips_tooshort + ip.ips_toosmall + ip.ips_badhlen + ip.ips_badlen - udp.udps_hdrops + udp.udps_badsum + udp.udps_badlen
               )
            ),
            0L
         );
      }
   }

   @Override
   public InternetProtocolStats.TcpStats getTCPv6Stats() {
      CLibrary.BsdIp6stat ip6 = this.ip6stat.get();
      CLibrary.BsdUdpstat udp = this.udpstat.get();
      return new InternetProtocolStats.TcpStats(
         this.establishedv4v6.get().getB(),
         0L,
         0L,
         0L,
         0L,
         ip6.ip6s_localout - ParseUtil.unsignedIntToLong(udp.udps_snd6_swcsum),
         ip6.ip6s_total - ParseUtil.unsignedIntToLong(udp.udps_rcv6_swcsum),
         0L,
         0L,
         0L
      );
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv4Stats() {
      CLibrary.BsdUdpstat stat = this.udpstat.get();
      return new InternetProtocolStats.UdpStats(
         ParseUtil.unsignedIntToLong(stat.udps_opackets),
         ParseUtil.unsignedIntToLong(stat.udps_ipackets),
         ParseUtil.unsignedIntToLong(stat.udps_noportmcast),
         ParseUtil.unsignedIntToLong(stat.udps_hdrops + stat.udps_badsum + stat.udps_badlen)
      );
   }

   @Override
   public InternetProtocolStats.UdpStats getUDPv6Stats() {
      CLibrary.BsdUdpstat stat = this.udpstat.get();
      return new InternetProtocolStats.UdpStats(ParseUtil.unsignedIntToLong(stat.udps_snd6_swcsum), ParseUtil.unsignedIntToLong(stat.udps_rcv6_swcsum), 0L, 0L);
   }

   @Override
   public List<InternetProtocolStats.IPConnection> getConnections() {
      List<InternetProtocolStats.IPConnection> conns = new ArrayList<>();
      int[] pids = new int[1024];
      int numberOfProcesses = SystemB.INSTANCE.proc_listpids(1, 0, pids, pids.length * com.sun.jna.platform.mac.SystemB.INT_SIZE)
         / com.sun.jna.platform.mac.SystemB.INT_SIZE;

      for (int i = 0; i < numberOfProcesses; i++) {
         if (pids[i] > 0) {
            for (Integer fd : queryFdList(pids[i])) {
               InternetProtocolStats.IPConnection ipc = queryIPConnection(pids[i], fd);
               if (ipc != null) {
                  conns.add(ipc);
               }
            }
         }
      }

      return conns;
   }

   private static List<Integer> queryFdList(int pid) {
      List<Integer> fdList = new ArrayList<>();
      int bufferSize = SystemB.INSTANCE.proc_pidinfo(pid, 1, 0L, null, 0);
      if (bufferSize > 0) {
         SystemB.ProcFdInfo fdInfo = new SystemB.ProcFdInfo();
         int numStructs = bufferSize / fdInfo.size();
         SystemB.ProcFdInfo[] fdArray = (SystemB.ProcFdInfo[])fdInfo.toArray(numStructs);
         bufferSize = SystemB.INSTANCE.proc_pidinfo(pid, 1, 0L, fdArray[0], bufferSize);
         numStructs = bufferSize / fdInfo.size();

         for (int i = 0; i < numStructs; i++) {
            if (fdArray[i].proc_fdtype == 2) {
               fdList.add(fdArray[i].proc_fd);
            }
         }
      }

      return fdList;
   }

   private static InternetProtocolStats.IPConnection queryIPConnection(int pid, int fd) {
      SystemB.SocketFdInfo si = new SystemB.SocketFdInfo();

      Object var15;
      label59: {
         Object var16;
         label58: {
            InternetProtocolStats.IPConnection var11;
            label57: {
               try {
                  int ret = SystemB.INSTANCE.proc_pidfdinfo(pid, fd, 3, si, si.size());
                  if (si.size() == ret && si.psi.soi_family == 2 || si.psi.soi_family == 30) {
                     SystemB.InSockInfo ini;
                     String type;
                     InternetProtocolStats.TcpState state;
                     if (si.psi.soi_kind == 2) {
                        si.psi.soi_proto.setType("pri_tcp");
                        si.psi.soi_proto.read();
                        ini = si.psi.soi_proto.pri_tcp.tcpsi_ini;
                        state = stateLookup(si.psi.soi_proto.pri_tcp.tcpsi_state);
                        type = "tcp";
                     } else {
                        if (si.psi.soi_kind != 1) {
                           var15 = null;
                           break label59;
                        }

                        si.psi.soi_proto.setType("pri_in");
                        si.psi.soi_proto.read();
                        ini = si.psi.soi_proto.pri_in;
                        state = InternetProtocolStats.TcpState.NONE;
                        type = "udp";
                     }

                     byte[] laddr;
                     byte[] faddr;
                     if (ini.insi_vflag == 1) {
                        laddr = ParseUtil.parseIntToIP(ini.insi_laddr[3]);
                        faddr = ParseUtil.parseIntToIP(ini.insi_faddr[3]);
                        type = type + "4";
                     } else {
                        if (ini.insi_vflag != 2) {
                           var16 = null;
                           break label58;
                        }

                        laddr = ParseUtil.parseIntArrayToIP(ini.insi_laddr);
                        faddr = ParseUtil.parseIntArrayToIP(ini.insi_faddr);
                        type = type + "6";
                     }

                     int lport = ParseUtil.bigEndian16ToLittleEndian(ini.insi_lport);
                     int fport = ParseUtil.bigEndian16ToLittleEndian(ini.insi_fport);
                     var11 = new InternetProtocolStats.IPConnection(type, laddr, lport, faddr, fport, state, si.psi.soi_qlen, si.psi.soi_incqlen, pid);
                     break label57;
                  }
               } catch (Throwable var13) {
                  try {
                     si.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               si.close();
               return null;
            }

            si.close();
            return var11;
         }

         si.close();
         return (InternetProtocolStats.IPConnection)var16;
      }

      si.close();
      return (InternetProtocolStats.IPConnection)var15;
   }

   private static InternetProtocolStats.TcpState stateLookup(int state) {
      switch (state) {
         case 0:
            return InternetProtocolStats.TcpState.CLOSED;
         case 1:
            return InternetProtocolStats.TcpState.LISTEN;
         case 2:
            return InternetProtocolStats.TcpState.SYN_SENT;
         case 3:
            return InternetProtocolStats.TcpState.SYN_RECV;
         case 4:
            return InternetProtocolStats.TcpState.ESTABLISHED;
         case 5:
            return InternetProtocolStats.TcpState.CLOSE_WAIT;
         case 6:
            return InternetProtocolStats.TcpState.FIN_WAIT_1;
         case 7:
            return InternetProtocolStats.TcpState.CLOSING;
         case 8:
            return InternetProtocolStats.TcpState.LAST_ACK;
         case 9:
            return InternetProtocolStats.TcpState.FIN_WAIT_2;
         case 10:
            return InternetProtocolStats.TcpState.TIME_WAIT;
         default:
            return InternetProtocolStats.TcpState.UNKNOWN;
      }
   }

   private static CLibrary.BsdTcpstat queryTcpstat() {
      CLibrary.BsdTcpstat mt = new CLibrary.BsdTcpstat();
      Memory m = SysctlUtil.sysctl("net.inet.tcp.stats");

      try {
         if (m != null && m.size() >= 128L) {
            mt.tcps_connattempt = m.getInt(0L);
            mt.tcps_accepts = m.getInt(4L);
            mt.tcps_drops = m.getInt(12L);
            mt.tcps_conndrops = m.getInt(16L);
            mt.tcps_sndpack = m.getInt(64L);
            mt.tcps_sndrexmitpack = m.getInt(72L);
            mt.tcps_rcvpack = m.getInt(104L);
            mt.tcps_rcvbadsum = m.getInt(112L);
            mt.tcps_rcvbadoff = m.getInt(116L);
            mt.tcps_rcvmemdrop = m.getInt(120L);
            mt.tcps_rcvshort = m.getInt(124L);
         }
      } catch (Throwable var5) {
         if (m != null) {
            try {
               m.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (m != null) {
         m.close();
      }

      return mt;
   }

   private static CLibrary.BsdIpstat queryIpstat() {
      CLibrary.BsdIpstat mi = new CLibrary.BsdIpstat();
      Memory m = SysctlUtil.sysctl("net.inet.ip.stats");

      try {
         if (m != null && m.size() >= 60L) {
            mi.ips_total = m.getInt(0L);
            mi.ips_badsum = m.getInt(4L);
            mi.ips_tooshort = m.getInt(8L);
            mi.ips_toosmall = m.getInt(12L);
            mi.ips_badhlen = m.getInt(16L);
            mi.ips_badlen = m.getInt(20L);
            mi.ips_delivered = m.getInt(56L);
         }
      } catch (Throwable var5) {
         if (m != null) {
            try {
               m.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (m != null) {
         m.close();
      }

      return mi;
   }

   private static CLibrary.BsdIp6stat queryIp6stat() {
      CLibrary.BsdIp6stat mi6 = new CLibrary.BsdIp6stat();
      Memory m = SysctlUtil.sysctl("net.inet6.ip6.stats");

      try {
         if (m != null && m.size() >= 96L) {
            mi6.ip6s_total = m.getLong(0L);
            mi6.ip6s_localout = m.getLong(88L);
         }
      } catch (Throwable var5) {
         if (m != null) {
            try {
               m.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (m != null) {
         m.close();
      }

      return mi6;
   }

   public static CLibrary.BsdUdpstat queryUdpstat() {
      CLibrary.BsdUdpstat ut = new CLibrary.BsdUdpstat();
      Memory m = SysctlUtil.sysctl("net.inet.udp.stats");

      try {
         if (m != null && m.size() >= 1644L) {
            ut.udps_ipackets = m.getInt(0L);
            ut.udps_hdrops = m.getInt(4L);
            ut.udps_badsum = m.getInt(8L);
            ut.udps_badlen = m.getInt(12L);
            ut.udps_opackets = m.getInt(36L);
            ut.udps_noportmcast = m.getInt(48L);
            ut.udps_rcv6_swcsum = m.getInt(64L);
            ut.udps_snd6_swcsum = m.getInt(80L);
         }
      } catch (Throwable var5) {
         if (m != null) {
            try {
               m.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
      }

      if (m != null) {
         m.close();
      }

      return ut;
   }
}
