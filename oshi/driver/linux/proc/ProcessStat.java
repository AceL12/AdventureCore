package oshi.driver.linux.proc;

import java.io.File;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSProcess;
import oshi.util.Constants;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class ProcessStat {
   private static final Pattern SOCKET = Pattern.compile("socket:\\[(\\d+)\\]");
   public static final int PROC_PID_STAT_LENGTH;

   private ProcessStat() {
   }

   public static Triplet<String, Character, Map<ProcessStat.PidStat, Long>> getPidStats(int pid) {
      String stat = FileUtil.getStringFromFile(String.format(ProcPath.PID_STAT, pid));
      if (stat.isEmpty()) {
         return null;
      } else {
         int nameStart = stat.indexOf(40) + 1;
         int nameEnd = stat.indexOf(41);
         String name = stat.substring(nameStart, nameEnd);
         Character state = stat.charAt(nameEnd + 2);
         String[] split = ParseUtil.whitespaces.split(stat.substring(nameEnd + 4).trim());
         Map<ProcessStat.PidStat, Long> statMap = new EnumMap<>(ProcessStat.PidStat.class);
         ProcessStat.PidStat[] enumArray = ProcessStat.PidStat.class.getEnumConstants();

         for (int i = 3; i < enumArray.length && i - 3 < split.length; i++) {
            statMap.put(enumArray[i], ParseUtil.parseLongOrDefault(split[i - 3], 0L));
         }

         return new Triplet<>(name, state, statMap);
      }
   }

   public static Map<ProcessStat.PidStatM, Long> getPidStatM(int pid) {
      String statm = FileUtil.getStringFromFile(String.format(ProcPath.PID_STATM, pid));
      if (statm.isEmpty()) {
         return null;
      } else {
         String[] split = ParseUtil.whitespaces.split(statm);
         Map<ProcessStat.PidStatM, Long> statmMap = new EnumMap<>(ProcessStat.PidStatM.class);
         ProcessStat.PidStatM[] enumArray = ProcessStat.PidStatM.class.getEnumConstants();

         for (int i = 0; i < enumArray.length && i < split.length; i++) {
            statmMap.put(enumArray[i], ParseUtil.parseLongOrDefault(split[i], 0L));
         }

         return statmMap;
      }
   }

   public static File[] getFileDescriptorFiles(int pid) {
      return listNumericFiles(String.format(ProcPath.PID_FD, pid));
   }

   public static File[] getPidFiles() {
      return listNumericFiles(ProcPath.PROC);
   }

   public static Map<Integer, Integer> querySocketToPidMap() {
      Map<Integer, Integer> pidMap = new HashMap<>();

      for (File f : getPidFiles()) {
         int pid = ParseUtil.parseIntOrDefault(f.getName(), -1);
         File[] fds = getFileDescriptorFiles(pid);

         for (File fd : fds) {
            String symLink = FileUtil.readSymlinkTarget(fd);
            if (symLink != null) {
               Matcher m = SOCKET.matcher(symLink);
               if (m.matches()) {
                  pidMap.put(ParseUtil.parseIntOrDefault(m.group(1), -1), pid);
               }
            }
         }
      }

      return pidMap;
   }

   public static List<Integer> getThreadIds(int pid) {
      File[] threads = listNumericFiles(String.format(ProcPath.TASK_PATH, pid));
      return Arrays.stream(threads)
         .map(thread -> ParseUtil.parseIntOrDefault(thread.getName(), 0))
         .filter(threadId -> threadId != pid)
         .collect(Collectors.toList());
   }

   private static File[] listNumericFiles(String path) {
      File directory = new File(path);
      File[] numericFiles = directory.listFiles(file -> Constants.DIGITS.matcher(file.getName()).matches());
      return numericFiles == null ? new File[0] : numericFiles;
   }

   public static OSProcess.State getState(char stateValue) {
      OSProcess.State state;
      switch (stateValue) {
         case 'D':
            state = OSProcess.State.WAITING;
            break;
         case 'R':
            state = OSProcess.State.RUNNING;
            break;
         case 'S':
            state = OSProcess.State.SLEEPING;
            break;
         case 'T':
            state = OSProcess.State.STOPPED;
            break;
         case 'Z':
            state = OSProcess.State.ZOMBIE;
            break;
         default:
            state = OSProcess.State.OTHER;
      }

      return state;
   }

   static {
      String stat = FileUtil.getStringFromFile(ProcPath.SELF_STAT);
      if (stat.contains(")")) {
         PROC_PID_STAT_LENGTH = ParseUtil.countStringToLongArray(stat, ' ') + 3;
      } else {
         PROC_PID_STAT_LENGTH = 52;
      }
   }

   public static enum PidStat {
      PID,
      COMM,
      STATE,
      PPID,
      PGRP,
      SESSION,
      TTY_NR,
      PTGID,
      FLAGS,
      MINFLT,
      CMINFLT,
      MAJFLT,
      CMAJFLT,
      UTIME,
      STIME,
      CUTIME,
      CSTIME,
      PRIORITY,
      NICE,
      NUM_THREADS,
      ITREALVALUE,
      STARTTIME,
      VSIZE,
      RSS,
      RSSLIM,
      STARTCODE,
      ENDCODE,
      STARTSTACK,
      KSTKESP,
      KSTKEIP,
      SIGNAL,
      BLOCKED,
      SIGIGNORE,
      SIGCATCH,
      WCHAN,
      NSWAP,
      CNSWAP,
      EXIT_SIGNAL,
      PROCESSOR,
      RT_PRIORITY,
      POLICY,
      DELAYACCT_BLKIO_TICKS,
      GUEST_TIME,
      CGUEST_TIME,
      START_DATA,
      END_DATA,
      START_BRK,
      ARG_START,
      ARG_END,
      ENV_START,
      ENV_END,
      EXIT_CODE;
   }

   public static enum PidStatM {
      SIZE,
      RESIDENT,
      SHARED,
      TEXT,
      LIB,
      DATA,
      DT;
   }
}
