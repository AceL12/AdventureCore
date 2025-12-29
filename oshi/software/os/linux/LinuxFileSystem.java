package oshi.software.os.linux;

import com.sun.jna.Native;
import com.sun.jna.platform.linux.LibC;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileSystemUtil;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;

@ThreadSafe
public class LinuxFileSystem extends AbstractFileSystem {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxFileSystem.class);
   public static final String OSHI_LINUX_FS_PATH_EXCLUDES = "oshi.os.linux.filesystem.path.excludes";
   public static final String OSHI_LINUX_FS_PATH_INCLUDES = "oshi.os.linux.filesystem.path.includes";
   public static final String OSHI_LINUX_FS_VOLUME_EXCLUDES = "oshi.os.linux.filesystem.volume.excludes";
   public static final String OSHI_LINUX_FS_VOLUME_INCLUDES = "oshi.os.linux.filesystem.volume.includes";
   private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.linux.filesystem.path.excludes");
   private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.linux.filesystem.path.includes");
   private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.linux.filesystem.volume.excludes");
   private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.linux.filesystem.volume.includes");
   private static final String UNICODE_SPACE = "\\040";

   @Override
   public List<OSFileStore> getFileStores(boolean localOnly) {
      Map<String, String> volumeDeviceMap = new HashMap<>();
      File devMapper = new File("/dev/mapper");
      File[] volumes = devMapper.listFiles();
      if (volumes != null) {
         for (File volume : volumes) {
            try {
               volumeDeviceMap.put(volume.getCanonicalPath(), volume.getAbsolutePath());
            } catch (IOException var14) {
               LOG.error("Couldn't get canonical path for {}. {}", volume.getName(), var14.getMessage());
            }
         }
      }

      Map<String, String> uuidMap = new HashMap<>();
      File uuidDir = new File("/dev/disk/by-uuid");
      File[] uuids = uuidDir.listFiles();
      if (uuids != null) {
         for (File uuid : uuids) {
            try {
               String canonicalPath = uuid.getCanonicalPath();
               uuidMap.put(canonicalPath, uuid.getName().toLowerCase());
               if (volumeDeviceMap.containsKey(canonicalPath)) {
                  uuidMap.put(volumeDeviceMap.get(canonicalPath), uuid.getName().toLowerCase());
               }
            } catch (IOException var13) {
               LOG.error("Couldn't get canonical path for {}. {}", uuid.getName(), var13.getMessage());
            }
         }
      }

      return getFileStoreMatching(null, uuidMap, localOnly);
   }

   static List<OSFileStore> getFileStoreMatching(String nameToMatch, Map<String, String> uuidMap) {
      return getFileStoreMatching(nameToMatch, uuidMap, false);
   }

   private static List<OSFileStore> getFileStoreMatching(String nameToMatch, Map<String, String> uuidMap, boolean localOnly) {
      List<OSFileStore> fsList = new ArrayList<>();
      Map<String, String> labelMap = queryLabelMap();

      for (String mount : FileUtil.readFile(ProcPath.MOUNTS)) {
         String[] split = mount.split(" ");
         if (split.length >= 6) {
            String volume = split[0].replace("\\040", " ");
            String name = volume;
            String path = split[1].replace("\\040", " ");
            if (path.equals("/")) {
               name = "/";
            }

            String type = split[2];
            if ((!localOnly || !NETWORK_FS_TYPES.contains(type))
               && (
                  path.equals("/")
                     || !PSEUDO_FS_TYPES.contains(type)
                        && !FileSystemUtil.isFileStoreExcluded(path, volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES)
               )) {
               String options = split[3];
               if (nameToMatch == null || nameToMatch.equals(name)) {
                  String uuid = uuidMap != null ? uuidMap.getOrDefault(split[0], "") : "";
                  String description;
                  if (volume.startsWith("/dev")) {
                     description = "Local Disk";
                  } else if (volume.equals("tmpfs")) {
                     description = "Ram Disk";
                  } else if (NETWORK_FS_TYPES.contains(type)) {
                     description = "Network Disk";
                  } else {
                     description = "Mount Point";
                  }

                  String logicalVolume = "";
                  String volumeMapperDirectory = "/dev/mapper/";
                  Path link = Paths.get(volume);
                  if (link.toFile().exists() && Files.isSymbolicLink(link)) {
                     try {
                        Path slink = Files.readSymbolicLink(link);
                        Path full = Paths.get(volumeMapperDirectory + slink.toString());
                        if (full.toFile().exists()) {
                           logicalVolume = full.normalize().toString();
                        }
                     } catch (IOException var31) {
                        LOG.warn("Couldn't access symbolic path  {}. {}", link, var31.getMessage());
                     }
                  }

                  long totalInodes = 0L;
                  long freeInodes = 0L;
                  long totalSpace = 0L;
                  long usableSpace = 0L;
                  long freeSpace = 0L;

                  try {
                     LibC.Statvfs vfsStat = new LibC.Statvfs();
                     if (0 == LibC.INSTANCE.statvfs(path, vfsStat)) {
                        totalInodes = vfsStat.f_files.longValue();
                        freeInodes = vfsStat.f_ffree.longValue();
                        totalSpace = vfsStat.f_blocks.longValue() * vfsStat.f_frsize.longValue();
                        usableSpace = vfsStat.f_bavail.longValue() * vfsStat.f_frsize.longValue();
                        freeSpace = vfsStat.f_bfree.longValue() * vfsStat.f_frsize.longValue();
                     } else {
                        LOG.warn("Failed to get information to use statvfs. path: {}, Error code: {}", path, Native.getLastError());
                     }
                  } catch (NoClassDefFoundError | UnsatisfiedLinkError var30) {
                     LOG.error("Failed to get file counts from statvfs. {}", var30.getMessage());
                  }

                  if (totalSpace == 0L) {
                     File tmpFile = new File(path);
                     totalSpace = tmpFile.getTotalSpace();
                     usableSpace = tmpFile.getUsableSpace();
                     freeSpace = tmpFile.getFreeSpace();
                  }

                  fsList.add(
                     new LinuxOSFileStore(
                        name,
                        volume,
                        labelMap.getOrDefault(path, name),
                        path,
                        options,
                        uuid,
                        logicalVolume,
                        description,
                        type,
                        freeSpace,
                        usableSpace,
                        totalSpace,
                        freeInodes,
                        totalInodes
                     )
                  );
               }
            }
         }
      }

      return fsList;
   }

   private static Map<String, String> queryLabelMap() {
      Map<String, String> labelMap = new HashMap<>();

      for (String line : ExecutingCommand.runNative("lsblk -o mountpoint,label")) {
         String[] split = ParseUtil.whitespaces.split(line, 2);
         if (split.length == 2) {
            labelMap.put(split[0], split[1]);
         }
      }

      return labelMap;
   }

   @Override
   public long getOpenFileDescriptors() {
      return getFileDescriptors(0);
   }

   @Override
   public long getMaxFileDescriptors() {
      return getFileDescriptors(2);
   }

   @Override
   public long getMaxFileDescriptorsPerProcess() {
      return getFileDescriptorsPerProcess();
   }

   private static long getFileDescriptors(int index) {
      String filename = ProcPath.SYS_FS_FILE_NR;
      if (index >= 0 && index <= 2) {
         List<String> osDescriptors = FileUtil.readFile(filename);
         if (!osDescriptors.isEmpty()) {
            String[] splittedLine = osDescriptors.get(0).split("\\D+");
            return ParseUtil.parseLongOrDefault(splittedLine[index], 0L);
         } else {
            return 0L;
         }
      } else {
         throw new IllegalArgumentException("Index must be between 0 and 2.");
      }
   }

   private static long getFileDescriptorsPerProcess() {
      return FileUtil.getLongFromFile(ProcPath.SYS_FS_FILE_MAX);
   }
}
