package oshi.software.os.unix.freebsd;

import java.io.File;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.linux.LinuxOSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

@ThreadSafe
public final class FreeBsdFileSystem extends AbstractFileSystem {
   public static final String OSHI_FREEBSD_FS_PATH_EXCLUDES = "oshi.os.freebsd.filesystem.path.excludes";
   public static final String OSHI_FREEBSD_FS_PATH_INCLUDES = "oshi.os.freebsd.filesystem.path.includes";
   public static final String OSHI_FREEBSD_FS_VOLUME_EXCLUDES = "oshi.os.freebsd.filesystem.volume.excludes";
   public static final String OSHI_FREEBSD_FS_VOLUME_INCLUDES = "oshi.os.freebsd.filesystem.volume.includes";
   private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.freebsd.filesystem.path.excludes");
   private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.freebsd.filesystem.path.includes");
   private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.freebsd.filesystem.volume.excludes");
   private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.freebsd.filesystem.volume.includes");

   @Override
   public List<OSFileStore> getFileStores(boolean localOnly) {
      Map<String, String> uuidMap = new HashMap<>();
      String device = "";

      for (String line : ExecutingCommand.runNative("geom part list")) {
         if (line.contains("Name: ")) {
            device = line.substring(line.lastIndexOf(32) + 1);
         }

         if (!device.isEmpty()) {
            line = line.trim();
            if (line.startsWith("rawuuid:")) {
               uuidMap.put(device, line.substring(line.lastIndexOf(32) + 1));
               device = "";
            }
         }
      }

      List<OSFileStore> fsList = new ArrayList<>();
      Map<String, Long> inodeFreeMap = new HashMap<>();
      Map<String, Long> inodeTotalMap = new HashMap<>();

      for (String line : ExecutingCommand.runNative("df -i")) {
         if (line.startsWith("/")) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 7) {
               inodeFreeMap.put(split[0], ParseUtil.parseLongOrDefault(split[6], 0L));
               inodeTotalMap.put(split[0], inodeFreeMap.get(split[0]) + ParseUtil.parseLongOrDefault(split[5], 0L));
            }
         }
      }

      for (String fs : ExecutingCommand.runNative("mount -p")) {
         String[] split = ParseUtil.whitespaces.split(fs);
         if (split.length >= 5) {
            String volume = split[0];
            String path = split[1];
            String type = split[2];
            String options = split[3];
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

               File f = new File(path);
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

               String uuid = uuidMap.getOrDefault(name, "");
               fsList.add(
                  new LinuxOSFileStore(
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
                     inodeFreeMap.containsKey(path) ? inodeFreeMap.get(path) : 0L,
                     inodeTotalMap.containsKey(path) ? inodeTotalMap.get(path) : 0L
                  )
               );
            }
         }
      }

      return fsList;
   }

   @Override
   public long getOpenFileDescriptors() {
      return BsdSysctlUtil.sysctl("kern.openfiles", 0);
   }

   @Override
   public long getMaxFileDescriptors() {
      return BsdSysctlUtil.sysctl("kern.maxfiles", 0);
   }

   @Override
   public long getMaxFileDescriptorsPerProcess() {
      return this.getMaxFileDescriptors();
   }
}
