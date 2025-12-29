package oshi.hardware.platform.unix.openbsd;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class OpenBsdPowerSource extends AbstractPowerSource {
   public OpenBsdPowerSource(
      String psName,
      String psDeviceName,
      double psRemainingCapacityPercent,
      double psTimeRemainingEstimated,
      double psTimeRemainingInstant,
      double psPowerUsageRate,
      double psVoltage,
      double psAmperage,
      boolean psPowerOnLine,
      boolean psCharging,
      boolean psDischarging,
      PowerSource.CapacityUnits psCapacityUnits,
      int psCurrentCapacity,
      int psMaxCapacity,
      int psDesignCapacity,
      int psCycleCount,
      String psChemistry,
      LocalDate psManufactureDate,
      String psManufacturer,
      String psSerialNumber,
      double psTemperature
   ) {
      super(
         psName,
         psDeviceName,
         psRemainingCapacityPercent,
         psTimeRemainingEstimated,
         psTimeRemainingInstant,
         psPowerUsageRate,
         psVoltage,
         psAmperage,
         psPowerOnLine,
         psCharging,
         psDischarging,
         psCapacityUnits,
         psCurrentCapacity,
         psMaxCapacity,
         psDesignCapacity,
         psCycleCount,
         psChemistry,
         psManufactureDate,
         psManufacturer,
         psSerialNumber,
         psTemperature
      );
   }

   public static List<PowerSource> getPowerSources() {
      Set<String> psNames = new HashSet<>();

      for (String line : ExecutingCommand.runNative("systat -ab sensors")) {
         if (line.contains(".amphour") || line.contains(".watthour")) {
            int dot = line.indexOf(46);
            psNames.add(line.substring(0, dot));
         }
      }

      List<PowerSource> psList = new ArrayList<>();

      for (String name : psNames) {
         psList.add(getPowerSource(name));
      }

      return psList;
   }

   private static OpenBsdPowerSource getPowerSource(String name) {
      String psName = name.startsWith("acpi") ? name.substring(4) : name;
      double psRemainingCapacityPercent = 1.0;
      double psTimeRemainingEstimated = -1.0;
      double psPowerUsageRate = 0.0;
      double psVoltage = -1.0;
      double psAmperage = 0.0;
      boolean psPowerOnLine = false;
      boolean psCharging = false;
      boolean psDischarging = false;
      PowerSource.CapacityUnits psCapacityUnits = PowerSource.CapacityUnits.RELATIVE;
      int psCurrentCapacity = 0;
      int psMaxCapacity = 1;
      int psDesignCapacity = 1;
      int psCycleCount = -1;
      LocalDate psManufactureDate = null;
      double psTemperature = 0.0;

      for (String line : ExecutingCommand.runNative("systat -ab sensors")) {
         String[] split = ParseUtil.whitespaces.split(line);
         if (split.length > 1 && split[0].startsWith(name)) {
            if (!split[0].contains("volt0") && (!split[0].contains("volt") || !line.contains("current"))) {
               if (split[0].contains("current0")) {
                  psAmperage = ParseUtil.parseDoubleOrDefault(split[1], 0.0);
               } else if (split[0].contains("temp0")) {
                  psTemperature = ParseUtil.parseDoubleOrDefault(split[1], 0.0);
               } else if (split[0].contains("watthour") || split[0].contains("amphour")) {
                  psCapacityUnits = split[0].contains("watthour") ? PowerSource.CapacityUnits.MWH : PowerSource.CapacityUnits.MAH;
                  if (line.contains("remaining")) {
                     psCurrentCapacity = (int)(1000.0 * ParseUtil.parseDoubleOrDefault(split[1], 0.0));
                  } else if (line.contains("full")) {
                     psMaxCapacity = (int)(1000.0 * ParseUtil.parseDoubleOrDefault(split[1], 0.0));
                  } else if (line.contains("new") || line.contains("design")) {
                     psDesignCapacity = (int)(1000.0 * ParseUtil.parseDoubleOrDefault(split[1], 0.0));
                  }
               }
            } else {
               psVoltage = ParseUtil.parseDoubleOrDefault(split[1], -1.0);
            }
         }
      }

      int state = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("apm -b"), 255);
      if (state < 4) {
         psPowerOnLine = true;
         if (state == 3) {
            psCharging = true;
         } else {
            int time = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("apm -m"), -1);
            psTimeRemainingEstimated = time < 0 ? -1.0 : 60.0 * time;
            psDischarging = true;
         }
      }

      int life = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("apm -l"), -1);
      if (life > 0) {
         psRemainingCapacityPercent = life / 100.0;
      }

      if (psMaxCapacity < psDesignCapacity && psMaxCapacity < psCurrentCapacity) {
         psMaxCapacity = psDesignCapacity;
      } else if (psDesignCapacity < psMaxCapacity && psDesignCapacity < psCurrentCapacity) {
         psDesignCapacity = psMaxCapacity;
      }

      String psDeviceName = "unknown";
      String psSerialNumber = "unknown";
      String psChemistry = "unknown";
      String psManufacturer = "unknown";
      if (psVoltage > 0.0) {
         if (psAmperage > 0.0 && psPowerUsageRate == 0.0) {
            psPowerUsageRate = psAmperage * psVoltage;
         } else if (psAmperage == 0.0 && psPowerUsageRate > 0.0) {
            psAmperage = psPowerUsageRate / psVoltage;
         }
      }

      return new OpenBsdPowerSource(
         psName,
         psDeviceName,
         psRemainingCapacityPercent,
         psTimeRemainingEstimated,
         psTimeRemainingEstimated,
         psPowerUsageRate,
         psVoltage,
         psAmperage,
         psPowerOnLine,
         psCharging,
         psDischarging,
         psCapacityUnits,
         psCurrentCapacity,
         psMaxCapacity,
         psDesignCapacity,
         psCycleCount,
         psChemistry,
         psManufactureDate,
         psManufacturer,
         psSerialNumber,
         psTemperature
      );
   }
}
