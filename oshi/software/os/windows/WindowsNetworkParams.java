package oshi.software.os.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
final class WindowsNetworkParams extends AbstractNetworkParams {
   private static final Logger LOG = LoggerFactory.getLogger(WindowsNetworkParams.class);
   private static final int COMPUTER_NAME_DNS_DOMAIN_FULLY_QUALIFIED = 3;

   @Override
   public String getDomainName() {
      char[] buffer = new char[256];
      ByRef.CloseableIntByReference bufferSize = new ByRef.CloseableIntByReference(buffer.length);

      String var3;
      label27: {
         try {
            if (!Kernel32.INSTANCE.GetComputerNameEx(3, buffer, bufferSize)) {
               LOG.error("Failed to get dns domain name. Error code: {}", Kernel32.INSTANCE.GetLastError());
               var3 = "";
               break label27;
            }
         } catch (Throwable var6) {
            try {
               bufferSize.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         bufferSize.close();
         return Native.toString(buffer);
      }

      bufferSize.close();
      return var3;
   }

   @Override
   public String[] getDnsServers() {
      ByRef.CloseableIntByReference bufferSize = new ByRef.CloseableIntByReference();

      String[] var14;
      label73: {
         String[] fixedInfo;
         label72: {
            String[] var16;
            try {
               int ret = IPHlpAPI.INSTANCE.GetNetworkParams(null, bufferSize);
               if (ret != 111) {
                  LOG.error("Failed to get network parameters buffer size. Error code: {}", ret);
                  var14 = new String[0];
                  break label73;
               }

               Memory buffer = new Memory(bufferSize.getValue());

               label68: {
                  try {
                     ret = IPHlpAPI.INSTANCE.GetNetworkParams(buffer, bufferSize);
                     if (ret == 0) {
                        IPHlpAPI.FIXED_INFO fixedInfox = new IPHlpAPI.FIXED_INFO(buffer);
                        List<String> list = new ArrayList<>();

                        for (IPHlpAPI.IP_ADDR_STRING dns = fixedInfox.DnsServerList; dns != null; dns = dns.Next) {
                           String addr = Native.toString(dns.IpAddress.String, StandardCharsets.US_ASCII);
                           int nullPos = addr.indexOf(0);
                           if (nullPos != -1) {
                              addr = addr.substring(0, nullPos);
                           }

                           list.add(addr);
                        }

                        var16 = list.toArray(new String[0]);
                        break label68;
                     }

                     LOG.error("Failed to get network parameters. Error code: {}", ret);
                     fixedInfo = new String[0];
                  } catch (Throwable var11) {
                     try {
                        buffer.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }

                     throw var11;
                  }

                  buffer.close();
                  break label72;
               }

               buffer.close();
            } catch (Throwable var12) {
               try {
                  bufferSize.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }

               throw var12;
            }

            bufferSize.close();
            return var16;
         }

         bufferSize.close();
         return fixedInfo;
      }

      bufferSize.close();
      return var14;
   }

   @Override
   public String getHostName() {
      return Kernel32Util.getComputerName();
   }

   @Override
   public String getIpv4DefaultGateway() {
      return parseIpv4Route();
   }

   @Override
   public String getIpv6DefaultGateway() {
      return parseIpv6Route();
   }

   private static String parseIpv4Route() {
      for (String line : ExecutingCommand.runNative("route print -4 0.0.0.0")) {
         String[] fields = ParseUtil.whitespaces.split(line.trim());
         if (fields.length > 2 && "0.0.0.0".equals(fields[0])) {
            return fields[2];
         }
      }

      return "";
   }

   private static String parseIpv6Route() {
      for (String line : ExecutingCommand.runNative("route print -6 ::/0")) {
         String[] fields = ParseUtil.whitespaces.split(line.trim());
         if (fields.length > 3 && "::/0".equals(fields[2])) {
            return fields[3];
         }
      }

      return "";
   }
}
