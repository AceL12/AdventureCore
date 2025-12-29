package oshi.util;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class FileSystemUtil {
   private static final String GLOB_PREFIX = "glob:";
   private static final String REGEX_PREFIX = "regex:";

   private FileSystemUtil() {
   }

   public static boolean isFileStoreExcluded(
      String path,
      String volume,
      List<PathMatcher> pathIncludes,
      List<PathMatcher> pathExcludes,
      List<PathMatcher> volumeIncludes,
      List<PathMatcher> volumeExcludes
   ) {
      Path p = Paths.get(path);
      Path v = Paths.get(volume);
      return !matches(p, pathIncludes) && !matches(v, volumeIncludes) ? matches(p, pathExcludes) || matches(v, volumeExcludes) : false;
   }

   public static List<PathMatcher> loadAndParseFileSystemConfig(String configPropertyName) {
      String config = GlobalConfig.get(configPropertyName, "");
      return parseFileSystemConfig(config);
   }

   public static List<PathMatcher> parseFileSystemConfig(String config) {
      FileSystem fs = FileSystems.getDefault();
      List<PathMatcher> patterns = new ArrayList<>();

      for (String item : config.split(",")) {
         if (item.length() > 0) {
            if (!item.startsWith("glob:") && !item.startsWith("regex:")) {
               item = "glob:" + item;
            }

            patterns.add(fs.getPathMatcher(item));
         }
      }

      return patterns;
   }

   public static boolean matches(Path text, List<PathMatcher> patterns) {
      for (PathMatcher pattern : patterns) {
         if (pattern.matches(text)) {
            return true;
         }
      }

      return false;
   }
}
