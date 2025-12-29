package oshi.driver.windows.registry;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Wtsapi32;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32Process;
import oshi.jna.ByRef;
import oshi.util.platform.windows.WmiUtil;

@ThreadSafe
public final class ProcessWtsData {
   private static final Logger LOG = LoggerFactory.getLogger(ProcessWtsData.class);
   private static final boolean IS_WINDOWS7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

   private ProcessWtsData() {
   }

   public static Map<Integer, ProcessWtsData.WtsInfo> queryProcessWtsMap(Collection<Integer> pids) {
      return IS_WINDOWS7_OR_GREATER ? queryProcessWtsMapFromWTS(pids) : queryProcessWtsMapFromPerfMon(pids);
   }

   private static Map<Integer, ProcessWtsData.WtsInfo> queryProcessWtsMapFromWTS(Collection<Integer> pids) {
      Map<Integer, ProcessWtsData.WtsInfo> wtsMap = new HashMap<>();
      ByRef.CloseableIntByReference pCount = new ByRef.CloseableIntByReference(0);

      Object var18;
      label90: {
         try {
            ByRef.CloseablePointerByReference ppProcessInfo;
            label94: {
               ppProcessInfo = new ByRef.CloseablePointerByReference();

               try {
                  ByRef.CloseableIntByReference infoLevel1 = new ByRef.CloseableIntByReference(1);

                  label82: {
                     try {
                        if (!Wtsapi32.INSTANCE.WTSEnumerateProcessesEx(Wtsapi32.WTS_CURRENT_SERVER_HANDLE, infoLevel1, -2, ppProcessInfo, pCount)) {
                           LOG.error("Failed to enumerate Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
                           var18 = wtsMap;
                           break label82;
                        }

                        Pointer pProcessInfo = ppProcessInfo.getValue();
                        Wtsapi32.WTS_PROCESS_INFO_EX processInfoRef = new Wtsapi32.WTS_PROCESS_INFO_EX(pProcessInfo);
                        Wtsapi32.WTS_PROCESS_INFO_EX[] processInfo = (Wtsapi32.WTS_PROCESS_INFO_EX[])processInfoRef.toArray(pCount.getValue());

                        for (Wtsapi32.WTS_PROCESS_INFO_EX info : processInfo) {
                           if (pids == null || pids.contains(info.ProcessId)) {
                              wtsMap.put(
                                 info.ProcessId,
                                 new ProcessWtsData.WtsInfo(
                                    info.pProcessName,
                                    "",
                                    info.NumberOfThreads,
                                    info.PagefileUsage & 4294967295L,
                                    info.KernelTime.getValue() / 10000L,
                                    info.UserTime.getValue() / 10000L,
                                    info.HandleCount
                                 )
                              );
                           }
                        }

                        if (!Wtsapi32.INSTANCE.WTSFreeMemoryEx(1, pProcessInfo, pCount.getValue())) {
                           LOG.warn("Failed to Free Memory for Processes. Error code: {}", Kernel32.INSTANCE.GetLastError());
                        }
                     } catch (Throwable var15) {
                        try {
                           infoLevel1.close();
                        } catch (Throwable var14) {
                           var15.addSuppressed(var14);
                        }

                        throw var15;
                     }

                     infoLevel1.close();
                     break label94;
                  }

                  infoLevel1.close();
               } catch (Throwable var16) {
                  try {
                     ppProcessInfo.close();
                  } catch (Throwable var13) {
                     var16.addSuppressed(var13);
                  }

                  throw var16;
               }

               ppProcessInfo.close();
               break label90;
            }

            ppProcessInfo.close();
         } catch (Throwable var17) {
            try {
               pCount.close();
            } catch (Throwable var12) {
               var17.addSuppressed(var12);
            }

            throw var17;
         }

         pCount.close();
         return wtsMap;
      }

      pCount.close();
      return (Map<Integer, ProcessWtsData.WtsInfo>)var18;
   }

   private static Map<Integer, ProcessWtsData.WtsInfo> queryProcessWtsMapFromPerfMon(Collection<Integer> pids) {
      Map<Integer, ProcessWtsData.WtsInfo> wtsMap = new HashMap<>();
      WbemcliUtil.WmiResult<Win32Process.ProcessXPProperty> processWmiResult = Win32Process.queryProcesses(pids);

      for (int i = 0; i < processWmiResult.getResultCount(); i++) {
         wtsMap.put(
            WmiUtil.getUint32(processWmiResult, Win32Process.ProcessXPProperty.PROCESSID, i),
            new ProcessWtsData.WtsInfo(
               WmiUtil.getString(processWmiResult, Win32Process.ProcessXPProperty.NAME, i),
               WmiUtil.getString(processWmiResult, Win32Process.ProcessXPProperty.EXECUTABLEPATH, i),
               WmiUtil.getUint32(processWmiResult, Win32Process.ProcessXPProperty.THREADCOUNT, i),
               1024L * (WmiUtil.getUint32(processWmiResult, Win32Process.ProcessXPProperty.PAGEFILEUSAGE, i) & 4294967295L),
               WmiUtil.getUint64(processWmiResult, Win32Process.ProcessXPProperty.KERNELMODETIME, i) / 10000L,
               WmiUtil.getUint64(processWmiResult, Win32Process.ProcessXPProperty.USERMODETIME, i) / 10000L,
               WmiUtil.getUint32(processWmiResult, Win32Process.ProcessXPProperty.HANDLECOUNT, i)
            )
         );
      }

      return wtsMap;
   }

   @Immutable
   public static class WtsInfo {
      private final String name;
      private final String path;
      private final int threadCount;
      private final long virtualSize;
      private final long kernelTime;
      private final long userTime;
      private final long openFiles;

      public WtsInfo(String name, String path, int threadCount, long virtualSize, long kernelTime, long userTime, long openFiles) {
         this.name = name;
         this.path = path;
         this.threadCount = threadCount;
         this.virtualSize = virtualSize;
         this.kernelTime = kernelTime;
         this.userTime = userTime;
         this.openFiles = openFiles;
      }

      public String getName() {
         return this.name;
      }

      public String getPath() {
         return this.path;
      }

      public int getThreadCount() {
         return this.threadCount;
      }

      public long getVirtualSize() {
         return this.virtualSize;
      }

      public long getKernelTime() {
         return this.kernelTime;
      }

      public long getUserTime() {
         return this.userTime;
      }

      public long getOpenFiles() {
         return this.openFiles;
      }
   }
}
