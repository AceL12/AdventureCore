package oshi.software.os.unix.solaris;

import com.sun.jna.platform.unix.solaris.Kstat2;
import com.sun.jna.platform.unix.solaris.LibKstat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.unix.solaris.Who;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.AbstractOperatingSystem;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.NetworkParams;
import oshi.software.os.OSProcess;
import oshi.software.os.OSService;
import oshi.software.os.OSSession;
import oshi.software.os.OperatingSystem;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
public class SolarisOperatingSystem extends AbstractOperatingSystem {
   private static final String VERSION;
   private static final String BUILD_NUMBER;
   public static final boolean HAS_KSTAT2;
   private static final Supplier<Pair<Long, Long>> BOOT_UPTIME;
   private static final long BOOTTIME;

   @Override
   public String queryManufacturer() {
      return "Oracle";
   }

   @Override
   public Pair<String, OperatingSystem.OSVersionInfo> queryFamilyVersionInfo() {
      return new Pair<>("SunOS", new OperatingSystem.OSVersionInfo(VERSION, "Solaris", BUILD_NUMBER));
   }

   @Override
   protected int queryBitness(int jvmBitness) {
      return jvmBitness == 64 ? 64 : ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("isainfo -b"), 32);
   }

   @Override
   public FileSystem getFileSystem() {
      return new SolarisFileSystem();
   }

   @Override
   public InternetProtocolStats getInternetProtocolStats() {
      return new SolarisInternetProtocolStats();
   }

   @Override
   public List<OSSession> getSessions() {
      return USE_WHO_COMMAND ? super.getSessions() : Who.queryUtxent();
   }

   @Override
   public OSProcess getProcess(int pid) {
      List<OSProcess> procs = this.getProcessListFromProcfs(pid);
      return procs.isEmpty() ? null : procs.get(0);
   }

   @Override
   public List<OSProcess> queryAllProcesses() {
      return this.queryAllProcessesFromPrStat();
   }

   @Override
   public List<OSProcess> queryChildProcesses(int parentPid) {
      List<OSProcess> allProcs = this.queryAllProcessesFromPrStat();
      Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, false);
      return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
   }

   @Override
   public List<OSProcess> queryDescendantProcesses(int parentPid) {
      List<OSProcess> allProcs = this.queryAllProcessesFromPrStat();
      Set<Integer> descendantPids = getChildrenOrDescendants(allProcs, parentPid, true);
      return allProcs.stream().filter(p -> descendantPids.contains(p.getProcessID())).collect(Collectors.toList());
   }

   private List<OSProcess> queryAllProcessesFromPrStat() {
      return this.getProcessListFromProcfs(-1);
   }

   private List<OSProcess> getProcessListFromProcfs(int pid) {
      List<OSProcess> procs = new ArrayList<>();
      File[] numericFiles = null;
      if (pid < 0) {
         File directory = new File("/proc");
         numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
      } else {
         File pidFile = new File("/proc/" + pid);
         if (pidFile.exists()) {
            numericFiles = new File[]{pidFile};
         }
      }

      if (numericFiles == null) {
         return procs;
      } else {
         for (File pidFilex : numericFiles) {
            int pidNum = ParseUtil.parseIntOrDefault(pidFilex.getName(), 0);
            OSProcess proc = new SolarisOSProcess(pidNum, this);
            if (proc.getState() != OSProcess.State.INVALID) {
               procs.add(proc);
            }
         }

         return procs;
      }
   }

   @Override
   public int getProcessId() {
      return SolarisLibc.INSTANCE.getpid();
   }

   @Override
   public int getProcessCount() {
      return ProcessStat.getPidFiles().length;
   }

   @Override
   public int getThreadCount() {
      List<String> threadList = ExecutingCommand.runNative("ps -eLo pid");
      return !threadList.isEmpty() ? threadList.size() - 1 : this.getProcessCount();
   }

   @Override
   public long getSystemUptime() {
      return querySystemUptime();
   }

   private static long querySystemUptime() {
      if (HAS_KSTAT2) {
         return BOOT_UPTIME.get().getB();
      } else {
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         long var2;
         label46: {
            try {
               LibKstat.Kstat ksp = kc.lookup("unix", 0, "system_misc");
               if (ksp != null && kc.read(ksp)) {
                  var2 = ksp.ks_snaptime / 1000000000L;
                  break label46;
               }
            } catch (Throwable var5) {
               if (kc != null) {
                  try {
                     kc.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (kc != null) {
               kc.close();
            }

            return 0L;
         }

         if (kc != null) {
            kc.close();
         }

         return var2;
      }
   }

   @Override
   public long getSystemBootTime() {
      return BOOTTIME;
   }

   private static long querySystemBootTime() {
      if (HAS_KSTAT2) {
         return BOOT_UPTIME.get().getA();
      } else {
         KstatUtil.KstatChain kc = KstatUtil.openChain();

         long var2;
         label46: {
            try {
               LibKstat.Kstat ksp = kc.lookup("unix", 0, "system_misc");
               if (ksp != null && kc.read(ksp)) {
                  var2 = KstatUtil.dataLookupLong(ksp, "boot_time");
                  break label46;
               }
            } catch (Throwable var5) {
               if (kc != null) {
                  try {
                     kc.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (kc != null) {
               kc.close();
            }

            return System.currentTimeMillis() / 1000L - querySystemUptime();
         }

         if (kc != null) {
            kc.close();
         }

         return var2;
      }
   }

   private static Pair<Long, Long> queryBootAndUptime() {
      Object[] results = KstatUtil.queryKstat2("/misc/unix/system_misc", "boot_time", "snaptime");
      long boot = results[0] == null ? System.currentTimeMillis() : (Long)results[0];
      long snap = results[1] == null ? 0L : (Long)results[1] / 1000000000L;
      return new Pair<>(boot, snap);
   }

   @Override
   public NetworkParams getNetworkParams() {
      return new SolarisNetworkParams();
   }

   @Override
   public List<OSService> getServices() {
      List<OSService> services = new ArrayList<>();
      List<String> legacySvcs = new ArrayList<>();
      File dir = new File("/etc/init.d");
      File[] listFiles;
      if (dir.exists() && dir.isDirectory() && (listFiles = dir.listFiles()) != null) {
         for (File f : listFiles) {
            legacySvcs.add(f.getName());
         }
      }

      for (String line : ExecutingCommand.runNative("svcs -p")) {
         if (line.startsWith("online")) {
            int delim = line.lastIndexOf(":/");
            if (delim > 0) {
               String name = line.substring(delim + 1);
               if (name.endsWith(":default")) {
                  name = name.substring(0, name.length() - 8);
               }

               services.add(new OSService(name, 0, OSService.State.STOPPED));
            }
         } else if (line.startsWith(" ")) {
            String[] split = ParseUtil.whitespaces.split(line.trim());
            if (split.length == 3) {
               services.add(new OSService(split[2], ParseUtil.parseIntOrDefault(split[1], 0), OSService.State.RUNNING));
            }
         } else if (line.startsWith("legacy_run")) {
            for (String svc : legacySvcs) {
               if (line.endsWith(svc)) {
                  services.add(new OSService(svc, 0, OSService.State.STOPPED));
                  break;
               }
            }
         }
      }

      return services;
   }

   static {
      String[] split = ParseUtil.whitespaces.split(ExecutingCommand.getFirstAnswer("uname -rv"));
      VERSION = split[0];
      BUILD_NUMBER = split.length > 1 ? split[1] : "";
      Kstat2 lib = null;

      try {
         lib = Kstat2.INSTANCE;
      } catch (UnsatisfiedLinkError var2) {
      }

      HAS_KSTAT2 = lib != null;
      BOOT_UPTIME = Memoizer.memoize(SolarisOperatingSystem::queryBootAndUptime, Memoizer.defaultExpiration());
      BOOTTIME = querySystemBootTime();
   }
}
