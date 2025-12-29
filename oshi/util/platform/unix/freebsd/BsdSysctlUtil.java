package oshi.util.platform.unix.freebsd;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.unix.LibCAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.platform.unix.FreeBsdLibc;

@ThreadSafe
public final class BsdSysctlUtil {
   private static final Logger LOG = LoggerFactory.getLogger(BsdSysctlUtil.class);
   private static final String SYSCTL_FAIL = "Failed sysctl call: {}, Error code: {}";

   private BsdSysctlUtil() {
   }

   public static int sysctl(String name, int def) {
      int intSize = FreeBsdLibc.INT_SIZE;
      Memory p = new Memory(intSize);

      int var5;
      label50: {
         try {
            ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference(intSize);

            label46: {
               try {
                  if (0 == FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
                     var5 = p.getInt(0L);
                     break label46;
                  }

                  LOG.warn("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                  var5 = def;
               } catch (Throwable var9) {
                  try {
                     size.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               size.close();
               break label50;
            }

            size.close();
         } catch (Throwable var10) {
            try {
               p.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }

            throw var10;
         }

         p.close();
         return var5;
      }

      p.close();
      return var5;
   }

   public static long sysctl(String name, long def) {
      int uint64Size = FreeBsdLibc.UINT64_SIZE;
      Memory p = new Memory(uint64Size);

      long var6;
      label50: {
         try {
            ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference(uint64Size);

            label46: {
               try {
                  if (0 == FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
                     var6 = p.getLong(0L);
                     break label46;
                  }

                  LOG.warn("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                  var6 = def;
               } catch (Throwable var10) {
                  try {
                     size.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }

                  throw var10;
               }

               size.close();
               break label50;
            }

            size.close();
         } catch (Throwable var11) {
            try {
               p.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         p.close();
         return var6;
      }

      p.close();
      return var6;
   }

   public static String sysctl(String name, String def) {
      ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference();

      String var10;
      label60: {
         String var4;
         label59: {
            try {
               if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, null, size, null, LibCAPI.size_t.ZERO)) {
                  LOG.warn("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                  var10 = def;
                  break label60;
               }

               Memory p = new Memory(size.longValue() + 1L);

               label55: {
                  try {
                     if (0 == FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, LibCAPI.size_t.ZERO)) {
                        var4 = p.getString(0L);
                        break label55;
                     }

                     LOG.warn("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                     var4 = def;
                  } catch (Throwable var8) {
                     try {
                        p.close();
                     } catch (Throwable var7) {
                        var8.addSuppressed(var7);
                     }

                     throw var8;
                  }

                  p.close();
                  break label59;
               }

               p.close();
            } catch (Throwable var9) {
               try {
                  size.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }

               throw var9;
            }

            size.close();
            return var4;
         }

         size.close();
         return var4;
      }

      size.close();
      return var10;
   }

   public static boolean sysctl(String name, Structure struct) {
      ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference(struct.size());

      boolean var3;
      label27: {
         try {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, struct.getPointer(), size, null, LibCAPI.size_t.ZERO)) {
               LOG.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
               var3 = false;
               break label27;
            }
         } catch (Throwable var6) {
            try {
               size.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         size.close();
         struct.read();
         return true;
      }

      size.close();
      return var3;
   }

   public static Memory sysctl(String name) {
      ByRef.CloseableSizeTByReference size = new ByRef.CloseableSizeTByReference();

      Memory m;
      label37: {
         Memory var7;
         label36: {
            try {
               if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, null, size, null, LibCAPI.size_t.ZERO)) {
                  LOG.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                  m = null;
                  break label37;
               }

               m = new Memory(size.longValue());
               if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, m, size, null, LibCAPI.size_t.ZERO)) {
                  LOG.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                  m.close();
                  var7 = null;
                  break label36;
               }

               var7 = m;
            } catch (Throwable var5) {
               try {
                  size.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            size.close();
            return var7;
         }

         size.close();
         return var7;
      }

      size.close();
      return m;
   }
}
