package oshi.software.os.unix.aix;

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
import oshi.util.FileUtil;
import oshi.util.ParseUtil;

@ThreadSafe
public class AixFileSystem extends AbstractFileSystem {
   public static final String OSHI_AIX_FS_PATH_EXCLUDES = "oshi.os.aix.filesystem.path.excludes";
   public static final String OSHI_AIX_FS_PATH_INCLUDES = "oshi.os.aix.filesystem.path.includes";
   public static final String OSHI_AIX_FS_VOLUME_EXCLUDES = "oshi.os.aix.filesystem.volume.excludes";
   public static final String OSHI_AIX_FS_VOLUME_INCLUDES = "oshi.os.aix.filesystem.volume.includes";
   private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.aix.filesystem.path.excludes");
   private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.aix.filesystem.path.includes");
   private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.aix.filesystem.volume.excludes");
   private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.aix.filesystem.volume.includes");

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
      Map<String, Long> inodeTotalMap = new HashMap<>();
      String command = "df -i" + (localOnly ? " -l" : "");

      for (String line : ExecutingCommand.runNative(command)) {
         if (line.startsWith("/")) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 5) {
               inodeTotalMap.put(split[0], ParseUtil.parseLongOrDefault(split[1], 0L));
               inodeFreeMap.put(split[0], ParseUtil.parseLongOrDefault(split[3], 0L));
            }
         }
      }

      for (String fs : ExecutingCommand.runNative("mount")) {
         String[] split = ParseUtil.whitespaces.split("x" + fs);
         if (split.length > 7) {
            String volume = split[1];
            String path = split[2];
            String type = split[3];
            String options = split[4];
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
                  if (f.exists() && f.getTotalSpace() >= 0L) {
                     long totalSpace = f.getTotalSpace();
                     long usableSpace = f.getUsableSpace();
                     long freeSpace = f.getFreeSpace();
                     String description;
                     if (volume.startsWith("/dev") || path.equals("/")) {
                        description = "Local Disk";
                     } else if (volume.equals("tmpfs")) {
                        description = "Ram Disk";
                     } else if (NETWORK_FS_TYPES.contains(type)) {
                        description = "Network Disk";
                     } else {
                        description = "Mount Point";
                     }

                     fsList.add(
                        new AixOSFileStore(
                           name,
                           volume,
                           name,
                           path,
                           options,
                           "",
                           "",
                           description,
                           type,
                           freeSpace,
                           usableSpace,
                           totalSpace,
                           inodeFreeMap.getOrDefault(volume, 0L),
                           inodeTotalMap.getOrDefault(volume, 0L)
                        )
                     );
                  }
               }
            }
         }
      }

      return fsList;
   }

   @Override
   public long getOpenFileDescriptors() {
      boolean header = false;
      long openfiles = 0L;

      for (String f : ExecutingCommand.runNative("lsof -nl")) {
         if (!header) {
            header = f.startsWith("COMMAND");
         } else {
            openfiles++;
         }
      }

      return openfiles;
   }

   @Override
   public long getMaxFileDescriptors() {
      return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("ulimit -n"), 0L);
   }

   @Override
   public long getMaxFileDescriptorsPerProcess() {
      for (String line : FileUtil.readFile("/etc/security/limits")) {
         if (line.trim().startsWith("nofiles")) {
            return ParseUtil.parseLastLong(line, Long.MAX_VALUE);
         }
      }

      return Long.MAX_VALUE;
   }
}
