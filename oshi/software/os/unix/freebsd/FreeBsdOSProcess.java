package oshi.software.os.unix.freebsd;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI;
import com.sun.jna.platform.unix.Resource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.software.common.AbstractOSProcess;
import oshi.software.os.OSProcess;
import oshi.software.os.OSThread;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.platform.unix.freebsd.ProcstatUtil;

@ThreadSafe
public class FreeBsdOSProcess extends AbstractOSProcess {
   private static final Logger LOG = LoggerFactory.getLogger(FreeBsdOSProcess.class);
   private static final int ARGMAX = BsdSysctlUtil.sysctl("kern.argmax", 0);
   private final FreeBsdOperatingSystem os;
   static final String PS_THREAD_COLUMNS = Arrays.stream(FreeBsdOSProcess.PsThreadColumns.values())
      .map(Enum::name)
      .map(String::toLowerCase)
      .collect(Collectors.joining(","));
   private Supplier<Integer> bitness = Memoizer.memoize(this::queryBitness);
   private Supplier<String> commandLine = Memoizer.memoize(this::queryCommandLine);
   private Supplier<List<String>> arguments = Memoizer.memoize(this::queryArguments);
   private Supplier<Map<String, String>> environmentVariables = Memoizer.memoize(this::queryEnvironmentVariables);
   private String name;
   private String path = "";
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
   private long contextSwitches;
   private String commandLineBackup;

   public FreeBsdOSProcess(int pid, Map<FreeBsdOperatingSystem.PsKeywords, String> psMap, FreeBsdOperatingSystem os) {
      super(pid);
      this.os = os;
      this.updateAttributes(psMap);
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
      return this.arguments.get();
   }

   private List<String> queryArguments() {
      Memory m;
      List var4;
      label63: {
         if (ARGMAX > 0) {
            int[] mib = new int[]{1, 14, 7, this.getProcessID()};
            m = new Memory(ARGMAX);

            try {
               ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference(ARGMAX);

               label50: {
                  try {
                     if (FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, LibCAPI.size_t.ZERO) != 0) {
                        LOG.warn(
                           "Failed sysctl call for process arguments (kern.proc.args), process {} may not exist. Error code: {}",
                           this.getProcessID(),
                           Native.getLastError()
                        );
                        break label50;
                     }

                     var4 = Collections.unmodifiableList(ParseUtil.parseByteArrayToStrings(m.getByteArray(0L, size.getValue().intValue())));
                  } catch (Throwable var8) {
                     try {
                        size.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }

                     throw var8;
                  }

                  size.close();
                  break label63;
               }

               size.close();
            } catch (Throwable var9) {
               try {
                  m.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }

               throw var9;
            }

            m.close();
         }

         return Collections.emptyList();
      }

      m.close();
      return var4;
   }

   @Override
   public Map<String, String> getEnvironmentVariables() {
      return this.environmentVariables.get();
   }

   private Map<String, String> queryEnvironmentVariables() {
      Memory m;
      Map var4;
      label63: {
         if (ARGMAX > 0) {
            int[] mib = new int[]{1, 14, 35, this.getProcessID()};
            m = new Memory(ARGMAX);

            try {
               ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference(ARGMAX);

               label50: {
                  try {
                     if (FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, m, size, null, LibCAPI.size_t.ZERO) != 0) {
                        LOG.warn(
                           "Failed sysctl call for process environment variables (kern.proc.env), process {} may not exist. Error code: {}",
                           this.getProcessID(),
                           Native.getLastError()
                        );
                        break label50;
                     }

                     var4 = Collections.unmodifiableMap(ParseUtil.parseByteArrayToStringMap(m.getByteArray(0L, size.getValue().intValue())));
                  } catch (Throwable var8) {
                     try {
                        size.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }

                     throw var8;
                  }

                  size.close();
                  break label63;
               }

               size.close();
            } catch (Throwable var9) {
               try {
                  m.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }

               throw var9;
            }

            m.close();
         }

         return Collections.emptyMap();
      }

      m.close();
      return var4;
   }

   @Override
   public String getCurrentWorkingDirectory() {
      return ProcstatUtil.getCwd(this.getProcessID());
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
   public long getOpenFiles() {
      return ProcstatUtil.getOpenFiles(this.getProcessID());
   }

   @Override
   public long getSoftOpenFileLimit() {
      if (this.getProcessID() == this.os.getProcessId()) {
         Resource.Rlimit rlimit = new Resource.Rlimit();
         FreeBsdLibc.INSTANCE.getrlimit(7, rlimit);
         return rlimit.rlim_cur;
      } else {
         return this.getProcessOpenFileLimit(this.getProcessID(), 1);
      }
   }

   @Override
   public long getHardOpenFileLimit() {
      if (this.getProcessID() == this.os.getProcessId()) {
         Resource.Rlimit rlimit = new Resource.Rlimit();
         FreeBsdLibc.INSTANCE.getrlimit(7, rlimit);
         return rlimit.rlim_max;
      } else {
         return this.getProcessOpenFileLimit(this.getProcessID(), 2);
      }
   }

   @Override
   public int getBitness() {
      return this.bitness.get();
   }

   @Override
   public long getAffinityMask() {
      long bitMask = 0L;
      String cpuset = ExecutingCommand.getFirstAnswer("cpuset -gp " + this.getProcessID());
      String[] split = cpuset.split(":");
      if (split.length > 1) {
         String[] bits = split[1].split(",");

         for (String bit : bits) {
            int bitToSet = ParseUtil.parseIntOrDefault(bit.trim(), -1);
            if (bitToSet >= 0) {
               bitMask |= 1L << bitToSet;
            }
         }
      }

      return bitMask;
   }

   private int queryBitness() {
      int[] mib = new int[]{1, 14, 9, this.getProcessID()};
      Memory abi = new Memory(32L);

      byte var10;
      label61: {
         label60: {
            try {
               ByRef.CloseableSizeTByReference size;
               label57: {
                  label65: {
                     size = new ByRef.CloseableSizeTByReference(32L);

                     try {
                        if (0 != FreeBsdLibc.INSTANCE.sysctl(mib, mib.length, abi, size, null, LibCAPI.size_t.ZERO)) {
                           break label57;
                        }

                        String elf = abi.getString(0L);
                        if (elf.contains("ELF32")) {
                           var10 = 32;
                           break label65;
                        }

                        if (!elf.contains("ELF64")) {
                           break label57;
                        }

                        var10 = 64;
                     } catch (Throwable var8) {
                        try {
                           size.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }

                        throw var8;
                     }

                     size.close();
                     break label60;
                  }

                  size.close();
                  break label61;
               }

               size.close();
            } catch (Throwable var9) {
               try {
                  abi.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }

               throw var9;
            }

            abi.close();
            return 0;
         }

         abi.close();
         return var10;
      }

      abi.close();
      return var10;
   }

   @Override
   public List<OSThread> getThreadDetails() {
      String psCommand = "ps -awwxo " + PS_THREAD_COLUMNS + " -H";
      if (this.getProcessID() >= 0) {
         psCommand = psCommand + " -p " + this.getProcessID();
      }

      Predicate<Map<FreeBsdOSProcess.PsThreadColumns, String>> hasColumnsPri = threadMap -> threadMap.containsKey(FreeBsdOSProcess.PsThreadColumns.PRI);
      return ExecutingCommand.runNative(psCommand)
         .stream()
         .skip(1L)
         .parallel()
         .<Map<FreeBsdOSProcess.PsThreadColumns, String>>map(thread -> ParseUtil.stringToEnumMap(FreeBsdOSProcess.PsThreadColumns.class, thread.trim(), ' '))
         .filter(hasColumnsPri)
         .map(threadMap -> new FreeBsdOSThread(this.getProcessID(), (Map<FreeBsdOSProcess.PsThreadColumns, String>)threadMap))
         .filter(OSThread.ThreadFiltering.VALID_THREAD)
         .collect(Collectors.toList());
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
   public boolean updateAttributes() {
      String psCommand = "ps -awwxo " + FreeBsdOperatingSystem.PS_COMMAND_ARGS + " -p " + this.getProcessID();
      List<String> procList = ExecutingCommand.runNative(psCommand);
      if (procList.size() > 1) {
         Map<FreeBsdOperatingSystem.PsKeywords, String> psMap = ParseUtil.stringToEnumMap(FreeBsdOperatingSystem.PsKeywords.class, procList.get(1).trim(), ' ');
         if (psMap.containsKey(FreeBsdOperatingSystem.PsKeywords.ARGS)) {
            return this.updateAttributes(psMap);
         }
      }

      this.state = OSProcess.State.INVALID;
      return false;
   }

   private boolean updateAttributes(Map<FreeBsdOperatingSystem.PsKeywords, String> psMap) {
      long now = System.currentTimeMillis();
      switch (psMap.get(FreeBsdOperatingSystem.PsKeywords.STATE).charAt(0)) {
         case 'D':
         case 'L':
         case 'U':
            this.state = OSProcess.State.WAITING;
            break;
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'J':
         case 'K':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         default:
            this.state = OSProcess.State.OTHER;
            break;
         case 'I':
         case 'S':
            this.state = OSProcess.State.SLEEPING;
            break;
         case 'R':
            this.state = OSProcess.State.RUNNING;
            break;
         case 'T':
            this.state = OSProcess.State.STOPPED;
            break;
         case 'Z':
            this.state = OSProcess.State.ZOMBIE;
      }

      this.parentProcessID = ParseUtil.parseIntOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.PPID), 0);
      this.user = psMap.get(FreeBsdOperatingSystem.PsKeywords.USER);
      this.userID = psMap.get(FreeBsdOperatingSystem.PsKeywords.UID);
      this.group = psMap.get(FreeBsdOperatingSystem.PsKeywords.GROUP);
      this.groupID = psMap.get(FreeBsdOperatingSystem.PsKeywords.GID);
      this.threadCount = ParseUtil.parseIntOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.NLWP), 0);
      this.priority = ParseUtil.parseIntOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.PRI), 0);
      this.virtualSize = ParseUtil.parseLongOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.VSZ), 0L) * 1024L;
      this.residentSetSize = ParseUtil.parseLongOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.RSS), 0L) * 1024L;
      long elapsedTime = ParseUtil.parseDHMSOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.ETIMES), 0L);
      this.upTime = elapsedTime < 1L ? 1L : elapsedTime;
      this.startTime = now - this.upTime;
      this.kernelTime = ParseUtil.parseDHMSOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.SYSTIME), 0L);
      this.userTime = ParseUtil.parseDHMSOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.TIME), 0L) - this.kernelTime;
      this.path = psMap.get(FreeBsdOperatingSystem.PsKeywords.COMM);
      this.name = this.path.substring(this.path.lastIndexOf(47) + 1);
      this.minorFaults = ParseUtil.parseLongOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.MAJFLT), 0L);
      this.majorFaults = ParseUtil.parseLongOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.MINFLT), 0L);
      long nonVoluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.NVCSW), 0L);
      long voluntaryContextSwitches = ParseUtil.parseLongOrDefault(psMap.get(FreeBsdOperatingSystem.PsKeywords.NIVCSW), 0L);
      this.contextSwitches = voluntaryContextSwitches + nonVoluntaryContextSwitches;
      this.commandLineBackup = psMap.get(FreeBsdOperatingSystem.PsKeywords.ARGS);
      return true;
   }

   private long getProcessOpenFileLimit(long processId, int index) {
      String limitsPath = String.format("/proc/%d/limits", processId);
      if (!Files.exists(Paths.get(limitsPath))) {
         return -1L;
      } else {
         List<String> lines = FileUtil.readFile(limitsPath);
         Optional<String> maxOpenFilesLine = lines.stream().filter(line -> line.startsWith("Max open files")).findFirst();
         if (!maxOpenFilesLine.isPresent()) {
            return -1L;
         } else {
            String[] split = maxOpenFilesLine.get().split("\\D+");
            return Long.parseLong(split[index]);
         }
      }
   }

   static enum PsThreadColumns {
      TDNAME,
      LWP,
      STATE,
      ETIMES,
      SYSTIME,
      TIME,
      TDADDR,
      NIVCSW,
      NVCSW,
      MAJFLT,
      MINFLT,
      PRI;
   }
}
