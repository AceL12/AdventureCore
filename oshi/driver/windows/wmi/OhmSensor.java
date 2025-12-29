package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class OhmSensor {
   private static final String SENSOR = "Sensor";

   private OhmSensor() {
   }

   public static WbemcliUtil.WmiResult<OhmSensor.ValueProperty> querySensorValue(WmiQueryHandler h, String identifier, String sensorType) {
      StringBuilder sb = new StringBuilder("Sensor");
      sb.append(" WHERE Parent = \"").append(identifier);
      sb.append("\" AND SensorType=\"").append(sensorType).append('"');
      WbemcliUtil.WmiQuery<OhmSensor.ValueProperty> ohmSensorQuery = new WbemcliUtil.WmiQuery<>(
         "ROOT\\OpenHardwareMonitor", sb.toString(), OhmSensor.ValueProperty.class
      );
      return h.queryWMI(ohmSensorQuery, false);
   }

   public static enum ValueProperty {
      VALUE;
   }
}
