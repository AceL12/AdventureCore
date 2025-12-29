package oshi.util;

import java.util.Properties;
import oshi.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class GlobalConfig {
   private static final String OSHI_PROPERTIES = "oshi.properties";
   private static final Properties CONFIG = FileUtil.readPropertiesFromFilename("oshi.properties");
   public static final String OSHI_UTIL_MEMOIZER_EXPIRATION = "oshi.util.memoizer.expiration";
   public static final String OSHI_UTIL_WMI_TIMEOUT = "oshi.util.wmi.timeout";
   public static final String OSHI_UTIL_PROC_PATH = "oshi.util.proc.path";
   public static final String OSHI_PSEUDO_FILESYSTEM_TYPES = "oshi.pseudo.filesystem.types";
   public static final String OSHI_NETWORK_FILESYSTEM_TYPES = "oshi.network.filesystem.types";
   public static final String OSHI_OS_LINUX_PROCFS_LOGWARNING = "oshi.os.linux.procfs.logwarning";
   public static final String OSHI_OS_MAC_SYSCTL_LOGWARNING = "oshi.os.mac.sysctl.logwarning";
   public static final String OSHI_OS_WINDOWS_EVENTLOG = "oshi.os.windows.eventlog";
   public static final String OSHI_OS_WINDOWS_PROCSTATE_SUSPENDED = "oshi.os.windows.procstate.suspended";
   public static final String OSHI_OS_WINDOWS_COMMANDLINE_BATCH = "oshi.os.windows.commandline.batch";
   public static final String OSHI_OS_WINDOWS_HKEYPERFDATA = "oshi.os.windows.hkeyperfdata";
   public static final String OSHI_OS_WINDOWS_LOADAVERAGE = "oshi.os.windows.loadaverage";
   public static final String OSHI_OS_WINDOWS_CPU_UTILITY = "oshi.os.windows.cpu.utility";
   public static final String OSHI_OS_WINDOWS_PERFDISK_DIABLED = "oshi.os.windows.perfdisk.disabled";
   public static final String OSHI_OS_WINDOWS_PERFOS_DIABLED = "oshi.os.windows.perfos.disabled";
   public static final String OSHI_OS_WINDOWS_PERFPROC_DIABLED = "oshi.os.windows.perfproc.disabled";
   public static final String OSHI_OS_UNIX_WHOCOMMAND = "oshi.os.unix.whoCommand";

   private GlobalConfig() {
   }

   public static String get(String key) {
      return CONFIG.getProperty(key);
   }

   public static String get(String key, String def) {
      return CONFIG.getProperty(key, def);
   }

   public static int get(String key, int def) {
      String value = CONFIG.getProperty(key);
      return value == null ? def : ParseUtil.parseIntOrDefault(value, def);
   }

   public static double get(String key, double def) {
      String value = CONFIG.getProperty(key);
      return value == null ? def : ParseUtil.parseDoubleOrDefault(value, def);
   }

   public static boolean get(String key, boolean def) {
      String value = CONFIG.getProperty(key);
      return value == null ? def : Boolean.parseBoolean(value);
   }

   public static void set(String key, Object val) {
      if (val == null) {
         CONFIG.remove(key);
      } else {
         CONFIG.setProperty(key, val.toString());
      }
   }

   public static void remove(String key) {
      CONFIG.remove(key);
   }

   public static void clear() {
      CONFIG.clear();
   }

   public static void load(Properties properties) {
      CONFIG.putAll(properties);
   }

   public static class PropertyException extends RuntimeException {
      private static final long serialVersionUID = -7482581936621748005L;

      public PropertyException(String property) {
         super("Invalid property: \"" + property + "\" = " + GlobalConfig.get(property, null));
      }

      public PropertyException(String property, String message) {
         super("Invalid property \"" + property + "\": " + message);
      }
   }
}
