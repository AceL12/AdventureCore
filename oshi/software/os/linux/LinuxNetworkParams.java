package oshi.software.os.linux;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.jna.platform.unix.CLibrary;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
final class LinuxNetworkParams extends AbstractNetworkParams {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxNetworkParams.class);
   private static final LinuxLibc LIBC = LinuxLibc.INSTANCE;
   private static final String IPV4_DEFAULT_DEST = "0.0.0.0";
   private static final String IPV6_DEFAULT_DEST = "::/0";

   @Override
   public String getDomainName() {
      CLibrary.Addrinfo hint = new CLibrary.Addrinfo();

      String res;
      label85: {
         String info;
         label84: {
            String var6;
            try {
               ByRef.CloseablePointerByReference ptr;
               label89: {
                  hint.ai_flags = 2;
                  String hostname = "";

                  try {
                     hostname = InetAddress.getLocalHost().getHostName();
                  } catch (UnknownHostException var12) {
                     LOG.warn("Unknown host exception when getting address of local host: {}", var12.getMessage());
                     res = "";
                     break label85;
                  }

                  ptr = new ByRef.CloseablePointerByReference();

                  try {
                     int resx = LIBC.getaddrinfo(hostname, null, hint, ptr);
                     if (resx <= 0) {
                        CLibrary.Addrinfo infox = new CLibrary.Addrinfo(ptr.getValue());

                        try {
                           var6 = infox.ai_canonname == null ? hostname : infox.ai_canonname.trim();
                        } catch (Throwable var11) {
                           try {
                              infox.close();
                           } catch (Throwable var10) {
                              var11.addSuppressed(var10);
                           }

                           throw var11;
                        }

                        infox.close();
                        break label89;
                     }

                     if (LOG.isErrorEnabled()) {
                        LOG.error("Failed getaddrinfo(): {}", LIBC.gai_strerror(resx));
                     }

                     info = "";
                  } catch (Throwable var13) {
                     try {
                        ptr.close();
                     } catch (Throwable var9) {
                        var13.addSuppressed(var9);
                     }

                     throw var13;
                  }

                  ptr.close();
                  break label84;
               }

               ptr.close();
            } catch (Throwable var14) {
               try {
                  hint.close();
               } catch (Throwable var8) {
                  var14.addSuppressed(var8);
               }

               throw var14;
            }

            hint.close();
            return var6;
         }

         hint.close();
         return info;
      }

      hint.close();
      return res;
   }

   @Override
   public String getHostName() {
      byte[] hostnameBuffer = new byte[256];
      return 0 != LibC.INSTANCE.gethostname(hostnameBuffer, hostnameBuffer.length) ? super.getHostName() : Native.toString(hostnameBuffer);
   }

   @Override
   public String getIpv4DefaultGateway() {
      List<String> routes = ExecutingCommand.runNative("route -A inet -n");
      if (routes.size() <= 2) {
         return "";
      } else {
         String gateway = "";
         int minMetric = Integer.MAX_VALUE;

         for (int i = 2; i < routes.size(); i++) {
            String[] fields = ParseUtil.whitespaces.split(routes.get(i));
            if (fields.length > 4 && fields[0].equals("0.0.0.0")) {
               boolean isGateway = fields[3].indexOf(71) != -1;
               int metric = ParseUtil.parseIntOrDefault(fields[4], Integer.MAX_VALUE);
               if (isGateway && metric < minMetric) {
                  minMetric = metric;
                  gateway = fields[1];
               }
            }
         }

         return gateway;
      }
   }

   @Override
   public String getIpv6DefaultGateway() {
      List<String> routes = ExecutingCommand.runNative("route -A inet6 -n");
      if (routes.size() <= 2) {
         return "";
      } else {
         String gateway = "";
         int minMetric = Integer.MAX_VALUE;

         for (int i = 2; i < routes.size(); i++) {
            String[] fields = ParseUtil.whitespaces.split(routes.get(i));
            if (fields.length > 3 && fields[0].equals("::/0")) {
               boolean isGateway = fields[2].indexOf(71) != -1;
               int metric = ParseUtil.parseIntOrDefault(fields[3], Integer.MAX_VALUE);
               if (isGateway && metric < minMetric) {
                  minMetric = metric;
                  gateway = fields[1];
               }
            }
         }

         return gateway;
      }
   }
}
