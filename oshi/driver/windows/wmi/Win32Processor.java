package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Objects;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32Processor {
   private static final String WIN32_PROCESSOR = "Win32_Processor";

   private Win32Processor() {
   }

   public static WbemcliUtil.WmiResult<Win32Processor.VoltProperty> queryVoltage() {
      WbemcliUtil.WmiQuery<Win32Processor.VoltProperty> voltQuery = new WbemcliUtil.WmiQuery<>("Win32_Processor", Win32Processor.VoltProperty.class);
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(voltQuery);
   }

   public static WbemcliUtil.WmiResult<Win32Processor.ProcessorIdProperty> queryProcessorId() {
      WbemcliUtil.WmiQuery<Win32Processor.ProcessorIdProperty> idQuery = new WbemcliUtil.WmiQuery<>("Win32_Processor", Win32Processor.ProcessorIdProperty.class);
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(idQuery);
   }

   public static WbemcliUtil.WmiResult<Win32Processor.BitnessProperty> queryBitness() {
      WbemcliUtil.WmiQuery<Win32Processor.BitnessProperty> bitnessQuery = new WbemcliUtil.WmiQuery<>("Win32_Processor", Win32Processor.BitnessProperty.class);
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(bitnessQuery);
   }

   public static enum BitnessProperty {
      ADDRESSWIDTH;
   }

   public static enum ProcessorIdProperty {
      PROCESSORID;
   }

   public static enum VoltProperty {
      CURRENTVOLTAGE,
      VOLTAGECAPS;
   }
}
