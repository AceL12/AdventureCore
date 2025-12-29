package oshi.software.os.unix.aix;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.Uptime;
import oshi.driver.unix.aix.Who;
import oshi.driver.unix.aix.perfstat.PerfstatConfig;
import oshi.driver.unix.aix.perfstat.PerfstatProcess;
import oshi.jna.platform.unix.AixLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;

@ThreadSafe
public class AixOperatingSystem extends AbstractOperatingSystem {
   private final Supplier<Perfstat.perfstat_partition_config_t> config = Memoizer.memoize(PerfstatConfig::queryConfig);
   private final Supplier<Perfstat.perfstat_process_t[]> procCpu = Memoizer.memoize(PerfstatProcess::queryProcesses, Memoizer.defaultExpiration());
   private static final long BOOTTIME = querySystemBootTimeMillis() / 1000L;

   @Override
   public String queryManufacturer() {
      return "IBM";
   }

   @Override
   public Pair<String, OperatingSystem.OSVersionInfo> queryFamilyVersionInfo() {
      Perfstat.perfstat_partition_config_t cfg = this.config.get();
      String systemName = System.getProperty("os.name");
      String archName = System.getProperty("os.arch");
      String versionNumber = System.getProperty("os.version");
      if (Util.isBlank(versionNumber)) {
         versionNumber = ExecutingCommand.getFirstAnswer("oslevel");
      }

      String releaseNumber = Native.toString(cfg.OSBuild);
      if (Util.isBlank(releaseNumber)) {
         releaseNumber = ExecutingCommand.getFirstAnswer("oslevel -s");
      } else {
         int idx = releaseNumber.lastIndexOf(32);
         if (idx > 0 && idx < releaseNumber.length()) {
            releaseNumber = releaseNumber.substring(idx + 1);
         }
      }

      return new Pair<>(systemName, new OperatingSystem.OSVersionInfo(versionNumber, archName, releaseNumber));
   }

   @Override
   protected int queryBitness(int jvmBitness) {
      if (jvmBitness == 64) {
         return 64;
      } else {
         return (this.config.get().conf & 8388608) > 0 ? 64 : 32;
      }
   }

   @Override
   public FileSystem getFileSystem() {
      return new AixFileSystem();
   }

   @Override
   public InternetProtocolStats getInternetProtocolStats() {
      return new AixInternetProtocolStats();
   }

   @Override
   public List<OSProcess> queryAllProcesses() {
      return this.getProcessListFromProcfs(-1);
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
      List<OSProcess> procs = this.getProcessListFromProcfs(pid);
      return procs.isEmpty() ? null : procs.get(0);
   }

   private List<OSProcess> getProcessListFromProcfs(int pid) {
      List<OSProcess> procs = new ArrayList<>();
      Perfstat.perfstat_process_t[] perfstat = this.procCpu.get();
      Map<Integer, Pair<Long, Long>> cpuMap = new HashMap<>();

      for (Perfstat.perfstat_process_t stat : perfstat) {
         int statpid = (int)stat.pid;
         if (pid < 0 || statpid == pid) {
            cpuMap.put(statpid, new Pair<>((long)stat.ucpu_time, (long)stat.scpu_time));
         }
      }

      for (Entry<Integer, Pair<Long, Long>> entry : cpuMap.entrySet()) {
         OSProcess proc = new AixOSProcess(entry.getKey(), entry.getValue(), this.procCpu, this);
         if (proc.getState() != OSProcess.State.INVALID) {
            procs.add(proc);
         }
      }

      return procs;
   }

   @Override
   public int getProcessId() {
      return AixLibc.INSTANCE.getpid();
   }

   @Override
   public int getProcessCount() {
      return this.procCpu.get().length;
   }

   @Override
   public int getThreadCount() {
      long tc = 0L;

      for (Perfstat.perfstat_process_t proc : this.procCpu.get()) {
         tc += proc.num_threads;
      }

      return (int)tc;
   }

   @Override
   public long getSystemUptime() {
      return System.currentTimeMillis() / 1000L - BOOTTIME;
   }

   @Override
   public long getSystemBootTime() {
      return BOOTTIME;
   }

   private static long querySystemBootTimeMillis() {
      long bootTime = Who.queryBootTime();
      return bootTime >= 1000L ? bootTime : System.currentTimeMillis() - Uptime.queryUpTime();
   }

   @Override
   public NetworkParams getNetworkParams() {
      return new AixNetworkParams();
   }

   @Override
   public List<OSService> getServices() {
      List<OSService> services = new ArrayList<>();
      List<String> systemServicesInfoList = ExecutingCommand.runNative("lssrc -a");
      if (systemServicesInfoList.size() > 1) {
         systemServicesInfoList.remove(0);

         for (String systemService : systemServicesInfoList) {
            String[] serviceSplit = ParseUtil.whitespaces.split(systemService.trim());
            if (systemService.contains("active")) {
               if (serviceSplit.length == 4) {
                  services.add(new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[2], 0), OSService.State.RUNNING));
               } else if (serviceSplit.length == 3) {
                  services.add(new OSService(serviceSplit[0], ParseUtil.parseIntOrDefault(serviceSplit[1], 0), OSService.State.RUNNING));
               }
            } else if (systemService.contains("inoperative")) {
               services.add(new OSService(serviceSplit[0], 0, OSService.State.STOPPED));
            }
         }
      }

      File dir = new File("/etc/rc.d/init.d");
      File[] listFiles;
      if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
         for (File file : listFiles) {
            String installedService = ExecutingCommand.getFirstAnswer(file.getAbsolutePath() + " status");
            if (installedService.contains("running")) {
               services.add(new OSService(file.getName(), ParseUtil.parseLastInt(installedService, 0), OSService.State.RUNNING));
            } else {
               services.add(new OSService(file.getName(), 0, OSService.State.STOPPED));
            }
         }
      }

      return services;
   }
}
