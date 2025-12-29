package oshi.util.platform.windows;

import com.sun.jna.platform.win32.PdhUtil;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class PerfCounterQuery {
   private static final Logger LOG = LoggerFactory.getLogger(PerfCounterQuery.class);
   private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
   private static final Set<String> FAILED_QUERY_CACHE = ConcurrentHashMap.newKeySet();
   private static final ConcurrentHashMap<String, String> LOCALIZE_CACHE = new ConcurrentHashMap<>();
   public static final String TOTAL_INSTANCE = "_Total";
   public static final String TOTAL_OR_IDLE_INSTANCES = "_Total|Idle";
   public static final String TOTAL_INSTANCES = "*_Total";
   public static final String NOT_TOTAL_INSTANCE = "^_Total";
   public static final String NOT_TOTAL_INSTANCES = "^*_Total";

   private PerfCounterQuery() {
   }

   public static <T extends Enum<T>> Map<T, Long> queryValues(Class<T> propertyEnum, String perfObject, String perfWmiClass) {
      if (!FAILED_QUERY_CACHE.contains(perfObject)) {
         Map<T, Long> valueMap = queryValuesFromPDH(propertyEnum, perfObject);
         if (!valueMap.isEmpty()) {
            return valueMap;
         }

         LOG.warn("Disabling further attempts to query {}.", perfObject);
         FAILED_QUERY_CACHE.add(perfObject);
      }

      return queryValuesFromWMI(propertyEnum, perfWmiClass);
   }

   public static <T extends Enum<T>> Map<T, Long> queryValuesFromPDH(Class<T> propertyEnum, String perfObject) {
      T[] props = (T[])propertyEnum.getEnumConstants();
      String perfObjectLocalized = localizeIfNeeded(perfObject, false);
      EnumMap<T, PerfDataUtil.PerfCounter> counterMap = new EnumMap<>(propertyEnum);
      EnumMap<T, Long> valueMap = new EnumMap<>(propertyEnum);
      PerfCounterQueryHandler pdhQueryHandler = new PerfCounterQueryHandler();

      EnumMap var12;
      label48: {
         try {
            for (T prop : props) {
               PerfDataUtil.PerfCounter counter = PerfDataUtil.createCounter(
                  perfObjectLocalized, ((PerfCounterQuery.PdhCounterProperty)prop).getInstance(), ((PerfCounterQuery.PdhCounterProperty)prop).getCounter()
               );
               counterMap.put(prop, counter);
               if (!pdhQueryHandler.addCounterToQuery(counter)) {
                  var12 = valueMap;
                  break label48;
               }
            }

            if (0L < pdhQueryHandler.updateQuery()) {
               for (T propx : props) {
                  valueMap.put(propx, pdhQueryHandler.queryCounter(counterMap.get(propx)));
               }
            }
         } catch (Throwable var14) {
            try {
               pdhQueryHandler.close();
            } catch (Throwable var13) {
               var14.addSuppressed(var13);
            }

            throw var14;
         }

         pdhQueryHandler.close();
         return valueMap;
      }

      pdhQueryHandler.close();
      return var12;
   }

   public static <T extends Enum<T>> Map<T, Long> queryValuesFromWMI(Class<T> propertyEnum, String wmiClass) {
      WbemcliUtil.WmiQuery<T> query = new WbemcliUtil.WmiQuery<>(wmiClass, propertyEnum);
      WbemcliUtil.WmiResult<T> result = Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(query);
      EnumMap<T, Long> valueMap = new EnumMap<>(propertyEnum);
      if (result.getResultCount() > 0) {
         for (T prop : (Enum[])propertyEnum.getEnumConstants()) {
            switch (result.getCIMType(prop)) {
               case 18:
                  valueMap.put(prop, (long)WmiUtil.getUint16(result, prop, 0));
                  break;
               case 19:
                  valueMap.put(prop, WmiUtil.getUint32asLong(result, prop, 0));
                  break;
               case 21:
                  valueMap.put(prop, WmiUtil.getUint64(result, prop, 0));
                  break;
               case 101:
                  valueMap.put(prop, WmiUtil.getDateTime(result, prop, 0).toInstant().toEpochMilli());
                  break;
               default:
                  throw new ClassCastException("Unimplemented CIM Type Mapping.");
            }
         }
      }

      return valueMap;
   }

   public static String localizeIfNeeded(String perfObject, boolean force) {
      return !force && IS_VISTA_OR_GREATER ? perfObject : LOCALIZE_CACHE.computeIfAbsent(perfObject, PerfCounterQuery::localizeUsingPerfIndex);
   }

   private static String localizeUsingPerfIndex(String perfObject) {
      String localized = perfObject;

      try {
         localized = PdhUtil.PdhLookupPerfNameByIndex(null, PdhUtil.PdhLookupPerfIndexByEnglishName(perfObject));
      } catch (Win32Exception var3) {
         LOG.warn(
            "Unable to locate English counter names in registry Perflib 009. Assuming English counters. Error {}. {}",
            String.format("0x%x", var3.getHR().intValue()),
            "See https://support.microsoft.com/en-us/help/300956/how-to-manually-rebuild-performance-counter-library-values"
         );
      } catch (PdhUtil.PdhException var4) {
         LOG.warn("Unable to localize {} performance counter.  Error {}.", perfObject, String.format("0x%x", var4.getErrorCode()));
      }

      if (localized.isEmpty()) {
         return perfObject;
      } else {
         LOG.debug("Localized {} to {}", perfObject, localized);
         return localized;
      }
   }

   public interface PdhCounterProperty {
      String getInstance();

      String getCounter();
   }
}
