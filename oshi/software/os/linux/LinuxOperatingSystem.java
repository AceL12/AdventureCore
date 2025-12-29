package oshi.software.os.linux;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.linux.Udev;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.Who;
import oshi.driver.linux.proc.Auxv;
import oshi.driver.linux.proc.CpuStat;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.linux.proc.UpTime;
import oshi.jna.Struct;
import oshi.jna.platform.linux.LinuxLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
public class LinuxOperatingSystem extends AbstractOperatingSystem {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxOperatingSystem.class);
   private static final String OS_RELEASE_LOG = "os-release: {}";
   private static final String LSB_RELEASE_A_LOG = "lsb_release -a: {}";
   private static final String LSB_RELEASE_LOG = "lsb-release: {}";
   private static final String RELEASE_DELIM = " release ";
   private static final String DOUBLE_QUOTES = "(?:^\")|(?:\"$)";
   private static final String FILENAME_PROPERTIES = "oshi.linux.filename.properties";
   public static final boolean HAS_UDEV;
   private static final long USER_HZ;
   private static final long PAGE_SIZE;
   private static final String OS_NAME;
   static final long BOOTTIME;
   private static final int[] PPID_INDEX;

   public LinuxOperatingSystem() {
      super.getVersionInfo();
   }

   @Override
   public String queryManufacturer() {
      return OS_NAME;
   }

   @Override
   public Pair<String, OperatingSystem.OSVersionInfo> queryFamilyVersionInfo() {
      Triplet<String, String, String> familyVersionCodename = queryFamilyVersionCodenameFromReleaseFiles();
      String buildNumber = null;
      List<String> procVersion = FileUtil.readFile(ProcPath.VERSION);
      if (!procVersion.isEmpty()) {
         String[] split = ParseUtil.whitespaces.split(procVersion.get(0));

         for (String s : split) {
            if (!"Linux".equals(s) && !"version".equals(s)) {
               buildNumber = s;
               break;
            }
         }
      }

      OperatingSystem.OSVersionInfo versionInfo = new OperatingSystem.OSVersionInfo(familyVersionCodename.getB(), familyVersionCodename.getC(), buildNumber);
      return new Pair<>(familyVersionCodename.getA(), versionInfo);
   }

   @Override
   protected int queryBitness(int jvmBitness) {
      return jvmBitness < 64 && !ExecutingCommand.getFirstAnswer("uname -m").contains("64") ? jvmBitness : 64;
   }

   @Override
   public FileSystem getFileSystem() {
      return new LinuxFileSystem();
   }

   @Override
   public InternetProtocolStats getInternetProtocolStats() {
      return new LinuxInternetProtocolStats();
   }

   @Override
   public List<OSSession> getSessions() {
      return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
   }

   @Override
   public OSProcess getProcess(int pid) {
      OSProcess proc = new LinuxOSProcess(pid, this);
      return !proc.getState().equals(OSProcess.State.INVALID) ? proc : null;
   }

   @Override
   public List<OSProcess> queryAllProcesses() {
      return this.queryChildProcesses(-1);
   }

   @Override
   public List<OSProcess> queryChildProcesses(int parentPid) {
      File[] pidFiles = ProcessStat.getPidFiles();
      if (parentPid >= 0) {
         return this.queryProcessList(getChildrenOrDescendants(getParentPidsFromProcFiles(pidFiles), parentPid, false));
      } else {
         Set<Integer> descendantPids = new HashSet<>();

         for (File procFile : pidFiles) {
            int pid = ParseUtil.parseIntOrDefault(procFile.getName(), -2);
            if (pid != -2) {
               descendantPids.add(pid);
            }
         }

         return this.queryProcessList(descendantPids);
      }
   }

   @Override
   public List<OSProcess> queryDescendantProcesses(int parentPid) {
      File[] pidFiles = ProcessStat.getPidFiles();
      return this.queryProcessList(getChildrenOrDescendants(getParentPidsFromProcFiles(pidFiles), parentPid, true));
   }

   private List<OSProcess> queryProcessList(Set<Integer> descendantPids) {
      List<OSProcess> procs = new ArrayList<>();

      for (int pid : descendantPids) {
         OSProcess proc = new LinuxOSProcess(pid, this);
         if (!proc.getState().equals(OSProcess.State.INVALID)) {
            procs.add(proc);
         }
      }

      return procs;
   }

   private static Map<Integer, Integer> getParentPidsFromProcFiles(File[] pidFiles) {
      Map<Integer, Integer> parentPidMap = new HashMap<>();

      for (File procFile : pidFiles) {
         int pid = ParseUtil.parseIntOrDefault(procFile.getName(), 0);
         parentPidMap.put(pid, getParentPidFromProcFile(pid));
      }

      return parentPidMap;
   }

   private static int getParentPidFromProcFile(int pid) {
      String stat = FileUtil.getStringFromFile(String.format("/proc/%d/stat", pid));
      if (stat.isEmpty()) {
         return 0;
      } else {
         long[] statArray = ParseUtil.parseStringToLongArray(stat, PPID_INDEX, ProcessStat.PROC_PID_STAT_LENGTH, ' ');
         return (int)statArray[0];
      }
   }

   @Override
   public int getProcessId() {
      return LinuxLibc.INSTANCE.getpid();
   }

   @Override
   public int getProcessCount() {
      return ProcessStat.getPidFiles().length;
   }

   @Override
   public int getThreadCount() {
      try {
         Struct.CloseableSysinfo info = new Struct.CloseableSysinfo();

         short var7;
         label35: {
            try {
               if (0 != LibC.INSTANCE.sysinfo(info)) {
                  LOG.error("Failed to get process thread count. Error code: {}", Native.getLastError());
                  var7 = 0;
                  break label35;
               }

               var7 = info.procs;
            } catch (Throwable var5) {
               try {
                  info.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            info.close();
            return var7;
         }

         info.close();
         return var7;
      } catch (NoClassDefFoundError | UnsatisfiedLinkError var6) {
         LOG.error("Failed to get procs from sysinfo. {}", var6.getMessage());
         return 0;
      }
   }

   @Override
   public long getSystemUptime() {
      return (long)UpTime.getSystemUptimeSeconds();
   }

   @Override
   public long getSystemBootTime() {
      return BOOTTIME;
   }

   @Override
   public NetworkParams getNetworkParams() {
      return new LinuxNetworkParams();
   }

   private static Triplet<String, String, String> queryFamilyVersionCodenameFromReleaseFiles() {
      Triplet<String, String, String> familyVersionCodename;
      if ((familyVersionCodename = readDistribRelease("/etc/system-release")) != null) {
         return familyVersionCodename;
      } else if ((familyVersionCodename = readOsRelease()) != null) {
         return familyVersionCodename;
      } else if ((familyVersionCodename = execLsbRelease()) != null) {
         return familyVersionCodename;
      } else if ((familyVersionCodename = readLsbRelease()) != null) {
         return familyVersionCodename;
      } else {
         String etcDistribRelease = getReleaseFilename();
         if ((familyVersionCodename = readDistribRelease(etcDistribRelease)) != null) {
            return familyVersionCodename;
         } else {
            String family = filenameToFamily(
               etcDistribRelease.replace("/etc/", "").replace("release", "").replace("version", "").replace("-", "").replace("_", "")
            );
            return new Triplet<>(family, "unknown", "unknown");
         }
      }
   }

   private static Triplet<String, String, String> readOsRelease() {
      String family = null;
      String versionId = "unknown";
      String codeName = "unknown";

      for (String line : FileUtil.readFile("/etc/os-release")) {
         if (line.startsWith("VERSION=")) {
            LOG.debug("os-release: {}", line);
            line = line.replace("VERSION=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
            String[] split = line.split("[()]");
            if (split.length <= 1) {
               split = line.split(", ");
            }

            if (split.length > 0) {
               versionId = split[0].trim();
            }

            if (split.length > 1) {
               codeName = split[1].trim();
            }
         } else if (line.startsWith("NAME=") && family == null) {
            LOG.debug("os-release: {}", line);
            family = line.replace("NAME=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
         } else if (line.startsWith("VERSION_ID=") && versionId.equals("unknown")) {
            LOG.debug("os-release: {}", line);
            versionId = line.replace("VERSION_ID=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
         }
      }

      return family == null ? null : new Triplet<>(family, versionId, codeName);
   }

   private static Triplet<String, String, String> execLsbRelease() {
      String family = null;
      String versionId = "unknown";
      String codeName = "unknown";

      for (String line : ExecutingCommand.runNative("lsb_release -a")) {
         if (line.startsWith("Description:")) {
            LOG.debug("lsb_release -a: {}", line);
            line = line.replace("Description:", "").trim();
            if (line.contains(" release ")) {
               Triplet<String, String, String> triplet = parseRelease(line, " release ");
               family = triplet.getA();
               if (versionId.equals("unknown")) {
                  versionId = triplet.getB();
               }

               if (codeName.equals("unknown")) {
                  codeName = triplet.getC();
               }
            }
         } else if (line.startsWith("Distributor ID:") && family == null) {
            LOG.debug("lsb_release -a: {}", line);
            family = line.replace("Distributor ID:", "").trim();
         } else if (line.startsWith("Release:") && versionId.equals("unknown")) {
            LOG.debug("lsb_release -a: {}", line);
            versionId = line.replace("Release:", "").trim();
         } else if (line.startsWith("Codename:") && codeName.equals("unknown")) {
            LOG.debug("lsb_release -a: {}", line);
            codeName = line.replace("Codename:", "").trim();
         }
      }

      return family == null ? null : new Triplet<>(family, versionId, codeName);
   }

   private static Triplet<String, String, String> readLsbRelease() {
      String family = null;
      String versionId = "unknown";
      String codeName = "unknown";

      for (String line : FileUtil.readFile("/etc/lsb-release")) {
         if (line.startsWith("DISTRIB_DESCRIPTION=")) {
            LOG.debug("lsb-release: {}", line);
            line = line.replace("DISTRIB_DESCRIPTION=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
            if (line.contains(" release ")) {
               Triplet<String, String, String> triplet = parseRelease(line, " release ");
               family = triplet.getA();
               if (versionId.equals("unknown")) {
                  versionId = triplet.getB();
               }

               if (codeName.equals("unknown")) {
                  codeName = triplet.getC();
               }
            }
         } else if (line.startsWith("DISTRIB_ID=") && family == null) {
            LOG.debug("lsb-release: {}", line);
            family = line.replace("DISTRIB_ID=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
         } else if (line.startsWith("DISTRIB_RELEASE=") && versionId.equals("unknown")) {
            LOG.debug("lsb-release: {}", line);
            versionId = line.replace("DISTRIB_RELEASE=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
         } else if (line.startsWith("DISTRIB_CODENAME=") && codeName.equals("unknown")) {
            LOG.debug("lsb-release: {}", line);
            codeName = line.replace("DISTRIB_CODENAME=", "").replaceAll("(?:^\")|(?:\"$)", "").trim();
         }
      }

      return family == null ? null : new Triplet<>(family, versionId, codeName);
   }

   private static Triplet<String, String, String> readDistribRelease(String filename) {
      if (new File(filename).exists()) {
         for (String line : FileUtil.readFile(filename)) {
            LOG.debug("{}: {}", filename, line);
            if (line.contains(" release ")) {
               return parseRelease(line, " release ");
            }

            if (line.contains(" VERSION ")) {
               return parseRelease(line, " VERSION ");
            }
         }
      }

      return null;
   }

   private static Triplet<String, String, String> parseRelease(String line, String splitLine) {
      String[] split = line.split(splitLine);
      String family = split[0].trim();
      String versionId = "unknown";
      String codeName = "unknown";
      if (split.length > 1) {
         split = split[1].split("[()]");
         if (split.length > 0) {
            versionId = split[0].trim();
         }

         if (split.length > 1) {
            codeName = split[1].trim();
         }
      }

      return new Triplet<>(family, versionId, codeName);
   }

   protected static String getReleaseFilename() {
      File etc = new File("/etc");
      File[] matchingFiles = etc.listFiles(
         f -> (f.getName().endsWith("-release") || f.getName().endsWith("-version") || f.getName().endsWith("_release") || f.getName().endsWith("_version"))
            && !f.getName().endsWith("os-release")
            && !f.getName().endsWith("lsb-release")
            && !f.getName().endsWith("system-release")
      );
      if (matchingFiles != null && matchingFiles.length > 0) {
         return matchingFiles[0].getPath();
      } else {
         return new File("/etc/release").exists() ? "/etc/release" : "/etc/issue";
      }
   }

   private static String filenameToFamily(String name) {
      if (name.isEmpty()) {
         return "Solaris";
      } else if ("issue".equalsIgnoreCase(name)) {
         return "Unknown";
      } else {
         Properties filenameProps = FileUtil.readPropertiesFromFilename("oshi.linux.filename.properties");
         String family = filenameProps.getProperty(name.toLowerCase());
         return family != null ? family : name.substring(0, 1).toUpperCase() + name.substring(1);
      }
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

      boolean systemctlFound = false;

      for (String str : ExecutingCommand.runNative("systemctl list-unit-files")) {
         String[] split = ParseUtil.whitespaces.split(str);
         if (split.length >= 2 && split[0].endsWith(".service") && "enabled".equals(split[1])) {
            String name = split[0].substring(0, split[0].length() - 8);
            int index = name.lastIndexOf(46);
            String shortName = index >= 0 && index <= name.length() - 2 ? name.substring(index + 1) : name;
            if (!running.contains(name) && !running.contains(shortName)) {
               OSService s = new OSService(name, 0, OSService.State.STOPPED);
               services.add(s);
               systemctlFound = true;
            }
         }
      }

      if (!systemctlFound) {
         File dir = new File("/etc/init");
         if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles((fx, namex) -> namex.toLowerCase().endsWith(".conf"))) {
               String name = f.getName().substring(0, f.getName().length() - 5);
               int index = name.lastIndexOf(46);
               String shortName = index >= 0 && index <= name.length() - 2 ? name.substring(index + 1) : name;
               if (!running.contains(name) && !running.contains(shortName)) {
                  OSService s = new OSService(name, 0, OSService.State.STOPPED);
                  services.add(s);
               }
            }
         } else {
            LOG.error("Directory: /etc/init does not exist");
         }
      }

      return services;
   }

   public static long getHz() {
      return USER_HZ;
   }

   public static long getPageSize() {
      return PAGE_SIZE;
   }

   static {
      Udev lib = null;

      try {
         lib = Udev.INSTANCE;
      } catch (NoClassDefFoundError | UnsatisfiedLinkError var5) {
      }

      HAS_UDEV = lib != null;
      Map<Integer, Long> auxv = Auxv.queryAuxv();
      long hz = auxv.getOrDefault(17, 0L);
      if (hz > 0L) {
         USER_HZ = hz;
      } else {
         USER_HZ = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf CLK_TCK"), 100L);
      }

      long pagesz = Auxv.queryAuxv().getOrDefault(6, 0L);
      if (pagesz > 0L) {
         PAGE_SIZE = pagesz;
      } else {
         PAGE_SIZE = ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("getconf PAGE_SIZE"), 4096L);
      }

      OS_NAME = ExecutingCommand.getFirstAnswer("uname -o");
      long tempBT = CpuStat.getBootTime();
      if (tempBT == 0L) {
         tempBT = System.currentTimeMillis() / 1000L - (long)UpTime.getSystemUptimeSeconds();
      }

      BOOTTIME = tempBT;
      PPID_INDEX = new int[]{3};
   }
}
