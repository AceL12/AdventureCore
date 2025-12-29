package oshi.software.os.unix.openbsd;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

@ThreadSafe
public class OpenBsdFileSystem extends AbstractFileSystem {
   public static final String OSHI_OPENBSD_FS_PATH_EXCLUDES = "oshi.os.openbsd.filesystem.path.excludes";
   public static final String OSHI_OPENBSD_FS_PATH_INCLUDES = "oshi.os.openbsd.filesystem.path.includes";
   public static final String OSHI_OPENBSD_FS_VOLUME_EXCLUDES = "oshi.os.openbsd.filesystem.volume.excludes";
   public static final String OSHI_OPENBSD_FS_VOLUME_INCLUDES = "oshi.os.openbsd.filesystem.volume.includes";
   private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.openbsd.filesystem.path.excludes");
   private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.openbsd.filesystem.path.includes");
   private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.openbsd.filesystem.volume.excludes");
   private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.openbsd.filesystem.volume.includes");

   @Override
   public List<OSFileStore> getFileStores(boolean localOnly) {
      return getFileStoreMatching(null, localOnly);
   }

   static List<OSFileStore> getFileStoreMatching(String nameToMatch) {
      return getFileStoreMatching(nameToMatch, false);
   }

   private static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
      List<OSFileStore> fsList = new ArrayList<>();
      Map<String, Long> inodeFreeMap = new HashMap<>();
      Map<String, Long> inodeUsedlMap = new HashMap<>();
      String command = "df -i" + (localOnly ? " -l" : "");

      for (String line : ExecutingCommand.runNative(command)) {
         if (line.startsWith("/")) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 6) {
               inodeUsedlMap.put(split[0], ParseUtil.parseLongOrDefault(split[5], 0L));
               inodeFreeMap.put(split[0], ParseUtil.parseLongOrDefault(split[6], 0L));
            }
         }
      }

      for (String fs : ExecutingCommand.runNative("mount -v")) {
         String[] split = ParseUtil.whitespaces.split(fs, 7);
         if (split.length == 7) {
            String volume = split[0];
            String uuid = split[1];
            String path = split[3];
            String type = split[5];
            String options = split[6];
            if ((!localOnly || !NETWORK_FS_TYPES.contains(type))
               && (
                  path.equals("/")
                     || !PSEUDO_FS_TYPES.contains(type)
                        && !FileSystemUtil.isFileStoreExcluded(path, volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES)
               )) {
               String name = path.substring(path.lastIndexOf(47) + 1);
               if (name.isEmpty()) {
                  name = volume.substring(volume.lastIndexOf(47) + 1);
               }

               if (nameToMatch == null || nameToMatch.equals(name)) {
                  File f = new File(path);
                  long totalSpace = f.getTotalSpace();
                  long usableSpace = f.getUsableSpace();
                  long freeSpace = f.getFreeSpace();
                  String description;
                  if (volume.startsWith("/dev") || path.equals("/")) {
                     description = "Local Disk";
                  } else if (volume.equals("tmpfs")) {
                     description = "Ram Disk (dynamic)";
                  } else if (volume.equals("mfs")) {
                     description = "Ram Disk (fixed)";
                  } else if (NETWORK_FS_TYPES.contains(type)) {
                     description = "Network Disk";
                  } else {
                     description = "Mount Point";
                  }

                  fsList.add(
                     new OpenBsdOSFileStore(
                        name,
                        volume,
                        name,
                        path,
                        options,
                        uuid,
                        "",
                        description,
                        type,
                        freeSpace,
                        usableSpace,
                        totalSpace,
                        inodeFreeMap.getOrDefault(volume, 0L),
                        inodeUsedlMap.getOrDefault(volume, 0L) + inodeFreeMap.getOrDefault(volume, 0L)
                     )
                  );
               }
            }
         }
      }

      return fsList;
   }

   @Override
   public long getOpenFileDescriptors() {
      return OpenBsdSysctlUtil.sysctl("kern.nfiles", 0);
   }

   @Override
   public long getMaxFileDescriptors() {
      return OpenBsdSysctlUtil.sysctl("kern.maxfiles", 0);
   }

   @Override
   public long getMaxFileDescriptorsPerProcess() {
      return OpenBsdSysctlUtil.sysctl("kern.maxfilesperproc", 0);
   }
}
