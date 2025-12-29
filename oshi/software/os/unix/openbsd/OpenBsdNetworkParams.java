package oshi.software.os.unix.openbsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractNetworkParams;
import oshi.util.ExecutingCommand;

@ThreadSafe
public class OpenBsdNetworkParams extends AbstractNetworkParams {
   @Override
   public String getIpv4DefaultGateway() {
      return searchGateway(ExecutingCommand.runNative("route -n get default"));
   }

   @Override
   public String getIpv6DefaultGateway() {
      return searchGateway(ExecutingCommand.runNative("route -n get default"));
   }
}
