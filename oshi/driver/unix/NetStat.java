package oshi.driver.unix;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.InternetProtocolStats;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class NetStat {
   private NetStat() {
   }

   public static Pair<Long, Long> queryTcpnetstat() {
      long tcp4 = 0L;
      long tcp6 = 0L;

      for (String s : ExecutingCommand.runNative("netstat -n -p tcp")) {
         if (s.endsWith("ESTABLISHED")) {
            if (s.startsWith("tcp4")) {
               tcp4++;
            } else if (s.startsWith("tcp6")) {
               tcp6++;
            }
         }
      }

      return new Pair<>(tcp4, tcp6);
   }

   public static List<InternetProtocolStats.IPConnection> queryNetstat() {
      List<InternetProtocolStats.IPConnection> connections = new ArrayList<>();

      for (String s : ExecutingCommand.runNative("netstat -n")) {
         String[] split = null;
         if (s.startsWith("tcp") || s.startsWith("udp")) {
            split = ParseUtil.whitespaces.split(s);
            if (split.length >= 5) {
               String state = split.length == 6 ? split[5] : null;
               if ("SYN_RCVD".equals(state)) {
                  state = "SYN_RECV";
               }

               String type = split[0];
               Pair<byte[], Integer> local = parseIP(split[3]);
               Pair<byte[], Integer> foreign = parseIP(split[4]);
               connections.add(
                  new InternetProtocolStats.IPConnection(
                     type,
                     local.getA(),
                     local.getB(),
                     foreign.getA(),
                     foreign.getB(),
                     state == null ? InternetProtocolStats.TcpState.NONE : InternetProtocolStats.TcpState.valueOf(state),
                     ParseUtil.parseIntOrDefault(split[2], 0),
                     ParseUtil.parseIntOrDefault(split[1], 0),
                     -1
                  )
               );
            }
         }
      }

      return connections;
   }

   private static Pair<byte[], Integer> parseIP(String s) {
      int portPos = s.lastIndexOf(46);
      if (portPos > 0 && s.length() > portPos) {
         int port = ParseUtil.parseIntOrDefault(s.substring(portPos + 1), 0);
         String ip = s.substring(0, portPos);

         try {
            return new Pair<>(InetAddress.getByName(ip).getAddress(), port);
         } catch (UnknownHostException var7) {
            try {
               if (ip.endsWith(":") && ip.contains("::")) {
                  ip = ip + "0";
               } else if (!ip.endsWith(":") && !ip.contains("::")) {
                  ip = ip + "::0";
               } else {
                  ip = ip + ":0";
               }

               return new Pair<>(InetAddress.getByName(ip).getAddress(), port);
            } catch (UnknownHostException var6) {
               return new Pair<>(new byte[0], port);
            }
         }
      } else {
         return new Pair<>(new byte[0], 0);
      }
   }

   public static InternetProtocolStats.TcpStats queryTcpStats(String netstatStr) {
      long connectionsEstablished = 0L;
      long connectionsActive = 0L;
      long connectionsPassive = 0L;
      long connectionFailures = 0L;
      long connectionsReset = 0L;
      long segmentsSent = 0L;
      long segmentsReceived = 0L;
      long segmentsRetransmitted = 0L;
      long inErrors = 0L;
      long outResets = 0L;

      for (String s : ExecutingCommand.runNative(netstatStr)) {
         String[] split = s.trim().split(" ", 2);
         if (split.length == 2) {
            String var25 = split[1];
            switch (var25) {
               case "connections established":
               case "connection established (including accepts)":
               case "connections established (including accepts)":
                  connectionsEstablished = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "active connection openings":
                  connectionsActive = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "passive connection openings":
                  connectionsPassive = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "failed connection attempts":
               case "bad connection attempts":
                  connectionFailures = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "connection resets received":
               case "dropped due to RST":
                  connectionsReset = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "segments sent out":
               case "packet sent":
               case "packets sent":
                  segmentsSent = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "segments received":
               case "packet received":
               case "packets received":
                  segmentsReceived = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "segments retransmitted":
                  segmentsRetransmitted = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "bad segments received":
               case "discarded for bad checksum":
               case "discarded for bad checksums":
               case "discarded for bad header offset field":
               case "discarded for bad header offset fields":
               case "discarded because packet too short":
               case "discarded for missing IPsec protection":
                  inErrors += ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "resets sent":
                  outResets = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               default:
                  if (split[1].contains("retransmitted") && split[1].contains("data packet")) {
                     segmentsRetransmitted += ParseUtil.parseLongOrDefault(split[0], 0L);
                  }
            }
         }
      }

      return new InternetProtocolStats.TcpStats(
         connectionsEstablished,
         connectionsActive,
         connectionsPassive,
         connectionFailures,
         connectionsReset,
         segmentsSent,
         segmentsReceived,
         segmentsRetransmitted,
         inErrors,
         outResets
      );
   }

   public static InternetProtocolStats.UdpStats queryUdpStats(String netstatStr) {
      long datagramsSent = 0L;
      long datagramsReceived = 0L;
      long datagramsNoPort = 0L;
      long datagramsReceivedErrors = 0L;

      for (String s : ExecutingCommand.runNative(netstatStr)) {
         String[] split = s.trim().split(" ", 2);
         if (split.length == 2) {
            String var13 = split[1];
            switch (var13) {
               case "packets sent":
               case "datagram output":
               case "datagrams output":
                  datagramsSent = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "packets received":
               case "datagram received":
               case "datagrams received":
                  datagramsReceived = ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "packets to unknown port received":
               case "dropped due to no socket":
               case "broadcast/multicast datagram dropped due to no socket":
               case "broadcast/multicast datagrams dropped due to no socket":
                  datagramsNoPort += ParseUtil.parseLongOrDefault(split[0], 0L);
                  break;
               case "packet receive errors":
               case "with incomplete header":
               case "with bad data length field":
               case "with bad checksum":
               case "woth no checksum":
                  datagramsReceivedErrors += ParseUtil.parseLongOrDefault(split[0], 0L);
            }
         }
      }

      return new InternetProtocolStats.UdpStats(datagramsSent, datagramsReceived, datagramsNoPort, datagramsReceivedErrors);
   }
}
