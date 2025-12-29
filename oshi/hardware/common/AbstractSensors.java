package oshi.hardware.common;

import java.util.Arrays;
import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Sensors;
import oshi.util.Memoizer;

@ThreadSafe
public abstract class AbstractSensors implements Sensors {
   private final Supplier<Double> cpuTemperature = Memoizer.memoize(this::queryCpuTemperature, Memoizer.defaultExpiration());
   private final Supplier<int[]> fanSpeeds = Memoizer.memoize(this::queryFanSpeeds, Memoizer.defaultExpiration());
   private final Supplier<Double> cpuVoltage = Memoizer.memoize(this::queryCpuVoltage, Memoizer.defaultExpiration());

   @Override
   public double getCpuTemperature() {
      return this.cpuTemperature.get();
   }

   protected abstract double queryCpuTemperature();

   @Override
   public int[] getFanSpeeds() {
      return this.fanSpeeds.get();
   }

   protected abstract int[] queryFanSpeeds();

   @Override
   public double getCpuVoltage() {
      return this.cpuVoltage.get();
   }

   protected abstract double queryCpuVoltage();

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("CPU Temperature=").append(this.getCpuTemperature()).append("°C, ");
      sb.append("Fan Speeds=").append(Arrays.toString(this.getFanSpeeds())).append(", ");
      sb.append("CPU Voltage=").append(this.getCpuVoltage());
      return sb.toString();
   }
}
