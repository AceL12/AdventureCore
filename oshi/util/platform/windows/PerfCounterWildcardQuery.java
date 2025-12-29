package oshi.util.platform.windows;

import com.sun.jna.platform.win32.PdhUtil;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Util;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class PerfCounterWildcardQuery {
   private static final Logger LOG = LoggerFactory.getLogger(PerfCounterWildcardQuery.class);
   private static final Set<String> FAILED_QUERY_CACHE = ConcurrentHashMap.newKeySet();

   private PerfCounterWildcardQuery() {
   }

   public static <T extends Enum<T>> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValues(
      Class<T> propertyEnum, String perfObject, String perfWmiClass
   ) {
      if (!FAILED_QUERY_CACHE.contains(perfObject)) {
         Pair<List<String>, Map<T, List<Long>>> instancesAndValuesMap = queryInstancesAndValuesFromPDH(propertyEnum, perfObject);
         if (!instancesAndValuesMap.getA().isEmpty()) {
            return instancesAndValuesMap;
         }

         LOG.warn("Disabling further attempts to query {}.", perfObject);
         FAILED_QUERY_CACHE.add(perfObject);
      }

      return queryInstancesAndValuesFromWMI(propertyEnum, perfWmiClass);
   }

   public static <T extends Enum<T>> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromPDH(Class<T> propertyEnum, String perfObject) {
      T[] props = (T[])propertyEnum.getEnumConstants();
      if (props.length < 2) {
         throw new IllegalArgumentException("Enum " + propertyEnum.getName() + " must have at least two elements, an instance filter and a counter.");
      } else {
         String instanceFilter = ((PerfCounterWildcardQuery.PdhCounterWildcardProperty)((Enum[])propertyEnum.getEnumConstants())[0]).getCounter().toLowerCase();
         String perfObjectLocalized = PerfCounterQuery.localizeIfNeeded(perfObject, true);
         PdhUtil.PdhEnumObjectItems objectItems = null;

         try {
            objectItems = PdhUtil.PdhEnumObjectItems(null, null, perfObjectLocalized, 100);
         } catch (PdhUtil.PdhException var18) {
            LOG.warn(
               "Failed to locate performance object for {} in the registry. Performance counters may be corrupt. {}", perfObjectLocalized, var18.getMessage()
            );
         }

         if (objectItems == null) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
         } else {
            List<String> instances = objectItems.getInstances();
            instances.removeIf(ix -> !Util.wildcardMatch(ix.toLowerCase(), instanceFilter));
            EnumMap<T, List<Long>> valuesMap = new EnumMap<>(propertyEnum);
            PerfCounterQueryHandler pdhQueryHandler = new PerfCounterQueryHandler();

            Pair var16;
            label77: {
               try {
                  EnumMap<T, List<PerfDataUtil.PerfCounter>> counterListMap = new EnumMap<>(propertyEnum);

                  for (int i = 1; i < props.length; i++) {
                     T prop = props[i];
                     List<PerfDataUtil.PerfCounter> counterList = new ArrayList<>(instances.size());

                     for (String instance : instances) {
                        PerfDataUtil.PerfCounter counter = PerfDataUtil.createCounter(
                           perfObject, instance, ((PerfCounterWildcardQuery.PdhCounterWildcardProperty)prop).getCounter()
                        );
                        if (!pdhQueryHandler.addCounterToQuery(counter)) {
                           var16 = new Pair<>(Collections.emptyList(), Collections.emptyMap());
                           break label77;
                        }

                        counterList.add(counter);
                     }

                     counterListMap.put(prop, counterList);
                  }

                  if (0L < pdhQueryHandler.updateQuery()) {
                     for (int i = 1; i < props.length; i++) {
                        T prop = props[i];
                        List<Long> values = new ArrayList<>();

                        for (PerfDataUtil.PerfCounter counter : counterListMap.get(prop)) {
                           values.add(pdhQueryHandler.queryCounter(counter));
                        }

                        valuesMap.put(prop, values);
                     }
                  }
               } catch (Throwable var19) {
                  try {
                     pdhQueryHandler.close();
                  } catch (Throwable var17) {
                     var19.addSuppressed(var17);
                  }

                  throw var19;
               }

               pdhQueryHandler.close();
               return new Pair<>(instances, valuesMap);
            }

            pdhQueryHandler.close();
            return var16;
         }
      }
   }

   public static <T extends Enum<T>> Pair<List<String>, Map<T, List<Long>>> queryInstancesAndValuesFromWMI(Class<T> propertyEnum, String wmiClass) {
      List<String> instances = new ArrayList<>();
      EnumMap<T, List<Long>> valuesMap = new EnumMap<>(propertyEnum);
      WbemcliUtil.WmiQuery<T> query = new WbemcliUtil.WmiQuery<>(wmiClass, propertyEnum);
      WbemcliUtil.WmiResult<T> result = Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(query);
      if (result.getResultCount() > 0) {
         for (T prop : (Enum[])propertyEnum.getEnumConstants()) {
            if (prop.ordinal() == 0) {
               for (int i = 0; i < result.getResultCount(); i++) {
                  instances.add(WmiUtil.getString(result, prop, i));
               }
            } else {
               List<Long> values = new ArrayList<>();

               for (int i = 0; i < result.getResultCount(); i++) {
                  switch (result.getCIMType(prop)) {
                     case 18:
                        values.add((long)WmiUtil.getUint16(result, prop, i));
                        break;
                     case 19:
                        values.add(WmiUtil.getUint32asLong(result, prop, i));
                        break;
                     case 21:
                        values.add(WmiUtil.getUint64(result, prop, i));
                        break;
                     case 101:
                        values.add(WmiUtil.getDateTime(result, prop, i).toInstant().toEpochMilli());
                        break;
                     default:
                        throw new ClassCastException("Unimplemented CIM Type Mapping.");
                  }
               }

               valuesMap.put(prop, values);
            }
         }
      }

      return new Pair<>(instances, valuesMap);
   }

   public interface PdhCounterWildcardProperty {
      String getCounter();
   }
}
