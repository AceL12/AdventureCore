package oshi.driver.windows.perfmon;

import com.sun.jna.platform.win32.VersionHelpers;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class ProcessorInformation {
   private static final boolean IS_WIN7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

   private ProcessorInformation() {
   }

   public static Pair<List<String>, Map<ProcessorInformation.ProcessorTickCountProperty, List<Long>>> queryProcessorCounters() {
      if (PerfmonDisabled.PERF_OS_DISABLED) {
         return new Pair<>(Collections.emptyList(), Collections.emptyMap());
      } else {
         return IS_WIN7_OR_GREATER
            ? PerfCounterWildcardQuery.queryInstancesAndValues(
               ProcessorInformation.ProcessorTickCountProperty.class,
               "Processor Information",
               "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\""
            )
            : PerfCounterWildcardQuery.queryInstancesAndValues(
               ProcessorInformation.ProcessorTickCountProperty.class, "Processor", "Win32_PerfRawData_PerfOS_Processor WHERE Name!=\"_Total\""
            );
      }
   }

   public static Pair<List<String>, Map<ProcessorInformation.ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
      return PerfmonDisabled.PERF_OS_DISABLED
         ? new Pair<>(Collections.emptyList(), Collections.emptyMap())
         : PerfCounterWildcardQuery.queryInstancesAndValues(
            ProcessorInformation.ProcessorUtilityTickCountProperty.class,
            "Processor Information",
            "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\""
         );
   }

   public static Map<ProcessorInformation.InterruptsProperty, Long> queryInterruptCounters() {
      return PerfmonDisabled.PERF_OS_DISABLED
         ? Collections.emptyMap()
         : PerfCounterQuery.queryValues(ProcessorInformation.InterruptsProperty.class, "Processor", "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"");
   }

   public static Pair<List<String>, Map<ProcessorInformation.ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
      return PerfmonDisabled.PERF_OS_DISABLED
         ? new Pair<>(Collections.emptyList(), Collections.emptyMap())
         : PerfCounterWildcardQuery.queryInstancesAndValues(
            ProcessorInformation.ProcessorFrequencyProperty.class,
            "Processor Information",
            "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\""
         );
   }

   public static enum InterruptsProperty implements PerfCounterQuery.PdhCounterProperty {
      INTERRUPTSPERSEC("_Total", "Interrupts/sec");

      private final String instance;
      private final String counter;

      private InterruptsProperty(String instance, String counter) {
         this.instance = instance;
         this.counter = counter;
      }

      @Override
      public String getInstance() {
         return this.instance;
      }

      @Override
      public String getCounter() {
         return this.counter;
      }
   }

   public static enum ProcessorFrequencyProperty implements PerfCounterWildcardQuery.PdhCounterWildcardProperty {
      NAME("^*_Total"),
      PERCENTOFMAXIMUMFREQUENCY("% of Maximum Frequency");

      private final String counter;

      private ProcessorFrequencyProperty(String counter) {
         this.counter = counter;
      }

      @Override
      public String getCounter() {
         return this.counter;
      }
   }

   public static enum ProcessorTickCountProperty implements PerfCounterWildcardQuery.PdhCounterWildcardProperty {
      NAME("^*_Total"),
      PERCENTDPCTIME("% DPC Time"),
      PERCENTINTERRUPTTIME("% Interrupt Time"),
      PERCENTPRIVILEGEDTIME("% Privileged Time"),
      PERCENTPROCESSORTIME("% Processor Time"),
      PERCENTUSERTIME("% User Time");

      private final String counter;

      private ProcessorTickCountProperty(String counter) {
         this.counter = counter;
      }

      @Override
      public String getCounter() {
         return this.counter;
      }
   }

   public static enum ProcessorUtilityTickCountProperty implements PerfCounterWildcardQuery.PdhCounterWildcardProperty {
      NAME("^*_Total"),
      PERCENTDPCTIME("% DPC Time"),
      PERCENTINTERRUPTTIME("% Interrupt Time"),
      PERCENTPRIVILEGEDTIME("% Privileged Time"),
      PERCENTPROCESSORTIME("% Processor Time"),
      TIMESTAMP_SYS100NS("% Processor Time_Base"),
      PERCENTPRIVILEGEDUTILITY("% Privileged Utility"),
      PERCENTPROCESSORUTILITY("% Processor Utility"),
      PERCENTPROCESSORUTILITY_BASE("% Processor Utility_Base"),
      PERCENTUSERTIME("% User Time");

      private final String counter;

      private ProcessorUtilityTickCountProperty(String counter) {
         this.counter = counter;
      }

      @Override
      public String getCounter() {
         return this.counter;
      }
   }
}
