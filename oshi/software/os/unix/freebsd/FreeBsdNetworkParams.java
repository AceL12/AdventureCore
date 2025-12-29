package oshi.software.os.unix.freebsd;

import com.sun.jna.Native;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.platform.unix.CLibrary;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

@ThreadSafe
final class FreeBsdNetworkParams extends AbstractNetworkParams {
   private static final Logger LOG = LoggerFactory.getLogger(FreeBsdNetworkParams.class);
   private static final FreeBsdLibc LIBC = FreeBsdLibc.INSTANCE;

   @Override
   public String getDomainName() {
      CLibrary.Addrinfo hint = new CLibrary.Addrinfo();

      String info;
      label52: {
         String var7;
         try {
            ByRef.CloseablePointerByReference ptr;
            label56: {
               hint.ai_flags = 2;
               String hostname = this.getHostName();
               ptr = new ByRef.CloseablePointerByReference();

               try {
                  int res = LIBC.getaddrinfo(hostname, null, hint, ptr);
                  if (res <= 0) {
                     CLibrary.Addrinfo infox = new CLibrary.Addrinfo(ptr.getValue());
                     String canonname = infox.ai_canonname.trim();
                     LIBC.freeaddrinfo(ptr.getValue());
                     var7 = canonname;
                     break label56;
                  }

                  if (LOG.isErrorEnabled()) {
                     LOG.warn("Failed getaddrinfo(): {}", LIBC.gai_strerror(res));
                  }

                  info = "";
               } catch (Throwable var10) {
                  try {
                     ptr.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }

                  throw var10;
               }

               ptr.close();
               break label52;
            }

            ptr.close();
         } catch (Throwable var11) {
            try {
               hint.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
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
      return 0 != LIBC.gethostname(hostnameBuffer, hostnameBuffer.length) ? super.getHostName() : Native.toString(hostnameBuffer);
   }

   @Override
   public String getIpv4DefaultGateway() {
      return searchGateway(ExecutingCommand.runNative("route -4 get default"));
   }

   @Override
   public String getIpv6DefaultGateway() {
      return searchGateway(ExecutingCommand.runNative("route -6 get default"));
   }
}
