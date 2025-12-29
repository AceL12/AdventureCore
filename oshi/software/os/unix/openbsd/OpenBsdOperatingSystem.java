package oshi.software.os.unix.openbsd;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class OpenBsdOperatingSystem extends AbstractOperatingSystem {
   private static final Logger LOG = LoggerFactory.getLogger(OpenBsdOperatingSystem.class);
   private static final long BOOTTIME = querySystemBootTime();
   static final String PS_COMMAND_ARGS = Arrays.stream(OpenBsdOperatingSystem.PsKeywords.values())
      .map(Enum::name)
      .map(String::toLowerCase)
      .collect(Collectors.joining(","));

   @Override
   public String queryManufacturer() {
      return "Unix/BSD";
   }

   @Override
   public Pair<String, OperatingSystem.OSVersionInfo> queryFamilyVersionInfo() {
      int[] mib = new int[]{1, 1};
      String family = OpenBsdSysctlUtil.sysctl(mib, "OpenBSD");
      mib[1] = 2;
      String version = OpenBsdSysctlUtil.sysctl(mib, "");
      mib[1] = 4;
      String versionInfo = OpenBsdSysctlUtil.sysctl(mib, "");
      String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();
      return new Pair<>(family, new OperatingSystem.OSVersionInfo(version, null, buildNumber));
   }

   @Override
   protected int queryBitness(int jvmBitness) {
      return jvmBitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") == -1 ? jvmBitness : 64;
   }

   @Override
   public FileSystem getFileSystem() {
      return new OpenBsdFileSystem();
   }

   @Override
   public InternetProtocolStats getInternetProtocolStats() {
      return new OpenBsdInternetProtocolStats();
   }

   @Override
   public List<OSProcess> queryAllProcesses() {
      return this.getProcessListFromPS(-1);
   }

   @Override
   public List<OSProcess> queryChildProcesses(int parentPid) {
      List<OSProcess> allProcs = this.queryAllProcesses();
      Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
      return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
   }

   @Override
   public List<OSProcess> queryDescendantProcesses(int parentPid) {
      List<OSProcess> allProcs = this.queryAllProcesses();
      Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
      return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
   }

   @Override
   public OSProcess getProcess(int pid) {
      List<OSProcess> procs = this.getProcessListFromPS(pid);
      return procs.isEmpty() ? null : procs.get(0);
   }

   private List<OSProcess> getProcessListFromPS(int pid) {
      List<OSProcess> procs = new ArrayList<>();
      String psCommand = "ps -awwxo " + PS_COMMAND_ARGS;
      if (pid >= 0) {
         psCommand = psCommand + " -p " + pid;
      }

      List<String> procList = ExecutingCommand.runNative(psCommand);
      if (!procList.isEmpty() && procList.size() >= 2) {
         procList.remove(0);

         for (String proc : procList) {
            Map<OpenBsdOperatingSystem.PsKeywords, String> psMap = ParseUtil.stringToEnumMap(OpenBsdOperatingSystem.PsKeywords.class, proc.trim(), ' ');
            if (psMap.containsKey(OpenBsdOperatingSystem.PsKeywords.ARGS)) {
               procs.add(new OpenBsdOSProcess(pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(OpenBsdOperatingSystem.PsKeywords.PID), 0) : pid, psMap, this));
            }
         }

         return procs;
      } else {
         return procs;
      }
   }

   @Override
   public int getProcessId() {
      return OpenBsdLibc.INSTANCE.getpid();
   }

   @Override
   public int getProcessCount() {
      List<String> procList = ExecutingCommand.runNative("ps -axo pid");
      return !procList.isEmpty() ? procList.size() - 1 : 0;
   }

   @Override
   public int getThreadCount() {
      List<String> threadList = ExecutingCommand.runNative("ps -axHo tid");
      return !threadList.isEmpty() ? threadList.size() - 1 : 0;
   }

   @Override
   public long getSystemUptime() {
      return System.currentTimeMillis() / 1000L - BOOTTIME;
   }

   @Override
   public long getSystemBootTime() {
      return BOOTTIME;
   }

   private static long querySystemBootTime() {
      return ParseUtil.parseLongOrDefault(
         ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""), System.currentTimeMillis() / 1000L
      );
   }

   @Override
   public NetworkParams getNetworkParams() {
      return new OpenBsdNetworkParams();
   }

   @Override
   public List<OSService> getServices() {
      List<OSService> services = new ArrayList<>();
      Set<String> running = new HashSet<>();

      for (OSProcess p : this.getChildProcesses(1, OperatingSystem.ProcessFiltering.ALL_PROCESSES, OperatingSystem.ProcessSorting.PID_ASC, 0)) {
         OSService s = new OSService(p.getName(), p.getProcessID(), OSService.State.RUNNING);
         services.add(s);
         running.add(p.getName());
      }

      File dir = new File("/etc/rc.d");
      File[] listFiles;
      if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
         for (File f : listFiles) {
            String name = f.getName();
            if (!running.contains(name)) {
               OSService s = new OSService(name, 0, OSService.State.STOPPED);
               services.add(s);
            }
         }
      } else {
         LOG.error("Directory: /etc/rc.d does not exist");
      }

      return services;
   }

   static enum PsKeywords {
      STATE,
      PID,
      PPID,
      USER,
      UID,
      GROUP,
      GID,
      PRI,
      VSZ,
      RSS,
      ETIME,
      CPUTIME,
      COMM,
      MAJFLT,
      MINFLT,
      NVCSW,
      NIVCSW,
      ARGS;
   }
}
