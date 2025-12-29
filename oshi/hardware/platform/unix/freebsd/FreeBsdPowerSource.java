package oshi.hardware.platform.unix.freebsd;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

@ThreadSafe
public final class FreeBsdPowerSource extends AbstractPowerSource {
   public FreeBsdPowerSource(
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
      return Arrays.asList(getPowerSource("BAT0"));
   }

   private static FreeBsdPowerSource getPowerSource(String name) {
      double psRemainingCapacityPercent = 1.0;
      double psTimeRemainingEstimated = -1.0;
      double psPowerUsageRate = 0.0;
      int psVoltage = -1;
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
      int state = BsdSysctlUtil.sysctl("hw.acpi.battery.state", 0);
      if (state == 2) {
         psCharging = true;
      } else {
         int time = BsdSysctlUtil.sysctl("hw.acpi.battery.time", -1);
         psTimeRemainingEstimated = time < 0 ? -1.0 : 60.0 * time;
         if (state == 1) {
            psDischarging = true;
         }
      }

      int life = BsdSysctlUtil.sysctl("hw.acpi.battery.life", -1);
      if (life > 0) {
         psRemainingCapacityPercent = life / 100.0;
      }

      List<String> acpiconf = ExecutingCommand.runNative("acpiconf -i 0");
      Map<String, String> psMap = new HashMap<>();

      for (String line : acpiconf) {
         String[] split = line.split(":", 2);
         if (split.length > 1) {
            String value = split[1].trim();
            if (!value.isEmpty()) {
               psMap.put(split[0], value);
            }
         }
      }

      String psDeviceName = psMap.getOrDefault("Model number", "unknown");
      String psSerialNumber = psMap.getOrDefault("Serial number", "unknown");
      String psChemistry = psMap.getOrDefault("Type", "unknown");
      String psManufacturer = psMap.getOrDefault("OEM info", "unknown");
      String cap = psMap.get("Design capacity");
      if (cap != null) {
         psDesignCapacity = ParseUtil.getFirstIntValue(cap);
         if (cap.toLowerCase().contains("mah")) {
            psCapacityUnits = PowerSource.CapacityUnits.MAH;
         } else if (cap.toLowerCase().contains("mwh")) {
            psCapacityUnits = PowerSource.CapacityUnits.MWH;
         }
      }

      cap = psMap.get("Last full capacity");
      if (cap != null) {
         psMaxCapacity = ParseUtil.getFirstIntValue(cap);
      } else {
         psMaxCapacity = psDesignCapacity;
      }

      double psTimeRemainingInstant = psTimeRemainingEstimated;
      String timex = psMap.get("Remaining time");
      if (timex != null) {
         String[] hhmm = timex.split(":");
         if (hhmm.length == 2) {
            psTimeRemainingInstant = 3600.0 * ParseUtil.parseIntOrDefault(hhmm[0], 0) + 60.0 * ParseUtil.parseIntOrDefault(hhmm[1], 0);
         }
      }

      String rate = psMap.get("Present rate");
      if (rate != null) {
         psPowerUsageRate = ParseUtil.getFirstIntValue(rate);
      }

      String volts = psMap.get("Present voltage");
      if (volts != null) {
         psVoltage = ParseUtil.getFirstIntValue(volts);
         if (psVoltage != 0) {
            psAmperage = psPowerUsageRate / psVoltage;
         }
      }

      return new FreeBsdPowerSource(
         name,
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
}
