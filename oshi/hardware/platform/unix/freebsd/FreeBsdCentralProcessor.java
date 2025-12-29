package oshi.hardware.platform.unix.freebsd;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.common.AbstractCentralProcessor;
import oshi.jna.ByRef;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class FreeBsdCentralProcessor extends AbstractCentralProcessor {
   private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessor.class);
   private static final Pattern CPUMASK = Pattern.compile(".*<cpu\\s.*mask=\"(?:0x)?(\\p{XDigit}+)\".*>.*</cpu>.*");
   private static final long CPTIME_SIZE;

   @Override
   protected CentralProcessor.ProcessorIdentifier queryProcessorId() {
      Pattern identifierPattern = Pattern.compile("Origin=\"([^\"]*)\".*Id=(\\S+).*Family=(\\S+).*Model=(\\S+).*Stepping=(\\S+).*");
      Pattern featuresPattern = Pattern.compile("Features=(\\S+)<.*");
      String cpuVendor = "";
      String cpuName = BsdSysctlUtil.sysctl("hw.model", "");
      String cpuFamily = "";
      String cpuModel = "";
      String cpuStepping = "";
      long cpuFreq = BsdSysctlUtil.sysctl("hw.clockrate", 0L) * 1000000L;
      long processorIdBits = 0L;

      for (String line : FileUtil.readFile("/var/run/dmesg.boot")) {
         line = line.trim();
         if (line.startsWith("CPU:") && cpuName.isEmpty()) {
            cpuName = line.replace("CPU:", "").trim();
         } else if (line.startsWith("Origin=")) {
            Matcher m = identifierPattern.matcher(line);
            if (m.matches()) {
               cpuVendor = m.group(1);
               processorIdBits |= Long.decode(m.group(2));
               cpuFamily = Integer.decode(m.group(3)).toString();
               cpuModel = Integer.decode(m.group(4)).toString();
               cpuStepping = Integer.decode(m.group(5)).toString();
            }
         } else if (line.startsWith("Features=")) {
            Matcher m = featuresPattern.matcher(line);
            if (m.matches()) {
               processorIdBits |= Long.decode(m.group(1)) << 32;
            }
            break;
         }
      }

      boolean cpu64bit = ExecutingCommand.getFirstAnswer("uname -m").trim().contains("64");
      String processorID = getProcessorIDfromDmiDecode(processorIdBits);
      return new CentralProcessor.ProcessorIdentifier(cpuVendor, cpuName, cpuFamily, cpuModel, cpuStepping, processorID, cpu64bit, cpuFreq);
   }

   @Override
   protected Triplet<List<CentralProcessor.LogicalProcessor>, List<CentralProcessor.PhysicalProcessor>, List<CentralProcessor.ProcessorCache>> initProcessorCounts() {
      List<CentralProcessor.LogicalProcessor> logProcs = parseTopology();
      if (logProcs.isEmpty()) {
         logProcs.add(new CentralProcessor.LogicalProcessor(0, 0, 0));
      }

      Map<Integer, String> dmesg = new HashMap<>();
      Pattern normal = Pattern.compile("cpu(\\\\d+): (.+) on .*");
      Pattern hybrid = Pattern.compile("CPU\\\\s*(\\\\d+): (.+) affinity:.*");

      for (String s : FileUtil.readFile("/var/run/dmesg.boot")) {
         Matcher h = hybrid.matcher(s);
         if (h.matches()) {
            int coreId = ParseUtil.parseIntOrDefault(h.group(1), 0);
            dmesg.put(coreId, h.group(2).trim());
         } else {
            Matcher n = normal.matcher(s);
            if (n.matches()) {
               int coreId = ParseUtil.parseIntOrDefault(n.group(1), 0);
               dmesg.putIfAbsent(coreId, n.group(2).trim());
            }
         }
      }

      List<CentralProcessor.PhysicalProcessor> physProcs = dmesg.isEmpty() ? null : this.createProcListFromDmesg(logProcs, dmesg);
      List<CentralProcessor.ProcessorCache> caches = this.getCacheInfoFromLscpu();
      return new Triplet<>(logProcs, physProcs, caches);
   }

   private List<CentralProcessor.ProcessorCache> getCacheInfoFromLscpu() {
      Set<CentralProcessor.ProcessorCache> caches = new HashSet<>();

      for (String checkLine : ExecutingCommand.runNative("lscpu")) {
         if (checkLine.contains("L1d cache:")) {
            caches.add(
               new CentralProcessor.ProcessorCache(
                  1, 0, 0, ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), CentralProcessor.ProcessorCache.Type.DATA
               )
            );
         } else if (checkLine.contains("L1i cache:")) {
            caches.add(
               new CentralProcessor.ProcessorCache(
                  1, 0, 0, ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), CentralProcessor.ProcessorCache.Type.INSTRUCTION
               )
            );
         } else if (checkLine.contains("L2 cache:")) {
            caches.add(
               new CentralProcessor.ProcessorCache(
                  2, 0, 0, ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), CentralProcessor.ProcessorCache.Type.UNIFIED
               )
            );
         } else if (checkLine.contains("L3 cache:")) {
            caches.add(
               new CentralProcessor.ProcessorCache(
                  3, 0, 0, ParseUtil.parseDecimalMemorySizeToBinary(checkLine.split(":")[1].trim()), CentralProcessor.ProcessorCache.Type.UNIFIED
               )
            );
         }
      }

      return orderedProcCaches(caches);
   }

   private static List<CentralProcessor.LogicalProcessor> parseTopology() {
      String[] topology = BsdSysctlUtil.sysctl("kern.sched.topology_spec", "").split("[\\n\\r]");
      long group1 = 1L;
      List<Long> group2 = new ArrayList<>();
      List<Long> group3 = new ArrayList<>();
      int groupLevel = 0;

      for (String topo : topology) {
         if (topo.contains("<group level=")) {
            groupLevel++;
         } else if (topo.contains("</group>")) {
            groupLevel--;
         } else if (topo.contains("<cpu")) {
            Matcher m = CPUMASK.matcher(topo);
            if (m.matches()) {
               switch (groupLevel) {
                  case 1:
                     group1 = Long.parseLong(m.group(1), 16);
                     break;
                  case 2:
                     group2.add(Long.parseLong(m.group(1), 16));
                     break;
                  case 3:
                     group3.add(Long.parseLong(m.group(1), 16));
               }
            }
         }
      }

      return matchBitmasks(group1, group2, group3);
   }

   private static List<CentralProcessor.LogicalProcessor> matchBitmasks(long group1, List<Long> group2, List<Long> group3) {
      List<CentralProcessor.LogicalProcessor> logProcs = new ArrayList<>();
      int lowBit = Long.numberOfTrailingZeros(group1);
      int hiBit = 63 - Long.numberOfLeadingZeros(group1);

      for (int i = lowBit; i <= hiBit; i++) {
         if ((group1 & 1L << i) > 0L) {
            int numaNode = 0;
            CentralProcessor.LogicalProcessor logProc = new CentralProcessor.LogicalProcessor(
               i, getMatchingBitmask(group3, i), getMatchingBitmask(group2, i), numaNode
            );
            logProcs.add(logProc);
         }
      }

      return logProcs;
   }

   private static int getMatchingBitmask(List<Long> bitmasks, int lp) {
      for (int j = 0; j < bitmasks.size(); j++) {
         if ((bitmasks.get(j) & 1L << lp) != 0L) {
            return j;
         }
      }

      return 0;
   }

   @Override
   public long[] querySystemCpuLoadTicks() {
      long[] ticks = new long[CentralProcessor.TickType.values().length];
      FreeBsdLibc.CpTime cpTime = new FreeBsdLibc.CpTime();

      try {
         BsdSysctlUtil.sysctl("kern.cp_time", cpTime);
         ticks[CentralProcessor.TickType.USER.getIndex()] = cpTime.cpu_ticks[0];
         ticks[CentralProcessor.TickType.NICE.getIndex()] = cpTime.cpu_ticks[1];
         ticks[CentralProcessor.TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[2];
         ticks[CentralProcessor.TickType.IRQ.getIndex()] = cpTime.cpu_ticks[3];
         ticks[CentralProcessor.TickType.IDLE.getIndex()] = cpTime.cpu_ticks[4];
      } catch (Throwable var6) {
         try {
            cpTime.close();
         } catch (Throwable var5) {
            var6.addSuppressed(var5);
         }

         throw var6;
      }

      cpTime.close();
      return ticks;
   }

   @Override
   public long[] queryCurrentFreq() {
      long[] freq = new long[]{BsdSysctlUtil.sysctl("dev.cpu.0.freq", -1L)};
      if (freq[0] > 0L) {
         freq[0] *= 1000000L;
      } else {
         freq[0] = BsdSysctlUtil.sysctl("machdep.tsc_freq", -1L);
      }

      return freq;
   }

   @Override
   public long queryMaxFreq() {
      long max = -1L;
      String freqLevels = BsdSysctlUtil.sysctl("dev.cpu.0.freq_levels", "");

      for (String s : ParseUtil.whitespaces.split(freqLevels)) {
         long freq = ParseUtil.parseLongOrDefault(s.split("/")[0], -1L);
         if (max < freq) {
            max = freq;
         }
      }

      if (max > 0L) {
         max *= 1000000L;
      } else {
         max = BsdSysctlUtil.sysctl("machdep.tsc_freq", -1L);
      }

      return max;
   }

   @Override
   public double[] getSystemLoadAverage(int nelem) {
      if (nelem >= 1 && nelem <= 3) {
         double[] average = new double[nelem];
         int retval = FreeBsdLibc.INSTANCE.getloadavg(average, nelem);
         if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
               average[i] = -1.0;
            }
         }

         return average;
      } else {
         throw new IllegalArgumentException("Must include from one to three elements.");
      }
   }

   @Override
   public long[][] queryProcessorCpuLoadTicks() {
      long[][] ticks = new long[this.getLogicalProcessorCount()][CentralProcessor.TickType.values().length];
      long arraySize = CPTIME_SIZE * this.getLogicalProcessorCount();
      Memory p = new Memory(arraySize);

      long[][] cpu;
      label56: {
         try {
            ByRef.CloseableSizeTByReference oldlenp = new ByRef.CloseableSizeTByReference(arraySize);

            label52: {
               try {
                  String name = "kern.cp_times";
                  if (0 == FreeBsdLibc.INSTANCE.sysctlbyname(name, p, oldlenp, null, LibCAPI.size_t.ZERO)) {
                     int cpux = 0;

                     while (true) {
                        if (cpux >= this.getLogicalProcessorCount()) {
                           break label52;
                        }

                        ticks[cpux][CentralProcessor.TickType.USER.getIndex()] = p.getLong(CPTIME_SIZE * cpux + 0 * FreeBsdLibc.UINT64_SIZE);
                        ticks[cpux][CentralProcessor.TickType.NICE.getIndex()] = p.getLong(CPTIME_SIZE * cpux + 1 * FreeBsdLibc.UINT64_SIZE);
                        ticks[cpux][CentralProcessor.TickType.SYSTEM.getIndex()] = p.getLong(CPTIME_SIZE * cpux + 2 * FreeBsdLibc.UINT64_SIZE);
                        ticks[cpux][CentralProcessor.TickType.IRQ.getIndex()] = p.getLong(CPTIME_SIZE * cpux + 3 * FreeBsdLibc.UINT64_SIZE);
                        ticks[cpux][CentralProcessor.TickType.IDLE.getIndex()] = p.getLong(CPTIME_SIZE * cpux + 4 * FreeBsdLibc.UINT64_SIZE);
                        cpux++;
                     }
                  }

                  LOG.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                  cpu = ticks;
               } catch (Throwable var10) {
                  try {
                     oldlenp.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }

                  throw var10;
               }

               oldlenp.close();
               break label56;
            }

            oldlenp.close();
         } catch (Throwable var11) {
            try {
               p.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         p.close();
         return ticks;
      }

      p.close();
      return cpu;
   }

   private static String getProcessorIDfromDmiDecode(long processorID) {
      boolean procInfo = false;
      String marker = "Processor Information";

      for (String checkLine : ExecutingCommand.runNative("dmidecode -t system")) {
         if (!procInfo && checkLine.contains(marker)) {
            marker = "ID:";
            procInfo = true;
         } else if (procInfo && checkLine.contains(marker)) {
            return checkLine.split(marker)[1].trim();
         }
      }

      return String.format("%016X", processorID);
   }

   @Override
   public long queryContextSwitches() {
      String name = "vm.stats.sys.v_swtch";
      LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(new LibCAPI.size_t(FreeBsdLibc.INT_SIZE));
      Memory p = new Memory(size.longValue());

      long var8;
      label29: {
         try {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
               var8 = 0L;
               break label29;
            }

            var8 = ParseUtil.unsignedIntToLong(p.getInt(0L));
         } catch (Throwable var7) {
            try {
               p.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         p.close();
         return var8;
      }

      p.close();
      return var8;
   }

   @Override
   public long queryInterrupts() {
      String name = "vm.stats.sys.v_intr";
      LibCAPI.size_t.ByReference size = new LibCAPI.size_t.ByReference(new LibCAPI.size_t(FreeBsdLibc.INT_SIZE));
      Memory p = new Memory(size.longValue());

      long var8;
      label29: {
         try {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
               var8 = 0L;
               break label29;
            }

            var8 = ParseUtil.unsignedIntToLong(p.getInt(0L));
         } catch (Throwable var7) {
            try {
               p.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         p.close();
         return var8;
      }

      p.close();
      return var8;
   }

   static {
      FreeBsdLibc.CpTime cpTime = new FreeBsdLibc.CpTime();

      try {
         CPTIME_SIZE = cpTime.size();
      } catch (Throwable var4) {
         try {
            cpTime.close();
         } catch (Throwable var3) {
            var4.addSuppressed(var3);
         }

         throw var4;
      }

      cpTime.close();
   }
}
