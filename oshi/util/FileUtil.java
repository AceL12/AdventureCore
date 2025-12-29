package oshi.util;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class FileUtil {
   private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);
   private static final String READING_LOG = "Reading file {}";
   private static final String READ_LOG = "Read {}";

   private FileUtil() {
   }

   public static List<String> readFile(String filename) {
      return readFile(filename, true);
   }

   public static List<String> readFile(String filename, boolean reportError) {
      if (new File(filename).canRead()) {
         if (LOG.isDebugEnabled()) {
            LOG.debug("Reading file {}", filename);
         }

         try {
            return Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);
         } catch (IOException var3) {
            if (reportError) {
               LOG.error("Error reading file {}. {}", filename, var3.getMessage());
            } else {
               LOG.debug("Error reading file {}. {}", filename, var3.getMessage());
            }
         }
      } else if (reportError) {
         LOG.warn("File not found or not readable: {}", filename);
      }

      return new ArrayList<>();
   }

   public static byte[] readAllBytes(String filename) {
      return readAllBytes(filename, true);
   }

   public static byte[] readAllBytes(String filename, boolean reportError) {
      if (new File(filename).canRead()) {
         if (LOG.isDebugEnabled()) {
            LOG.debug("Reading file {}", filename);
         }

         try {
            return Files.readAllBytes(Paths.get(filename));
         } catch (IOException var3) {
            if (reportError) {
               LOG.error("Error reading file {}. {}", filename, var3.getMessage());
            } else {
               LOG.debug("Error reading file {}. {}", filename, var3.getMessage());
            }
         }
      } else if (reportError) {
         LOG.warn("File not found or not readable: {}", filename);
      }

      return new byte[0];
   }

   public static ByteBuffer readAllBytesAsBuffer(String filename) {
      byte[] bytes = readAllBytes(filename, false);
      ByteBuffer buff = ByteBuffer.allocate(bytes.length);
      buff.order(ByteOrder.nativeOrder());

      for (byte b : bytes) {
         buff.put(b);
      }

      ((Buffer)buff).flip();
      return buff;
   }

   public static byte readByteFromBuffer(ByteBuffer buff) {
      return buff.position() < buff.limit() ? buff.get() : 0;
   }

   public static short readShortFromBuffer(ByteBuffer buff) {
      return buff.position() <= buff.limit() - 2 ? buff.getShort() : 0;
   }

   public static int readIntFromBuffer(ByteBuffer buff) {
      return buff.position() <= buff.limit() - 4 ? buff.getInt() : 0;
   }

   public static long readLongFromBuffer(ByteBuffer buff) {
      return buff.position() <= buff.limit() - 8 ? buff.getLong() : 0L;
   }

   public static NativeLong readNativeLongFromBuffer(ByteBuffer buff) {
      return new NativeLong(Native.LONG_SIZE == 4 ? readIntFromBuffer(buff) : readLongFromBuffer(buff));
   }

   public static LibCAPI.size_t readSizeTFromBuffer(ByteBuffer buff) {
      return new LibCAPI.size_t(Native.SIZE_T_SIZE == 4 ? readIntFromBuffer(buff) : readLongFromBuffer(buff));
   }

   public static void readByteArrayFromBuffer(ByteBuffer buff, byte[] array) {
      if (buff.position() <= buff.limit() - array.length) {
         buff.get(array);
      }
   }

   public static Pointer readPointerFromBuffer(ByteBuffer buff) {
      if (buff.position() <= buff.limit() - Native.POINTER_SIZE) {
         return Native.POINTER_SIZE == 4 ? new Pointer(buff.getInt()) : new Pointer(buff.getLong());
      } else {
         return Pointer.NULL;
      }
   }

   public static long getLongFromFile(String filename) {
      if (LOG.isDebugEnabled()) {
         LOG.debug("Reading file {}", filename);
      }

      List<String> read = readFile(filename, false);
      if (!read.isEmpty()) {
         if (LOG.isTraceEnabled()) {
            LOG.trace("Read {}", read.get(0));
         }

         return ParseUtil.parseLongOrDefault(read.get(0), 0L);
      } else {
         return 0L;
      }
   }

   public static long getUnsignedLongFromFile(String filename) {
      if (LOG.isDebugEnabled()) {
         LOG.debug("Reading file {}", filename);
      }

      List<String> read = readFile(filename, false);
      if (!read.isEmpty()) {
         if (LOG.isTraceEnabled()) {
            LOG.trace("Read {}", read.get(0));
         }

         return ParseUtil.parseUnsignedLongOrDefault(read.get(0), 0L);
      } else {
         return 0L;
      }
   }

   public static int getIntFromFile(String filename) {
      if (LOG.isDebugEnabled()) {
         LOG.debug("Reading file {}", filename);
      }

      try {
         List<String> read = readFile(filename, false);
         if (!read.isEmpty()) {
            if (LOG.isTraceEnabled()) {
               LOG.trace("Read {}", read.get(0));
            }

            return Integer.parseInt(read.get(0));
         }
      } catch (NumberFormatException var2) {
         LOG.warn("Unable to read value from {}. {}", filename, var2.getMessage());
      }

      return 0;
   }

   public static String getStringFromFile(String filename) {
      if (LOG.isDebugEnabled()) {
         LOG.debug("Reading file {}", filename);
      }

      List<String> read = readFile(filename, false);
      if (!read.isEmpty()) {
         if (LOG.isTraceEnabled()) {
            LOG.trace("Read {}", read.get(0));
         }

         return read.get(0);
      } else {
         return "";
      }
   }

   public static Map<String, String> getKeyValueMapFromFile(String filename, String separator) {
      Map<String, String> map = new HashMap<>();
      if (LOG.isDebugEnabled()) {
         LOG.debug("Reading file {}", filename);
      }

      for (String line : readFile(filename, false)) {
         String[] parts = line.split(separator);
         if (parts.length == 2) {
            map.put(parts[0], parts[1].trim());
         }
      }

      return map;
   }

   public static Properties readPropertiesFromFilename(String propsFilename) {
      Properties archProps = new Properties();

      for (ClassLoader loader : (LinkedHashSet)Stream.of(
            Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader(), FileUtil.class.getClassLoader()
         )
         .collect(Collectors.toCollection(LinkedHashSet::new))) {
         if (readPropertiesFromClassLoader(propsFilename, archProps, loader)) {
            return archProps;
         }
      }

      LOG.warn("Failed to load configuration file from classloader: {}", propsFilename);
      return archProps;
   }

   private static boolean readPropertiesFromClassLoader(String propsFilename, Properties archProps, ClassLoader loader) {
      if (loader == null) {
         return false;
      } else {
         try {
            List<URL> resources = Collections.list(loader.getResources(propsFilename));
            if (resources.isEmpty()) {
               LOG.debug("No {} file found from ClassLoader {}", propsFilename, loader);
               return false;
            } else {
               if (resources.size() > 1) {
                  LOG.warn("Configuration conflict: there is more than one {} file on the classpath: {}", propsFilename, resources);
               }

               InputStream in = resources.get(0).openStream();

               try {
                  if (in != null) {
                     archProps.load(in);
                  }
               } catch (Throwable var8) {
                  if (in != null) {
                     try {
                        in.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }
                  }

                  throw var8;
               }

               if (in != null) {
                  in.close();
               }

               return true;
            }
         } catch (IOException var9) {
            return false;
         }
      }
   }

   public static String readSymlinkTarget(File file) {
      try {
         return Files.readSymbolicLink(Paths.get(file.getAbsolutePath())).toString();
      } catch (IOException var2) {
         return null;
      }
   }
}
