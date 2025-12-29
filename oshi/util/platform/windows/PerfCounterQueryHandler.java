package oshi.util.platform.windows;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.NotThreadSafe;
import oshi.jna.ByRef;
import oshi.util.FormatUtil;

@NotThreadSafe
public final class PerfCounterQueryHandler implements AutoCloseable {
   private static final Logger LOG = LoggerFactory.getLogger(PerfCounterQueryHandler.class);
   private Map<PerfDataUtil.PerfCounter, ByRef.CloseableHANDLEByReference> counterHandleMap = new HashMap<>();
   private ByRef.CloseableHANDLEByReference queryHandle = null;

   public boolean addCounterToQuery(PerfDataUtil.PerfCounter counter) {
      if (this.queryHandle == null) {
         this.queryHandle = new ByRef.CloseableHANDLEByReference();
         if (!PerfDataUtil.openQuery(this.queryHandle)) {
            LOG.warn("Failed to open a query for PDH counter: {}", counter.getCounterPath());
            this.queryHandle.close();
            this.queryHandle = null;
            return false;
         }
      }

      ByRef.CloseableHANDLEByReference p = new ByRef.CloseableHANDLEByReference();
      if (!PerfDataUtil.addCounter(this.queryHandle, counter.getCounterPath(), p)) {
         LOG.warn("Failed to add counter for PDH counter: {}", counter.getCounterPath());
         p.close();
         return false;
      } else {
         this.counterHandleMap.put(counter, p);
         return true;
      }
   }

   public boolean removeCounterFromQuery(PerfDataUtil.PerfCounter counter) {
      boolean success = false;
      ByRef.CloseableHANDLEByReference href = this.counterHandleMap.remove(counter);

      try {
         if (href != null) {
            success = PerfDataUtil.removeCounter(href);
         }
      } catch (Throwable var7) {
         if (href != null) {
            try {
               href.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (href != null) {
         href.close();
      }

      if (this.counterHandleMap.isEmpty()) {
         PerfDataUtil.closeQuery(this.queryHandle);
         this.queryHandle.close();
         this.queryHandle = null;
      }

      return success;
   }

   public void removeAllCounters() {
      for (ByRef.CloseableHANDLEByReference href : this.counterHandleMap.values()) {
         PerfDataUtil.removeCounter(href);
         href.close();
      }

      this.counterHandleMap.clear();
      if (this.queryHandle != null) {
         PerfDataUtil.closeQuery(this.queryHandle);
         this.queryHandle.close();
         this.queryHandle = null;
      }
   }

   public long updateQuery() {
      if (this.queryHandle == null) {
         LOG.warn("Query does not exist to update.");
         return 0L;
      } else {
         return PerfDataUtil.updateQueryTimestamp(this.queryHandle);
      }
   }

   public long queryCounter(PerfDataUtil.PerfCounter counter) {
      if (!this.counterHandleMap.containsKey(counter)) {
         if (LOG.isWarnEnabled()) {
            LOG.warn("Counter {} does not exist to query.", counter.getCounterPath());
         }

         return 0L;
      } else {
         long value = counter.isBaseCounter()
            ? PerfDataUtil.querySecondCounter(this.counterHandleMap.get(counter))
            : PerfDataUtil.queryCounter(this.counterHandleMap.get(counter));
         if (value < 0L) {
            if (LOG.isWarnEnabled()) {
               LOG.warn("Error querying counter {}: {}", counter.getCounterPath(), String.format(FormatUtil.formatError((int)value)));
            }

            return 0L;
         } else {
            return value;
         }
      }
   }

   @Override
   public void close() {
      this.removeAllCounters();
   }
}
