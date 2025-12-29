package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
final class SolarisSensors extends AbstractSensors {
   @Override
   public double queryCpuTemperature() {
      double maxTemp = 0.0;

      for (String line : ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c temperature-sensor")) {
         if (line.trim().startsWith("Temperature:")) {
            int temp = ParseUtil.parseLastInt(line, 0);
            if (temp > maxTemp) {
               maxTemp = temp;
            }
         }
      }

      if (maxTemp > 1000.0) {
         maxTemp /= 1000.0;
      }

      return maxTemp;
   }

   @Override
   public int[] queryFanSpeeds() {
      List<Integer> speedList = new ArrayList<>();

      for (String line : ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c fan")) {
         if (line.trim().startsWith("Speed:")) {
            speedList.add(ParseUtil.parseLastInt(line, 0));
         }
      }

      int[] fans = new int[speedList.size()];

      for (int i = 0; i < speedList.size(); i++) {
         fans[i] = speedList.get(i);
      }

      return fans;
   }

   @Override
   public double queryCpuVoltage() {
      double voltage = 0.0;

      for (String line : ExecutingCommand.runNative("/usr/sbin/prtpicl -v -c voltage-sensor")) {
         if (line.trim().startsWith("Voltage:")) {
            voltage = ParseUtil.parseDoubleOrDefault(line.replace("Voltage:", "").trim(), 0.0);
            break;
         }
      }

      return voltage;
   }
}
