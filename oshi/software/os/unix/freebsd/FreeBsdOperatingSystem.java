package oshi.software.os.unix.freebsd;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.freebsd.Who;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class FreeBsdOperatingSystem extends AbstractOperatingSystem {
   private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOperatingSystem.class);
   private static final long BOOTTIME = querySystemBootTime();
   static final String PS_COMMAND_ARGS = Arrays.stream(FreeBsdOperatingSystem.PsKeywords.values())
      .map(Enum::name)
      .map(String::toLowerCase)
      .collect(Collectors.joining(","));

   @Override
   public String queryManufacturer() {
      return "Unix/BSD";
   }

   @Override
   public Pair<String, OperatingSystem.OSVersionInfo> queryFamilyVersionInfo() {
      String family = BsdSysctlUtil.sysctl("kern.ostype", "FreeBSD");
      String version = BsdSysctlUtil.sysctl("kern.osrelease", "");
      String versionInfo = BsdSysctlUtil.sysctl("kern.version", "");
      String buildNumber = versionInfo.split(":")[0].replace(family, "").replace(version, "").trim();
      return new Pair<>(family, new OperatingSystem.OSVersionInfo(version, null, buildNumber));
   }

   @Override
   protected int queryBitness(int jvmBitness) {
      return jvmBitness < 64 && ExecutingCommand.getFirstAnswer("uname -m").indexOf("64") == -1 ? jvmBitness : 64;
   }

   @Override
   public FileSystem getFileSystem() {
      return new FreeBsdFileSystem();
   }

   @Override
   public InternetProtocolStats getInternetProtocolStats() {
      return new FreeBsdInternetProtocolStats();
   }

   @Override
   public List<OSSession> getSessions() {
      return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
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
      String psCommand = "ps -awwxo " + PS_COMMAND_ARGS;
      if (pid >= 0) {
         psCommand = psCommand + " -p " + pid;
      }

      Predicate<Map<FreeBsdOperatingSystem.PsKeywords, String>> hasKeywordArgs = psMap -> psMap.containsKey(FreeBsdOperatingSystem.PsKeywords.ARGS);
      return ExecutingCommand.runNative(psCommand)
         .stream()
         .skip(1L)
         .parallel()
         .<Map<FreeBsdOperatingSystem.PsKeywords, String>>map(proc -> ParseUtil.stringToEnumMap(FreeBsdOperatingSystem.PsKeywords.class, proc.trim(), ' '))
         .filter(hasKeywordArgs)
         .map(
            psMap -> new FreeBsdOSProcess(
               pid < 0 ? ParseUtil.parseIntOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.PID), 0) : pid,
               (Map<FreeBsdOperatingSystem.PsKeywords, String>)psMap,
               this
            )
         )
         .filter(OperatingSystem.ProcessFiltering.VALID_PROCESS)
         .collect(Collectors.toList());
   }

   @Override
   public int getProcessId() {
      return FreeBsdLibc.INSTANCE.getpid();
   }

   @Override
   public int getProcessCount() {
      List<String> procList = ExecutingCommand.runNative("ps -axo pid");
      return !procList.isEmpty() ? procList.size() - 1 : 0;
   }

   @Override
   public int getThreadCount() {
      int threads = 0;

      for (String proc : ExecutingCommand.runNative("ps -axo nlwp")) {
         threads += ParseUtil.parseIntOrDefault(proc.trim(), 0);
      }

      return threads;
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
      FreeBsdLibc.Timeval tv = new FreeBsdLibc.Timeval();
      return BsdSysctlUtil.sysctl("kern.boottime", tv) && tv.tv_sec != 0L
         ? tv.tv_sec
         : ParseUtil.parseLongOrDefault(
            ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""), System.currentTimeMillis() / 1000L
         );
   }

   @Override
   public NetworkParams getNetworkParams() {
      return new FreeBsdNetworkParams();
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
         LOG.error("Directory: /etc/init does not exist");
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
      NLWP,
      PRI,
      VSZ,
      RSS,
      ETIMES,
      SYSTIME,
      TIME,
      COMM,
      MAJFLT,
      MINFLT,
      NVCSW,
      NIVCSW,
      ARGS;
   }
}
