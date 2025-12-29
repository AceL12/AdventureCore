package oshi.software.os.mac;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.DiskArbitration;
import com.sun.jna.platform.mac.IOKit;
import com.sun.jna.platform.mac.IOKitUtil;
import com.sun.jna.platform.mac.SystemB;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractFileSystem;
import oshi.software.os.OSFileStore;
import oshi.util.FileSystemUtil;
import oshi.util.platform.mac.CFUtil;
import oshi.util.platform.mac.SysctlUtil;

@ThreadSafe
public class MacFileSystem extends AbstractFileSystem {
   private static final Logger LOG = LoggerFactory.getLogger(MacFileSystem.class);
   public static final String OSHI_MAC_FS_PATH_EXCLUDES = "oshi.os.mac.filesystem.path.excludes";
   public static final String OSHI_MAC_FS_PATH_INCLUDES = "oshi.os.mac.filesystem.path.includes";
   public static final String OSHI_MAC_FS_VOLUME_EXCLUDES = "oshi.os.mac.filesystem.volume.excludes";
   public static final String OSHI_MAC_FS_VOLUME_INCLUDES = "oshi.os.mac.filesystem.volume.includes";
   private static final List<PathMatcher> FS_PATH_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.mac.filesystem.path.excludes");
   private static final List<PathMatcher> FS_PATH_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.mac.filesystem.path.includes");
   private static final List<PathMatcher> FS_VOLUME_EXCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.mac.filesystem.volume.excludes");
   private static final List<PathMatcher> FS_VOLUME_INCLUDES = FileSystemUtil.loadAndParseFileSystemConfig("oshi.os.mac.filesystem.volume.includes");
   private static final Pattern LOCAL_DISK = Pattern.compile("/dev/disk\\d");
   private static final int MNT_RDONLY = 1;
   private static final int MNT_SYNCHRONOUS = 2;
   private static final int MNT_NOEXEC = 4;
   private static final int MNT_NOSUID = 8;
   private static final int MNT_NODEV = 16;
   private static final int MNT_UNION = 32;
   private static final int MNT_ASYNC = 64;
   private static final int MNT_CPROTECT = 128;
   private static final int MNT_EXPORTED = 256;
   private static final int MNT_QUARANTINE = 1024;
   private static final int MNT_LOCAL = 4096;
   private static final int MNT_QUOTA = 8192;
   private static final int MNT_ROOTFS = 16384;
   private static final int MNT_DOVOLFS = 32768;
   private static final int MNT_DONTBROWSE = 1048576;
   private static final int MNT_IGNORE_OWNERSHIP = 2097152;
   private static final int MNT_AUTOMOUNTED = 4194304;
   private static final int MNT_JOURNALED = 8388608;
   private static final int MNT_NOUSERXATTR = 16777216;
   private static final int MNT_DEFWRITE = 33554432;
   private static final int MNT_MULTILABEL = 67108864;
   private static final int MNT_NOATIME = 268435456;
   private static final Map<Integer, String> OPTIONS_MAP = new HashMap<>();

   @Override
   public List<OSFileStore> getFileStores(boolean localOnly) {
      return getFileStoreMatching(null, localOnly);
   }

   static List<OSFileStore> getFileStoreMatching(String nameToMatch) {
      return getFileStoreMatching(nameToMatch, false);
   }

   private static List<OSFileStore> getFileStoreMatching(String nameToMatch, boolean localOnly) {
      List<OSFileStore> fsList = new ArrayList<>();
      int numfs = SystemB.INSTANCE.getfsstat64(null, 0, 0);
      if (numfs > 0) {
         DiskArbitration.DASessionRef session = DiskArbitration.INSTANCE.DASessionCreate(CoreFoundation.INSTANCE.CFAllocatorGetDefault());
         if (session == null) {
            LOG.error("Unable to open session to DiskArbitration framework.");
         } else {
            CoreFoundation.CFStringRef daVolumeNameKey = CoreFoundation.CFStringRef.createCFString("DAVolumeName");
            SystemB.Statfs s = new SystemB.Statfs();
            SystemB.Statfs[] fs = (SystemB.Statfs[])s.toArray(numfs);
            numfs = SystemB.INSTANCE.getfsstat64(fs, fs[0].size() * fs.length, 16);

            for (int f = 0; f < numfs; f++) {
               String volume = Native.toString(fs[f].f_mntfromname, StandardCharsets.UTF_8);
               String path = Native.toString(fs[f].f_mntonname, StandardCharsets.UTF_8);
               String type = Native.toString(fs[f].f_fstypename, StandardCharsets.UTF_8);
               int flags = fs[f].f_flags;
               if ((!localOnly || (flags & 4096) != 0)
                  && (
                     path.equals("/")
                        || !PSEUDO_FS_TYPES.contains(type)
                           && !FileSystemUtil.isFileStoreExcluded(path, volume, FS_PATH_INCLUDES, FS_PATH_EXCLUDES, FS_VOLUME_INCLUDES, FS_VOLUME_EXCLUDES)
                  )) {
                  String description = "Volume";
                  if (LOCAL_DISK.matcher(volume).matches()) {
                     description = "Local Disk";
                  } else if (volume.startsWith("localhost:") || volume.startsWith("//") || volume.startsWith("smb://") || NETWORK_FS_TYPES.contains(type)) {
                     description = "Network Drive";
                  }

                  File file = new File(path);
                  String name = file.getName();
                  if (name.isEmpty()) {
                     name = file.getPath();
                  }

                  if (nameToMatch == null || nameToMatch.equals(name)) {
                     StringBuilder options = new StringBuilder((1 & flags) == 0 ? "rw" : "ro");
                     String moreOptions = OPTIONS_MAP.entrySet()
                        .stream()
                        .filter(e -> (e.getKey() & flags) > 0)
                        .map(Entry::getValue)
                        .collect(Collectors.joining(","));
                     if (!moreOptions.isEmpty()) {
                        options.append(',').append(moreOptions);
                     }

                     String uuid = "";
                     String bsdName = volume.replace("/dev/disk", "disk");
                     if (bsdName.startsWith("disk")) {
                        DiskArbitration.DADiskRef disk = DiskArbitration.INSTANCE
                           .DADiskCreateFromBSDName(CoreFoundation.INSTANCE.CFAllocatorGetDefault(), session, volume);
                        if (disk != null) {
                           CoreFoundation.CFDictionaryRef diskInfo = DiskArbitration.INSTANCE.DADiskCopyDescription(disk);
                           if (diskInfo != null) {
                              Pointer result = diskInfo.getValue(daVolumeNameKey);
                              name = CFUtil.cfPointerToString(result);
                              diskInfo.release();
                           }

                           disk.release();
                        }

                        CoreFoundation.CFMutableDictionaryRef matchingDict = IOKitUtil.getBSDNameMatchingDict(bsdName);
                        if (matchingDict != null) {
                           IOKit.IOIterator fsIter = IOKitUtil.getMatchingServices(matchingDict);
                           if (fsIter != null) {
                              IOKit.IORegistryEntry fsEntry = fsIter.next();
                              if (fsEntry != null && fsEntry.conformsTo("IOMedia")) {
                                 uuid = fsEntry.getStringProperty("UUID");
                                 if (uuid != null) {
                                    uuid = uuid.toLowerCase();
                                 }

                                 fsEntry.release();
                              }

                              fsIter.release();
                           }
                        }
                     }

                     fsList.add(
                        new MacOSFileStore(
                           name,
                           volume,
                           name,
                           path,
                           options.toString(),
                           uuid == null ? "" : uuid,
                           "",
                           description,
                           type,
                           file.getFreeSpace(),
                           file.getUsableSpace(),
                           file.getTotalSpace(),
                           fs[f].f_ffree,
                           fs[f].f_files
                        )
                     );
                  }
               }
            }

            daVolumeNameKey.release();
            session.release();
         }
      }

      return fsList;
   }

   @Override
   public long getOpenFileDescriptors() {
      return SysctlUtil.sysctl("kern.num_files", 0);
   }

   @Override
   public long getMaxFileDescriptors() {
      return SysctlUtil.sysctl("kern.maxfiles", 0);
   }

   @Override
   public long getMaxFileDescriptorsPerProcess() {
      return SysctlUtil.sysctl("kern.maxfilesperproc", 0);
   }

   static {
      OPTIONS_MAP.put(2, "synchronous");
      OPTIONS_MAP.put(4, "noexec");
      OPTIONS_MAP.put(8, "nosuid");
      OPTIONS_MAP.put(16, "nodev");
      OPTIONS_MAP.put(32, "union");
      OPTIONS_MAP.put(64, "asynchronous");
      OPTIONS_MAP.put(128, "content-protection");
      OPTIONS_MAP.put(256, "exported");
      OPTIONS_MAP.put(1024, "quarantined");
      OPTIONS_MAP.put(4096, "local");
      OPTIONS_MAP.put(8192, "quotas");
      OPTIONS_MAP.put(16384, "rootfs");
      OPTIONS_MAP.put(32768, "volfs");
      OPTIONS_MAP.put(1048576, "nobrowse");
      OPTIONS_MAP.put(2097152, "noowners");
      OPTIONS_MAP.put(4194304, "automounted");
      OPTIONS_MAP.put(8388608, "journaled");
      OPTIONS_MAP.put(16777216, "nouserxattr");
      OPTIONS_MAP.put(33554432, "defwrite");
      OPTIONS_MAP.put(67108864, "multilabel");
      OPTIONS_MAP.put(268435456, "noatime");
   }
}
