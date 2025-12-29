package oshi.software.common;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.NetworkParams;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

@ThreadSafe
public abstract class AbstractNetworkParams implements NetworkParams {
   private static final String NAMESERVER = "nameserver";

   @Override
   public String getDomainName() {
      InetAddress localHost;
      try {
         localHost = InetAddress.getLocalHost();
      } catch (UnknownHostException var3) {
         localHost = InetAddress.getLoopbackAddress();
      }

      return localHost.getCanonicalHostName();
   }

   @Override
   public String getHostName() {
      InetAddress localHost;
      try {
         localHost = InetAddress.getLocalHost();
      } catch (UnknownHostException var4) {
         localHost = InetAddress.getLoopbackAddress();
      }

      String hn = localHost.getHostName();
      int dot = hn.indexOf(46);
      return dot == -1 ? hn : hn.substring(0, dot);
   }

   @Override
   public String[] getDnsServers() {
      List<String> resolv = FileUtil.readFile("/etc/resolv.conf");
      String key = "nameserver";
      int maxNameServer = 3;
      List<String> servers = new ArrayList<>();

      for (int i = 0; i < resolv.size() && servers.size() < maxNameServer; i++) {
         String line = resolv.get(i);
         if (line.startsWith(key)) {
            String value = line.substring(key.length()).replaceFirst("^[ \t]+", "");
            if (value.length() != 0 && value.charAt(0) != '#' && value.charAt(0) != ';') {
               String val = value.split("[ \t#;]", 2)[0];
               servers.add(val);
            }
         }
      }

      return servers.toArray(new String[0]);
   }

   protected static String searchGateway(List<String> lines) {
      for (String line : lines) {
         String leftTrimmed = line.replaceFirst("^\\s+", "");
         if (leftTrimmed.startsWith("gateway:")) {
            String[] split = ParseUtil.whitespaces.split(leftTrimmed);
            if (split.length < 2) {
               return "";
            }

            return split[1].split("%")[0];
         }
      }

      return "";
   }

   @Override
   public String toString() {
      return String.format(
         "Host name: %s, Domain name: %s, DNS servers: %s, IPv4 Gateway: %s, IPv6 Gateway: %s",
         this.getHostName(),
         this.getDomainName(),
         Arrays.toString((Object[])this.getDnsServers()),
         this.getIpv4DefaultGateway(),
         this.getIpv6DefaultGateway()
      );
   }
}
