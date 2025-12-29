package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class OhmHardware {
   private static final String HARDWARE = "Hardware";

   private OhmHardware() {
   }

   public static WbemcliUtil.WmiResult<OhmHardware.IdentifierProperty> queryHwIdentifier(WmiQueryHandler h, String typeToQuery, String typeName) {
      StringBuilder sb = new StringBuilder("Hardware");
      sb.append(" WHERE ").append(typeToQuery).append("Type=\"").append(typeName).append('"');
      WbemcliUtil.WmiQuery<OhmHardware.IdentifierProperty> cpuIdentifierQuery = new WbemcliUtil.WmiQuery<>(
         "ROOT\\OpenHardwareMonitor", sb.toString(), OhmHardware.IdentifierProperty.class
      );
      return h.queryWMI(cpuIdentifierQuery, false);
   }

   public static enum IdentifierProperty {
      IDENTIFIER;
   }
}
