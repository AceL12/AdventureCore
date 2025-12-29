package oshi.hardware;

import java.util.Map;
import java.util.Set;
import oshi.annotation.concurrent.Immutable;

@Immutable
public interface LogicalVolumeGroup {
   String getName();

   Set<String> getPhysicalVolumes();

   Map<String, Set<String>> getLogicalVolumes();
}
