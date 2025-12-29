package oshi.driver.windows.perfmon;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;
import oshi.util.Util;

@ThreadSafe
public final class PerfmonDisabled {
   private static final Logger LOG = LoggerFactory.getLogger(PerfmonDisabled.class);
   static final boolean PERF_OS_DISABLED = isDisabled("oshi.os.windows.perfos.disabled", "PerfOS");
   static final boolean PERF_PROC_DISABLED = isDisabled("oshi.os.windows.perfproc.disabled", "PerfProc");
   static final boolean PERF_DISK_DISABLED = isDisabled("oshi.os.windows.perfdisk.disabled", "PerfDisk");

   private PerfmonDisabled() {
      throw new AssertionError();
   }

   private static boolean isDisabled(String config, String service) {
      String perfDisabled = GlobalConfig.get(config);
      if (Util.isBlank(perfDisabled)) {
         String key = String.format("SYSTEM\\CurrentControlSet\\Services\\%s\\Performance", service);
         String value = "Disable Performance Counters";
         if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, value)) {
            Object disabled = Advapi32Util.registryGetValue(WinReg.HKEY_LOCAL_MACHINE, key, value);
            if (disabled instanceof Integer) {
               if ((Integer)disabled > 0) {
                  LOG.warn("{} counters are disabled and won't return data: {}\\\\{}\\\\{} > 0.", service, "HKEY_LOCAL_MACHINE", key, value);
                  return true;
               }
            } else {
               LOG.warn(
                  "Invalid registry value type detected for {} counters. Should be REG_DWORD. Ignoring: {}\\\\{}\\\\{}.",
                  service,
                  "HKEY_LOCAL_MACHINE",
                  key,
                  value
               );
            }
         }

         return false;
      } else {
         return Boolean.parseBoolean(perfDisabled);
      }
   }
}
