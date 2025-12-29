package oshi.hardware.common;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import oshi.hardware.LogicalVolumeGroup;

public class AbstractLogicalVolumeGroup implements LogicalVolumeGroup {
   private final String name;
   private final Map<String, Set<String>> lvMap;
   private final Set<String> pvSet;

   protected AbstractLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
      this.name = name;

      for (Entry<String, Set<String>> entry : lvMap.entrySet()) {
         lvMap.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
      }

      this.lvMap = Collections.unmodifiableMap(lvMap);
      this.pvSet = Collections.unmodifiableSet(pvSet);
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public Map<String, Set<String>> getLogicalVolumes() {
      return this.lvMap;
   }

   @Override
   public Set<String> getPhysicalVolumes() {
      return this.pvSet;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("Logical Volume Group: ");
      sb.append(this.name).append("\n |-- PVs: ");
      sb.append(this.pvSet.toString());

      for (Entry<String, Set<String>> entry : this.lvMap.entrySet()) {
         sb.append("\n |-- LV: ").append(entry.getKey());
         Set<String> mappedPVs = entry.getValue();
         if (!mappedPVs.isEmpty()) {
            sb.append(" --> ").append(mappedPVs);
         }
      }

      return sb.toString();
   }
}
