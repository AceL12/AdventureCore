package oshi.hardware.platform.unix.freebsd;

import com.sun.jna.Memory;
import com.sun.jna.platform.unix.LibCAPI;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.jna.ByRef;
import oshi.jna.platform.unix.FreeBsdLibc;

@ThreadSafe
final class FreeBsdSensors extends AbstractSensors {
   @Override
   public double queryCpuTemperature() {
      return queryKldloadCoretemp();
   }

   private static double queryKldloadCoretemp() {
      String name = "dev.cpu.%d.temperature";
      ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference(FreeBsdLibc.INT_SIZE);

      double var12;
      try {
         int cpu = 0;
         double sumTemp = 0.0;
         Memory p = new Memory(size.longValue());

         try {
            while (0 == FreeBsdLibc.INSTANCE.sysctlbyname(String.format(name, cpu), p, size, null, LibCAPI.size_t.ZERO)) {
               sumTemp += p.getInt(0L) / 10.0 - 273.15;
               cpu++;
            }
         } catch (Throwable var10) {
            try {
               p.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         p.close();
         var12 = cpu > 0 ? sumTemp / cpu : Double.NaN;
      } catch (Throwable var11) {
         try {
            size.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      size.close();
      return var12;
   }

   @Override
   public int[] queryFanSpeeds() {
      return new int[0];
   }

   @Override
   public double queryCpuVoltage() {
      return 0.0;
   }
}
