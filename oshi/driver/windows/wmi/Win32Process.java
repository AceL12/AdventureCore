package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class Win32Process {
   private static final String WIN32_PROCESS = "Win32_Process";

   private Win32Process() {
   }

   public static WbemcliUtil.WmiResult<Win32Process.CommandLineProperty> queryCommandLines(Set<Integer> pidsToQuery) {
      String sb = "Win32_Process";
      if (pidsToQuery != null) {
         sb = sb + " WHERE ProcessID=" + pidsToQuery.stream().map(String::valueOf).collect(Collectors.joining(" OR PROCESSID="));
      }

      WbemcliUtil.WmiQuery<Win32Process.CommandLineProperty> commandLineQuery = new WbemcliUtil.WmiQuery<>(sb, Win32Process.CommandLineProperty.class);
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(commandLineQuery);
   }

   public static WbemcliUtil.WmiResult<Win32Process.ProcessXPProperty> queryProcesses(Collection<Integer> pids) {
      String sb = "Win32_Process";
      if (pids != null) {
         sb = sb + " WHERE ProcessID=" + pids.stream().map(String::valueOf).collect(Collectors.joining(" OR PROCESSID="));
      }

      WbemcliUtil.WmiQuery<Win32Process.ProcessXPProperty> processQueryXP = new WbemcliUtil.WmiQuery<>(sb, Win32Process.ProcessXPProperty.class);
      return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(processQueryXP);
   }

   public static enum CommandLineProperty {
      PROCESSID,
      COMMANDLINE;
   }

   public static enum ProcessXPProperty {
      PROCESSID,
      NAME,
      KERNELMODETIME,
      USERMODETIME,
      THREADCOUNT,
      PAGEFILEUSAGE,
      HANDLECOUNT,
      EXECUTABLEPATH;
   }
}
