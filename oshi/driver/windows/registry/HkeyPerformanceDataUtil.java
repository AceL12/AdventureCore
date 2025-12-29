package oshi.driver.windows.registry;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinPerf;
import com.sun.jna.platform.win32.WinReg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class HkeyPerformanceDataUtil {
   private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceDataUtil.class);
   private static final String HKEY_PERFORMANCE_TEXT = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
   private static final String COUNTER = "Counter";
   private static final Map<String, Integer> COUNTER_INDEX_MAP = mapCounterIndicesFromRegistry();
   private static int maxPerfBufferSize = 16384;

   private HkeyPerformanceDataUtil() {
   }

   public static <T extends Enum<T> & PerfCounterWildcardQuery.PdhCounterWildcardProperty> Triplet<List<Map<T, Object>>, Long, Long> readPerfDataFromRegistry(
      String objectName, Class<T> counterEnum
   ) {
      Pair<Integer, EnumMap<T, Integer>> indices = getCounterIndices(objectName, counterEnum);
      if (indices == null) {
         return null;
      } else {
         Memory pPerfData = readPerfDataBuffer(objectName);

         Object var33;
         label106: {
            Object var30;
            label107: {
               Triplet var35;
               label108: {
                  try {
                     if (pPerfData == null) {
                        var33 = null;
                        break label106;
                     }

                     WinPerf.PERF_DATA_BLOCK perfData = new WinPerf.PERF_DATA_BLOCK(pPerfData.share(0L));
                     long perfTime100nSec = perfData.PerfTime100nSec.getValue();
                     long now = WinBase.FILETIME.filetimeToDate((int)(perfTime100nSec >> 32), (int)(perfTime100nSec & 4294967295L)).getTime();
                     long perfObjectOffset = perfData.HeaderLength;

                     for (int obj = 0; obj < perfData.NumObjectTypes; obj++) {
                        WinPerf.PERF_OBJECT_TYPE perfObject = new WinPerf.PERF_OBJECT_TYPE(pPerfData.share(perfObjectOffset));
                        if (perfObject.ObjectNameTitleIndex == COUNTER_INDEX_MAP.get(objectName)) {
                           long perfCounterOffset = perfObjectOffset + perfObject.HeaderLength;
                           Map<Integer, Integer> counterOffsetMap = new HashMap<>();
                           Map<Integer, Integer> counterSizeMap = new HashMap<>();

                           for (int counter = 0; counter < perfObject.NumCounters; counter++) {
                              WinPerf.PERF_COUNTER_DEFINITION perfCounter = new WinPerf.PERF_COUNTER_DEFINITION(pPerfData.share(perfCounterOffset));
                              counterOffsetMap.put(perfCounter.CounterNameTitleIndex, perfCounter.CounterOffset);
                              counterSizeMap.put(perfCounter.CounterNameTitleIndex, perfCounter.CounterSize);
                              perfCounterOffset += perfCounter.ByteLength;
                           }

                           long perfInstanceOffset = perfObjectOffset + perfObject.DefinitionLength;
                           List<Map<T, Object>> counterMaps = new ArrayList<>(perfObject.NumInstances);

                           for (int inst = 0; inst < perfObject.NumInstances; inst++) {
                              WinPerf.PERF_INSTANCE_DEFINITION perfInstance = new WinPerf.PERF_INSTANCE_DEFINITION(pPerfData.share(perfInstanceOffset));
                              long perfCounterBlockOffset = perfInstanceOffset + perfInstance.ByteLength;
                              Map<T, Object> counterMap = new EnumMap<>(counterEnum);
                              T[] counterKeys = (T[])counterEnum.getEnumConstants();
                              counterMap.put(counterKeys[0], pPerfData.getWideString(perfInstanceOffset + perfInstance.NameOffset));

                              for (int i = 1; i < counterKeys.length; i++) {
                                 T key = counterKeys[i];
                                 int keyIndex = COUNTER_INDEX_MAP.get(key.getCounter());
                                 int size = counterSizeMap.getOrDefault(keyIndex, 0);
                                 if (size == 4) {
                                    counterMap.put(key, pPerfData.getInt(perfCounterBlockOffset + counterOffsetMap.get(keyIndex).intValue()));
                                 } else {
                                    if (size != 8) {
                                       var30 = null;
                                       break label107;
                                    }

                                    counterMap.put(key, pPerfData.getLong(perfCounterBlockOffset + counterOffsetMap.get(keyIndex).intValue()));
                                 }
                              }

                              counterMaps.add(counterMap);
                              perfInstanceOffset = perfCounterBlockOffset
                                 + (new WinPerf.PERF_COUNTER_BLOCK(pPerfData.share(perfCounterBlockOffset))).ByteLength;
                           }

                           var35 = new Triplet<>(counterMaps, perfTime100nSec, now);
                           break label108;
                        }

                        perfObjectOffset += perfObject.TotalByteLength;
                     }
                  } catch (Throwable var32) {
                     if (pPerfData != null) {
                        try {
                           pPerfData.close();
                        } catch (Throwable var31) {
                           var32.addSuppressed(var31);
                        }
                     }

                     throw var32;
                  }

                  if (pPerfData != null) {
                     pPerfData.close();
                  }

                  return null;
               }

               if (pPerfData != null) {
                  pPerfData.close();
               }

               return var35;
            }

            if (pPerfData != null) {
               pPerfData.close();
            }

            return (Triplet<List<Map<T, Object>>, Long, Long>)var30;
         }

         if (pPerfData != null) {
            pPerfData.close();
         }

         return (Triplet<List<Map<T, Object>>, Long, Long>)var33;
      }
   }

   private static <T extends Enum<T> & PerfCounterWildcardQuery.PdhCounterWildcardProperty> Pair<Integer, EnumMap<T, Integer>> getCounterIndices(
      String objectName, Class<T> counterEnum
   ) {
      if (!COUNTER_INDEX_MAP.containsKey(objectName)) {
         LOG.debug("Couldn't find counter index of {}.", objectName);
         return null;
      } else {
         int counterIndex = COUNTER_INDEX_MAP.get(objectName);
         T[] enumConstants = (T[])counterEnum.getEnumConstants();
         EnumMap<T, Integer> indexMap = new EnumMap<>(counterEnum);

         for (int i = 1; i < enumConstants.length; i++) {
            T key = enumConstants[i];
            String counterName = key.getCounter();
            if (!COUNTER_INDEX_MAP.containsKey(counterName)) {
               LOG.debug("Couldn't find counter index of {}.", counterName);
               return null;
            }

            indexMap.put(key, COUNTER_INDEX_MAP.get(counterName));
         }

         return new Pair<>(counterIndex, indexMap);
      }
   }

   private static synchronized Memory readPerfDataBuffer(String objectName) {
      String objectIndexStr = Integer.toString(COUNTER_INDEX_MAP.get(objectName));
      ByRef.CloseableIntByReference lpcbData = new ByRef.CloseableIntByReference(maxPerfBufferSize);

      Memory var8;
      label36: {
         try {
            Memory pPerfData = new Memory(maxPerfBufferSize);
            int ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, objectIndexStr, 0, null, pPerfData, lpcbData);
            if (ret != 0 && ret != 234) {
               LOG.error("Error reading performance data from registry for {}.", objectName);
               pPerfData.close();
               var8 = null;
               break label36;
            }

            while (ret == 234) {
               maxPerfBufferSize += 8192;
               lpcbData.setValue(maxPerfBufferSize);
               pPerfData.close();
               pPerfData = new Memory(maxPerfBufferSize);
               ret = Advapi32.INSTANCE.RegQueryValueEx(WinReg.HKEY_PERFORMANCE_DATA, objectIndexStr, 0, null, pPerfData, lpcbData);
            }

            var8 = pPerfData;
         } catch (Throwable var7) {
            try {
               lpcbData.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         lpcbData.close();
         return var8;
      }

      lpcbData.close();
      return var8;
   }

   private static Map<String, Integer> mapCounterIndicesFromRegistry() {
      HashMap<String, Integer> indexMap = new HashMap<>();

      try {
         String[] counterText = Advapi32Util.registryGetStringArray(
            WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009", "Counter"
         );

         for (int i = 1; i < counterText.length; i += 2) {
            indexMap.putIfAbsent(counterText[i], Integer.parseInt(counterText[i - 1]));
         }
      } catch (Win32Exception var3) {
         LOG.error("Unable to locate English counter names in registry Perflib 009. Counters may need to be rebuilt: ", (Throwable)var3);
      } catch (NumberFormatException var4) {
         LOG.error("Unable to parse English counter names in registry Perflib 009.");
      }

      return Collections.unmodifiableMap(indexMap);
   }
}
