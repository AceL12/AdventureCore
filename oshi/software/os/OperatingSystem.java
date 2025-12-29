package oshi.software.os;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.Who;
import oshi.driver.unix.Xwininfo;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;

@ThreadSafe
public interface OperatingSystem {
   String getFamily();

   String getManufacturer();

   OperatingSystem.OSVersionInfo getVersionInfo();

   FileSystem getFileSystem();

   InternetProtocolStats getInternetProtocolStats();

   default List<OSProcess> getProcesses() {
      return this.getProcesses(null, null, 0);
   }

   List<OSProcess> getProcesses(Predicate<OSProcess> var1, Comparator<OSProcess> var2, int var3);

   default List<OSProcess> getProcesses(Collection<Integer> pids) {
      return pids.stream().map(this::getProcess).filter(Objects::nonNull).filter(OperatingSystem.ProcessFiltering.VALID_PROCESS).collect(Collectors.toList());
   }

   OSProcess getProcess(int var1);

   List<OSProcess> getChildProcesses(int var1, Predicate<OSProcess> var2, Comparator<OSProcess> var3, int var4);

   List<OSProcess> getDescendantProcesses(int var1, Predicate<OSProcess> var2, Comparator<OSProcess> var3, int var4);

   int getProcessId();

   int getProcessCount();

   int getThreadCount();

   int getBitness();

   long getSystemUptime();

   long getSystemBootTime();

   default boolean isElevated() {
      return 0 == ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("id -u"), -1);
   }

   NetworkParams getNetworkParams();

   default List<OSService> getServices() {
      return new ArrayList<>();
   }

   default List<OSSession> getSessions() {
      return Who.queryWho();
   }

   default List<OSDesktopWindow> getDesktopWindows(boolean visibleOnly) {
      return Xwininfo.queryXWindows(visibleOnly);
   }

   @Immutable
   public static class OSVersionInfo {
      private final String version;
      private final String codeName;
      private final String buildNumber;
      private final String versionStr;

      public OSVersionInfo(String version, String codeName, String buildNumber) {
         this.version = version;
         this.codeName = codeName;
         this.buildNumber = buildNumber;
         StringBuilder sb = new StringBuilder(this.getVersion() != null ? this.getVersion() : "unknown");
         if (!Util.isBlank(this.getCodeName())) {
            sb.append(" (").append(this.getCodeName()).append(')');
         }

         if (!Util.isBlank(this.getBuildNumber())) {
            sb.append(" build ").append(this.getBuildNumber());
         }

         this.versionStr = sb.toString();
      }

      public String getVersion() {
         return this.version;
      }

      public String getCodeName() {
         return this.codeName;
      }

      public String getBuildNumber() {
         return this.buildNumber;
      }

      @Override
      public String toString() {
         return this.versionStr;
      }
   }

   public static final class ProcessFiltering {
      public static final Predicate<OSProcess> ALL_PROCESSES = p -> true;
      public static final Predicate<OSProcess> VALID_PROCESS = p -> !p.getState().equals(OSProcess.State.INVALID);
      public static final Predicate<OSProcess> NO_PARENT = p -> p.getParentProcessID() == p.getProcessID();
      public static final Predicate<OSProcess> BITNESS_64 = p -> p.getBitness() == 64;
      public static final Predicate<OSProcess> BITNESS_32 = p -> p.getBitness() == 32;

      private ProcessFiltering() {
      }
   }

   public static final class ProcessSorting {
      public static final Comparator<OSProcess> NO_SORTING = (p1, p2) -> 0;
      public static final Comparator<OSProcess> CPU_DESC = Comparator.comparingDouble(OSProcess::getProcessCpuLoadCumulative).reversed();
      public static final Comparator<OSProcess> RSS_DESC = Comparator.comparingLong(OSProcess::getResidentSetSize).reversed();
      public static final Comparator<OSProcess> UPTIME_ASC = Comparator.comparingLong(OSProcess::getUpTime);
      public static final Comparator<OSProcess> UPTIME_DESC = UPTIME_ASC.reversed();
      public static final Comparator<OSProcess> PID_ASC = Comparator.comparingInt(OSProcess::getProcessID);
      public static final Comparator<OSProcess> PARENTPID_ASC = Comparator.comparingInt(OSProcess::getParentProcessID);
      public static final Comparator<OSProcess> NAME_ASC = Comparator.comparing(OSProcess::getName, String.CASE_INSENSITIVE_ORDER);

      private ProcessSorting() {
      }
   }
}
