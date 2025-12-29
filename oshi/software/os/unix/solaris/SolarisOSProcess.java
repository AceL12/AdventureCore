package oshi.software.os.unix.solaris;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.PsInfo;
import oshi.jna.platform.unix.SolarisLibc;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.UserGroupInfo;
import oshi.util.tuples.Pair;

@ThreadSafe
public class SolarisOSProcess extends AbstractOSProcess {
   private static final Logger LOG = LoggerFactory.getLogger(SolarisOSProcess.class);
   private final SolarisOperatingSystem os;
   private Supplier<Integer> bitness = Memoizer.memoize(this::queryBitness);
   private Supplier<SolarisLibc.SolarisPsInfo> psinfo = Memoizer.memoize(this::queryPsInfo, Memoizer.defaultExpiration());
   private Supplier<String> commandLine = Memoizer.memoize(this::queryCommandLine);
   private Supplier<Pair<List<String>, Map<String, String>>> cmdEnv = Memoizer.memoize(this::queryCommandlineEnvironment);
   private Supplier<SolarisLibc.SolarisPrUsage> prusage = Memoizer.memoize(this::queryPrUsage, Memoizer.defaultExpiration());
   private String name;
   private String path = "";
   private String commandLineBackup;
   private String user;
   private String userID;
   private String group;
   private String groupID;
   private OSProcess.State state = OSProcess.State.INVALID;
   private int parentProcessID;
   private int threadCount;
   private int priority;
   private long virtualSize;
   private long residentSetSize;
   private long kernelTime;
   private long userTime;
   private long startTime;
   private long upTime;
   private long bytesRead;
   private long bytesWritten;
   private long minorFaults;
   private long majorFaults;
   private long contextSwitches = 0L;

   public SolarisOSProcess(int pid, SolarisOperatingSystem os) {
      super(pid);
      this.os = os;
      this.updateAttributes();
   }

   private SolarisLibc.SolarisPsInfo queryPsInfo() {
      return PsInfo.queryPsInfo(this.getProcessID());
   }

   private SolarisLibc.SolarisPrUsage queryPrUsage() {
      return PsInfo.queryPrUsage(this.getProcessID());
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String getPath() {
      return this.path;
   }

   @Override
   public String getCommandLine() {
      return this.commandLine.get();
   }

   private String queryCommandLine() {
      String cl = String.join(" ", this.getArguments());
      return cl.isEmpty() ? this.commandLineBackup : cl;
   }

   @Override
   public List<String> getArguments() {
      return this.cmdEnv.get().getA();
   }

   @Override
   public Map<String, String> getEnvironmentVariables() {
      return this.cmdEnv.get().getB();
   }

   private Pair<List<String>, Map<String, String>> queryCommandlineEnvironment() {
      return PsInfo.queryArgsEnv(this.getProcessID(), this.psinfo.get());
   }

   @Override
   public String getCurrentWorkingDirectory() {
      try {
         String cwdLink = "/proc" + this.getProcessID() + "/cwd";
         String cwd = new File(cwdLink).getCanonicalPath();
         if (!cwd.equals(cwdLink)) {
            return cwd;
         }
      } catch (IOException var3) {
         LOG.trace("Couldn't find cwd for pid {}: {}", this.getProcessID(), var3.getMessage());
      }

      return "";
   }

   @Override
   public String getUser() {
      return this.user;
   }

   @Override
   public String getUserID() {
      return this.userID;
   }

   @Override
   public String getGroup() {
      return this.group;
   }

   @Override
   public String getGroupID() {
      return this.groupID;
   }

   @Override
   public OSProcess.State getState() {
      return this.state;
   }

   @Override
   public int getParentProcessID() {
      return this.parentProcessID;
   }

   @Override
   public int getThreadCount() {
      return this.threadCount;
   }

   @Override
   public int getPriority() {
      return this.priority;
   }

   @Override
   public long getVirtualSize() {
      return this.virtualSize;
   }

   @Override
   public long getResidentSetSize() {
      return this.residentSetSize;
   }

   @Override
   public long getKernelTime() {
      return this.kernelTime;
   }

   @Override
   public long getUserTime() {
      return this.userTime;
   }

   @Override
   public long getUpTime() {
      return this.upTime;
   }

   @Override
   public long getStartTime() {
      return this.startTime;
   }

   @Override
   public long getBytesRead() {
      return this.bytesRead;
   }

   @Override
   public long getBytesWritten() {
      return this.bytesWritten;
   }

   @Override
   public long getMinorFaults() {
      return this.minorFaults;
   }

   @Override
   public long getMajorFaults() {
      return this.majorFaults;
   }

   @Override
   public long getContextSwitches() {
      return this.contextSwitches;
   }

   @Override
   public long getOpenFiles() {
      try {
         Stream<Path> fd = Files.list(Paths.get("/proc/" + this.getProcessID() + "/fd"));

         long var2;
         try {
            var2 = fd.count();
         } catch (Throwable var5) {
            if (fd != null) {
               try {
                  fd.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }
            }

            throw var5;
         }

         if (fd != null) {
            fd.close();
         }

         return var2;
      } catch (IOException var6) {
         return 0L;
      }
   }

   @Override
   public long getSoftOpenFileLimit() {
      if (this.getProcessID() == this.os.getProcessId()) {
         Resource.Rlimit rlimit = new Resource.Rlimit();
         SolarisLibc.INSTANCE.getrlimit(7, rlimit);
         return rlimit.rlim_cur;
      } else {
         return this.getProcessOpenFileLimit(this.getProcessID(), 1);
      }
   }

   @Override
   public long getHardOpenFileLimit() {
      if (this.getProcessID() == this.os.getProcessId()) {
         Resource.Rlimit rlimit = new Resource.Rlimit();
         SolarisLibc.INSTANCE.getrlimit(7, rlimit);
         return rlimit.rlim_max;
      } else {
         return this.getProcessOpenFileLimit(this.getProcessID(), 2);
      }
   }

   @Override
   public int getBitness() {
      return this.bitness.get();
   }

   private int queryBitness() {
      for (String line : ExecutingCommand.runNative("pflags " + this.getProcessID())) {
         if (line.contains("data model")) {
            if (line.contains("LP32")) {
               return 32;
            }

            if (line.contains("LP64")) {
               return 64;
            }
         }
      }

      return 0;
   }

   @Override
   public long getAffinityMask() {
      long bitMask = 0L;
      String cpuset = ExecutingCommand.getFirstAnswer("pbind -q " + this.getProcessID());
      if (cpuset.isEmpty()) {
         for (String proc : ExecutingCommand.runNative("psrinfo")) {
            String[] split = ParseUtil.whitespaces.split(proc);
            int bitToSet = ParseUtil.parseIntOrDefault(split[0], -1);
            if (bitToSet >= 0) {
               bitMask |= 1L << bitToSet;
            }
         }

         return bitMask;
      } else {
         if (cpuset.endsWith(".") && cpuset.contains("strongly bound to processor(s)")) {
            String parse = cpuset.substring(0, cpuset.length() - 1);
            String[] split = ParseUtil.whitespaces.split(parse);

            for (int i = split.length - 1; i >= 0; i--) {
               int bitToSet = ParseUtil.parseIntOrDefault(split[i], -1);
               if (bitToSet < 0) {
                  break;
               }

               bitMask |= 1L << bitToSet;
            }
         }

         return bitMask;
      }
   }

   @Override
   public List<OSThread> getThreadDetails() {
      File directory = new File(String.format("/proc/%d/lwp", this.getProcessID()));
      File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
      return numericFiles == null
         ? Collections.emptyList()
         : Arrays.stream(numericFiles)
            .parallel()
            .<OSThread>map(lwpidFile -> new SolarisOSThread(this.getProcessID(), ParseUtil.parseIntOrDefault(lwpidFile.getName(), 0)))
            .filter(OSThread.ThreadFiltering.VALID_THREAD)
            .collect(Collectors.toList());
   }

   @Override
   public boolean updateAttributes() {
      SolarisLibc.SolarisPsInfo info = this.psinfo.get();
      if (info == null) {
         this.state = OSProcess.State.INVALID;
         return false;
      } else {
         SolarisLibc.SolarisPrUsage usage = this.prusage.get();
         long now = System.currentTimeMillis();
         this.state = getStateFromOutput((char)info.pr_lwp.pr_sname);
         this.parentProcessID = info.pr_ppid;
         this.userID = Integer.toString(info.pr_euid);
         this.user = UserGroupInfo.getUser(this.userID);
         this.groupID = Integer.toString(info.pr_egid);
         this.group = UserGroupInfo.getGroupName(this.groupID);
         this.threadCount = info.pr_nlwp;
         this.priority = info.pr_lwp.pr_pri;
         this.virtualSize = info.pr_size.longValue() * 1024L;
         this.residentSetSize = info.pr_rssize.longValue() * 1024L;
         this.startTime = info.pr_start.tv_sec.longValue() * 1000L + info.pr_start.tv_nsec.longValue() / 1000000L;
         long elapsedTime = now - this.startTime;
         this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
         this.kernelTime = 0L;
         this.userTime = info.pr_time.tv_sec.longValue() * 1000L + info.pr_time.tv_nsec.longValue() / 1000000L;
         this.commandLineBackup = Native.toString(info.pr_psargs);
         this.path = ParseUtil.whitespaces.split(this.commandLineBackup)[0];
         this.name = this.path.substring(this.path.lastIndexOf(47) + 1);
         if (usage != null) {
            this.userTime = usage.pr_utime.tv_sec.longValue() * 1000L + usage.pr_utime.tv_nsec.longValue() / 1000000L;
            this.kernelTime = usage.pr_stime.tv_sec.longValue() * 1000L + usage.pr_stime.tv_nsec.longValue() / 1000000L;
            this.bytesRead = usage.pr_ioch.longValue();
            this.majorFaults = usage.pr_majf.longValue();
            this.minorFaults = usage.pr_minf.longValue();
            this.contextSwitches = usage.pr_ictx.longValue() + usage.pr_vctx.longValue();
         }

         return true;
      }
   }

   static OSProcess.State getStateFromOutput(char stateValue) {
      OSProcess.State state;
      switch (stateValue) {
         case 'O':
            state = OSProcess.State.RUNNING;
            break;
         case 'P':
         case 'Q':
         case 'U':
         case 'V':
         case 'X':
         case 'Y':
         default:
            state = OSProcess.State.OTHER;
            break;
         case 'R':
         case 'W':
            state = OSProcess.State.WAITING;
            break;
         case 'S':
            state = OSProcess.State.SLEEPING;
            break;
         case 'T':
            state = OSProcess.State.STOPPED;
            break;
         case 'Z':
            state = OSProcess.State.ZOMBIE;
      }

      return state;
   }

   private long getProcessOpenFileLimit(long processId, int index) {
      List<String> output = ExecutingCommand.runNative("plimit " + processId);
      if (output.isEmpty()) {
         return -1L;
      } else {
         Optional<String> nofilesLine = output.stream().filter(line -> line.trim().startsWith("nofiles")).findFirst();
         if (!nofilesLine.isPresent()) {
            return -1L;
         } else {
            String[] split = nofilesLine.get().split("\\D+");
            return Long.parseLong(split[index]);
         }
      }
   }
}
