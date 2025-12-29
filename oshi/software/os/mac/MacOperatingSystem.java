package oshi.software.os.mac;

import com.sun.jna.platform.mac.SystemB;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.Who;
import oshi.driver.mac.WindowInfo;
import oshi.jna.Struct;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSDesktopWindow;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class MacOperatingSystem extends AbstractOperatingSystem {
   private static final Logger LOG = LoggerFactory.getLogger(MacOperatingSystem.class);
   public static final String MACOS_VERSIONS_PROPERTIES = "oshi.macos.versions.properties";
   private static final String SYSTEM_LIBRARY_LAUNCH_AGENTS = "/System/Library/LaunchAgents";
   private static final String SYSTEM_LIBRARY_LAUNCH_DAEMONS = "/System/Library/LaunchDaemons";
   private int maxProc = 1024;
   private final String osXVersion;
   private final int major;
   private final int minor;
   private static final long BOOTTIME;

   public MacOperatingSystem() {
      String version = System.getProperty("os.version");
      int verMajor = ParseUtil.getFirstIntValue(version);
      int verMinor = ParseUtil.getNthIntValue(version, 2);
      if (verMajor == 10 && verMinor > 15) {
         String swVers = ExecutingCommand.getFirstAnswer("sw_vers -productVersion");
         if (!swVers.isEmpty()) {
            version = swVers;
         }

         verMajor = ParseUtil.getFirstIntValue(version);
         verMinor = ParseUtil.getNthIntValue(version, 2);
      }

      this.osXVersion = version;
      this.major = verMajor;
      this.minor = verMinor;
      this.maxProc = SysctlUtil.sysctl("kern.maxproc", 4096);
   }

   @Override
   public String queryManufacturer() {
      return "Apple";
   }

   @Override
   public Pair<String, OperatingSystem.OSVersionInfo> queryFamilyVersionInfo() {
      String family = this.major <= 10 && (this.major != 10 || this.minor < 12) ? System.getProperty("os.name") : "macOS";
      String codeName = this.parseCodeName();
      String buildNumber = SysctlUtil.sysctl("kern.osversion", "");
      return new Pair<>(family, new OperatingSystem.OSVersionInfo(this.osXVersion, codeName, buildNumber));
   }

   private String parseCodeName() {
      Properties verProps = FileUtil.readPropertiesFromFilename("oshi.macos.versions.properties");
      String codeName = null;
      if (this.major > 10) {
         codeName = verProps.getProperty(Integer.toString(this.major));
      } else if (this.major == 10) {
         codeName = verProps.getProperty(this.major + "." + this.minor);
      }

      if (Util.isBlank(codeName)) {
         LOG.warn("Unable to parse version {}.{} to a codename.", this.major, this.minor);
      }

      return codeName;
   }

   @Override
   protected int queryBitness(int jvmBitness) {
      return jvmBitness != 64 && (this.major != 10 || this.minor <= 6)
         ? ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("getconf LONG_BIT"), 32)
         : 64;
   }

   @Override
   public FileSystem getFileSystem() {
      return new MacFileSystem();
   }

   @Override
   public InternetProtocolStats getInternetProtocolStats() {
      return new MacInternetProtocolStats(this.isElevated());
   }

   @Override
   public List<OSSession> getSessions() {
      return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
   }

   @Override
   public List<OSProcess> queryAllProcesses() {
      List<OSProcess> procs = new ArrayList<>();
      int[] pids = new int[this.maxProc];
      Arrays.fill(pids, -1);
      int numberOfProcesses = SystemB.INSTANCE.proc_listpids(1, 0, pids, pids.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;

      for (int i = 0; i < numberOfProcesses; i++) {
         if (pids[i] >= 0) {
            OSProcess proc = this.getProcess(pids[i]);
            if (proc != null) {
               procs.add(proc);
            }
         }
      }

      return procs;
   }

   @Override
   public OSProcess getProcess(int pid) {
      OSProcess proc = new MacOSProcess(pid, this.major, this.minor, this);
      return proc.getState().equals(OSProcess.State.INVALID) ? null : proc;
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
   public int getProcessId() {
      return SystemB.INSTANCE.getpid();
   }

   @Override
   public int getProcessCount() {
      return SystemB.INSTANCE.proc_listpids(1, 0, null, 0) / SystemB.INT_SIZE;
   }

   @Override
   public int getThreadCount() {
      int[] pids = new int[this.getProcessCount() + 10];
      int numberOfProcesses = SystemB.INSTANCE.proc_listpids(1, 0, pids, pids.length) / SystemB.INT_SIZE;
      int numberOfThreads = 0;
      Struct.CloseableProcTaskInfo taskInfo = new Struct.CloseableProcTaskInfo();

      try {
         for (int i = 0; i < numberOfProcesses; i++) {
            int exit = SystemB.INSTANCE.proc_pidinfo(pids[i], 4, 0L, taskInfo, taskInfo.size());
            if (exit != -1) {
               numberOfThreads += taskInfo.pti_threadnum;
            }
         }
      } catch (Throwable var8) {
         try {
            taskInfo.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      taskInfo.close();
      return numberOfThreads;
   }

   @Override
   public long getSystemUptime() {
      return System.currentTimeMillis() / 1000L - BOOTTIME;
   }

   @Override
   public long getSystemBootTime() {
      return BOOTTIME;
   }

   @Override
   public NetworkParams getNetworkParams() {
      return new MacNetworkParams();
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

      ArrayList<File> files = new ArrayList<>();
      File dir = new File("/System/Library/LaunchAgents");
      if (dir.exists() && dir.isDirectory()) {
         files.addAll(Arrays.asList(dir.listFiles((fx, namex) -> namex.toLowerCase().endsWith(".plist"))));
      } else {
         LOG.error("Directory: /System/Library/LaunchAgents does not exist");
      }

      dir = new File("/System/Library/LaunchDaemons");
      if (dir.exists() && dir.isDirectory()) {
         files.addAll(Arrays.asList(dir.listFiles((fx, namex) -> namex.toLowerCase().endsWith(".plist"))));
      } else {
         LOG.error("Directory: /System/Library/LaunchDaemons does not exist");
      }

      for (File f : files) {
         String name = f.getName().substring(0, f.getName().length() - 6);
         int index = name.lastIndexOf(46);
         String shortName = index >= 0 && index <= name.length() - 2 ? name.substring(index + 1) : name;
         if (!running.contains(name) && !running.contains(shortName)) {
            OSService s = new OSService(name, 0, OSService.State.STOPPED);
            services.add(s);
         }
      }

      return services;
   }

   @Override
   public List<OSDesktopWindow> getDesktopWindows(boolean visibleOnly) {
      return WindowInfo.queryDesktopWindows(visibleOnly);
   }

   static {
      Struct.CloseableTimeval tv = new Struct.CloseableTimeval();

      try {
         if (SysctlUtil.sysctl("kern.boottime", tv) && tv.tv_sec.longValue() != 0L) {
            BOOTTIME = tv.tv_sec.longValue();
         } else {
            BOOTTIME = ParseUtil.parseLongOrDefault(
               ExecutingCommand.getFirstAnswer("sysctl -n kern.boottime").split(",")[0].replaceAll("\\D", ""), System.currentTimeMillis() / 1000L
            );
         }
      } catch (Throwable var4) {
         try {
            tv.close();
         } catch (Throwable var3) {
            var4.addSuppressed(var3);
         }

         throw var4;
      }

      tv.close();
   }
}
