package oshi.software.os.mac;

import com.sun.jna.Native;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.platform.mac.SystemB;
import oshi.jna.platform.unix.CLibrary;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
final class MacNetworkParams extends AbstractNetworkParams {
   private static final Logger LOG = LoggerFactory.getLogger(MacNetworkParams.class);
   private static final SystemB SYS = SystemB.INSTANCE;
   private static final String IPV6_ROUTE_HEADER = "Internet6:";
   private static final String DEFAULT_GATEWAY = "default";

   @Override
   public String getDomainName() {
      String hostname = "";

      try {
         hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException var10) {
         LOG.error("Unknown host exception when getting address of local host: {}", var10.getMessage());
         return "";
      }

      CLibrary.Addrinfo hint = new CLibrary.Addrinfo();

      String info;
      label60: {
         String var7;
         try {
            ByRef.CloseablePointerByReference ptr;
            label64: {
               ptr = new ByRef.CloseablePointerByReference();

               try {
                  hint.ai_flags = 2;
                  int res = SYS.getaddrinfo(hostname, null, hint, ptr);
                  if (res <= 0) {
                     CLibrary.Addrinfo infox = new CLibrary.Addrinfo(ptr.getValue());
                     String canonname = infox.ai_canonname.trim();
                     SYS.freeaddrinfo(ptr.getValue());
                     var7 = canonname;
                     break label64;
                  }

                  if (LOG.isErrorEnabled()) {
                     LOG.error("Failed getaddrinfo(): {}", SYS.gai_strerror(res));
                  }

                  info = "";
               } catch (Throwable var11) {
                  try {
                     ptr.close();
                  } catch (Throwable var9) {
                     var11.addSuppressed(var9);
                  }

                  throw var11;
               }

               ptr.close();
               break label60;
            }

            ptr.close();
         } catch (Throwable var12) {
            try {
               hint.close();
            } catch (Throwable var8) {
               var12.addSuppressed(var8);
            }

            throw var12;
         }

         hint.close();
         return var7;
      }

      hint.close();
      return info;
   }

   @Override
   public String getHostName() {
      byte[] hostnameBuffer = new byte[256];
      return 0 != SYS.gethostname(hostnameBuffer, hostnameBuffer.length) ? super.getHostName() : Native.toString(hostnameBuffer);
   }

   @Override
   public String getIpv4DefaultGateway() {
      return searchGateway(ExecutingCommand.runNative("route -n get default"));
   }

   @Override
   public String getIpv6DefaultGateway() {
      List<String> lines = ExecutingCommand.runNative("netstat -nr");
      boolean v6Table = false;

      for (String line : lines) {
         if (v6Table && line.startsWith("default")) {
            String[] fields = ParseUtil.whitespaces.split(line);
            if (fields.length > 2 && fields[2].contains("G")) {
               return fields[1].split("%")[0];
            }
         } else if (line.startsWith("Internet6:")) {
            v6Table = true;
         }
      }

      return "";
   }
}
