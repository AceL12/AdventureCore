package oshi.util.platform.windows;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;
import oshi.util.Util;

@ThreadSafe
public final class PerfDataUtil {
   private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtil.class);
   private static final BaseTSD.DWORD_PTR PZERO = new BaseTSD.DWORD_PTR(0L);
   private static final WinDef.DWORDByReference PDH_FMT_RAW = new WinDef.DWORDByReference(new WinDef.DWORD(16L));
   private static final Pdh PDH = Pdh.INSTANCE;
   private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

   private PerfDataUtil() {
   }

   public static PerfDataUtil.PerfCounter createCounter(String object, String instance, String counter) {
      return new PerfDataUtil.PerfCounter(object, instance, counter);
   }

   public static long updateQueryTimestamp(WinNT.HANDLEByReference query) {
      ByRef.CloseableLONGLONGByReference pllTimeStamp = new ByRef.CloseableLONGLONGByReference();

      long var8;
      label59: {
         try {
            int ret = IS_VISTA_OR_GREATER ? PDH.PdhCollectQueryDataWithTime(query.getValue(), pllTimeStamp) : PDH.PdhCollectQueryData(query.getValue());

            for (int retries = 0;
               ret == -2147481643 && retries++ < 3;
               ret = IS_VISTA_OR_GREATER ? PDH.PdhCollectQueryDataWithTime(query.getValue(), pllTimeStamp) : PDH.PdhCollectQueryData(query.getValue())
            ) {
               Util.sleep(1 << retries);
            }

            if (ret != 0) {
               if (LOG.isWarnEnabled()) {
                  LOG.warn("Failed to update counter. Error code: {}", String.format(FormatUtil.formatError(ret)));
               }

               var8 = 0L;
               break label59;
            }

            var8 = IS_VISTA_OR_GREATER ? ParseUtil.filetimeToUtcMs(pllTimeStamp.getValue().longValue(), true) : System.currentTimeMillis();
         } catch (Throwable var7) {
            try {
               pllTimeStamp.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         pllTimeStamp.close();
         return var8;
      }

      pllTimeStamp.close();
      return var8;
   }

   public static boolean openQuery(WinNT.HANDLEByReference q) {
      int ret = PDH.PdhOpenQuery(null, PZERO, q);
      if (ret != 0) {
         if (LOG.isErrorEnabled()) {
            LOG.error("Failed to open PDH Query. Error code: {}", String.format(FormatUtil.formatError(ret)));
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean closeQuery(WinNT.HANDLEByReference q) {
      return 0 == PDH.PdhCloseQuery(q.getValue());
   }

   public static long queryCounter(WinNT.HANDLEByReference counter) {
      Struct.CloseablePdhRawCounter counterValue = new Struct.CloseablePdhRawCounter();

      long var7;
      label33: {
         try {
            int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
            if (ret != 0) {
               if (LOG.isWarnEnabled()) {
                  LOG.warn("Failed to get counter. Error code: {}", String.format(FormatUtil.formatError(ret)));
               }

               var7 = ret;
               break label33;
            }

            var7 = counterValue.FirstValue;
         } catch (Throwable var6) {
            try {
               counterValue.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         counterValue.close();
         return var7;
      }

      counterValue.close();
      return var7;
   }

   public static long querySecondCounter(WinNT.HANDLEByReference counter) {
      Struct.CloseablePdhRawCounter counterValue = new Struct.CloseablePdhRawCounter();

      long var7;
      label33: {
         try {
            int ret = PDH.PdhGetRawCounterValue(counter.getValue(), PDH_FMT_RAW, counterValue);
            if (ret != 0) {
               if (LOG.isWarnEnabled()) {
                  LOG.warn("Failed to get counter. Error code: {}", String.format(FormatUtil.formatError(ret)));
               }

               var7 = ret;
               break label33;
            }

            var7 = counterValue.SecondValue;
         } catch (Throwable var6) {
            try {
               counterValue.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         counterValue.close();
         return var7;
      }

      counterValue.close();
      return var7;
   }

   public static boolean addCounter(WinNT.HANDLEByReference query, String path, WinNT.HANDLEByReference p) {
      int ret = IS_VISTA_OR_GREATER ? PDH.PdhAddEnglishCounter(query.getValue(), path, PZERO, p) : PDH.PdhAddCounter(query.getValue(), path, PZERO, p);
      if (ret != 0) {
         if (LOG.isWarnEnabled()) {
            LOG.warn("Failed to add PDH Counter: {}, Error code: {}", path, String.format(FormatUtil.formatError(ret)));
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean removeCounter(WinNT.HANDLEByReference p) {
      return 0 == PDH.PdhRemoveCounter(p.getValue());
   }

   @Immutable
   public static class PerfCounter {
      private final String object;
      private final String instance;
      private final String counter;
      private final boolean baseCounter;

      public PerfCounter(String objectName, String instanceName, String counterName) {
         this.object = objectName;
         this.instance = instanceName;
         int baseIdx = counterName.indexOf("_Base");
         if (baseIdx > 0) {
            this.counter = counterName.substring(0, baseIdx);
            this.baseCounter = true;
         } else {
            this.counter = counterName;
            this.baseCounter = false;
         }
      }

      public String getObject() {
         return this.object;
      }

      public String getInstance() {
         return this.instance;
      }

      public String getCounter() {
         return this.counter;
      }

      public boolean isBaseCounter() {
         return this.baseCounter;
      }

      public String getCounterPath() {
         StringBuilder sb = new StringBuilder();
         sb.append('\\').append(this.object);
         if (this.instance != null) {
            sb.append('(').append(this.instance).append(')');
         }

         sb.append('\\').append(this.counter);
         return sb.toString();
      }
   }
}
