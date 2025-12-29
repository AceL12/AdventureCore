package oshi.util.platform.mac;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.mac.IOKitUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.platform.mac.IOKit;
import oshi.jna.platform.mac.SystemB;
import oshi.util.ParseUtil;

@ThreadSafe
public final class SmcUtil {
   private static final Logger LOG = LoggerFactory.getLogger(SmcUtil.class);
   private static final IOKit IO = IOKit.INSTANCE;
   private static Map<Integer, IOKit.SMCKeyDataKeyInfo> keyInfoCache = new ConcurrentHashMap<>();
   private static final byte[] DATATYPE_SP78 = ParseUtil.asciiStringToByteArray("sp78", 5);
   private static final byte[] DATATYPE_FPE2 = ParseUtil.asciiStringToByteArray("fpe2", 5);
   private static final byte[] DATATYPE_FLT = ParseUtil.asciiStringToByteArray("flt ", 5);
   public static final String SMC_KEY_FAN_NUM = "FNum";
   public static final String SMC_KEY_FAN_SPEED = "F%dAc";
   public static final String SMC_KEY_CPU_TEMP = "TC0P";
   public static final String SMC_KEY_CPU_VOLTAGE = "VC0C";
   public static final byte SMC_CMD_READ_BYTES = 5;
   public static final byte SMC_CMD_READ_KEYINFO = 9;
   public static final int KERNEL_INDEX_SMC = 2;

   private SmcUtil() {
   }

   public static com.sun.jna.platform.mac.IOKit.IOConnect smcOpen() {
      com.sun.jna.platform.mac.IOKit.IOService smcService = IOKitUtil.getMatchingService("AppleSMC");
      if (smcService != null) {
         com.sun.jna.platform.mac.IOKit.IOConnect var3;
         try {
            ByRef.CloseablePointerByReference connPtr = new ByRef.CloseablePointerByReference();

            label85: {
               try {
                  int result = IO.IOServiceOpen(smcService, SystemB.INSTANCE.mach_task_self(), 0, connPtr);
                  if (result == 0) {
                     var3 = new com.sun.jna.platform.mac.IOKit.IOConnect(connPtr.getValue());
                     break label85;
                  }

                  if (LOG.isErrorEnabled()) {
                     LOG.error(String.format("Unable to open connection to AppleSMC service. Error: 0x%08x", result));
                  }
               } catch (Throwable var9) {
                  try {
                     connPtr.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               connPtr.close();
               return null;
            }

            connPtr.close();
         } finally {
            smcService.release();
         }

         return var3;
      } else {
         LOG.error("Unable to locate AppleSMC service");
         return null;
      }
   }

   public static int smcClose(com.sun.jna.platform.mac.IOKit.IOConnect conn) {
      return IO.IOServiceClose(conn);
   }

   public static double smcGetFloat(com.sun.jna.platform.mac.IOKit.IOConnect conn, String key) {
      IOKit.SMCVal val = new IOKit.SMCVal();

      double var9;
      label53: {
         label52: {
            label51: {
               try {
                  int result = smcReadKey(conn, key, val);
                  if (result == 0 && val.dataSize > 0) {
                     if (Arrays.equals(val.dataType, DATATYPE_SP78) && val.dataSize == 2) {
                        var9 = val.bytes[0] + val.bytes[1] / 256.0;
                        break label53;
                     }

                     if (Arrays.equals(val.dataType, DATATYPE_FPE2) && val.dataSize == 2) {
                        var9 = ParseUtil.byteArrayToFloat(val.bytes, val.dataSize, 2);
                        break label52;
                     }

                     if (Arrays.equals(val.dataType, DATATYPE_FLT) && val.dataSize == 4) {
                        var9 = ByteBuffer.wrap(val.bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                        break label51;
                     }
                  }
               } catch (Throwable var7) {
                  try {
                     val.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }

                  throw var7;
               }

               val.close();
               return 0.0;
            }

            val.close();
            return var9;
         }

         val.close();
         return var9;
      }

      val.close();
      return var9;
   }

   public static long smcGetLong(com.sun.jna.platform.mac.IOKit.IOConnect conn, String key) {
      IOKit.SMCVal val = new IOKit.SMCVal();

      long var4;
      label27: {
         try {
            int result = smcReadKey(conn, key, val);
            if (result == 0) {
               var4 = ParseUtil.byteArrayToLong(val.bytes, val.dataSize);
               break label27;
            }
         } catch (Throwable var7) {
            try {
               val.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         val.close();
         return 0L;
      }

      val.close();
      return var4;
   }

   public static int smcGetKeyInfo(com.sun.jna.platform.mac.IOKit.IOConnect conn, IOKit.SMCKeyData inputStructure, IOKit.SMCKeyData outputStructure) {
      if (keyInfoCache.containsKey(inputStructure.key)) {
         IOKit.SMCKeyDataKeyInfo keyInfo = keyInfoCache.get(inputStructure.key);
         outputStructure.keyInfo.dataSize = keyInfo.dataSize;
         outputStructure.keyInfo.dataType = keyInfo.dataType;
         outputStructure.keyInfo.dataAttributes = keyInfo.dataAttributes;
      } else {
         inputStructure.data8 = 9;
         int result = smcCall(conn, 2, inputStructure, outputStructure);
         if (result != 0) {
            return result;
         }

         IOKit.SMCKeyDataKeyInfo keyInfo = new IOKit.SMCKeyDataKeyInfo();
         keyInfo.dataSize = outputStructure.keyInfo.dataSize;
         keyInfo.dataType = outputStructure.keyInfo.dataType;
         keyInfo.dataAttributes = outputStructure.keyInfo.dataAttributes;
         keyInfoCache.put(inputStructure.key, keyInfo);
      }

      return 0;
   }

   public static int smcReadKey(com.sun.jna.platform.mac.IOKit.IOConnect conn, String key, IOKit.SMCVal val) {
      IOKit.SMCKeyData inputStructure = new IOKit.SMCKeyData();

      int var11;
      label52: {
         try {
            IOKit.SMCKeyData outputStructure = new IOKit.SMCKeyData();

            label48: {
               try {
                  label60: {
                     inputStructure.key = (int)ParseUtil.strToLong(key, 4);
                     int result = smcGetKeyInfo(conn, inputStructure, outputStructure);
                     if (result == 0) {
                        val.dataSize = outputStructure.keyInfo.dataSize;
                        val.dataType = ParseUtil.longToByteArray(outputStructure.keyInfo.dataType, 4, 5);
                        inputStructure.keyInfo.dataSize = val.dataSize;
                        inputStructure.data8 = 5;
                        result = smcCall(conn, 2, inputStructure, outputStructure);
                        if (result == 0) {
                           System.arraycopy(outputStructure.bytes, 0, val.bytes, 0, val.bytes.length);
                           var11 = 0;
                           break label60;
                        }
                     }

                     var11 = result;
                     break label48;
                  }
               } catch (Throwable var9) {
                  try {
                     outputStructure.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }

                  throw var9;
               }

               outputStructure.close();
               break label52;
            }

            outputStructure.close();
         } catch (Throwable var10) {
            try {
               inputStructure.close();
            } catch (Throwable var7) {
               var10.addSuppressed(var7);
            }

            throw var10;
         }

         inputStructure.close();
         return var11;
      }

      inputStructure.close();
      return var11;
   }

   public static int smcCall(com.sun.jna.platform.mac.IOKit.IOConnect conn, int index, IOKit.SMCKeyData inputStructure, IOKit.SMCKeyData outputStructure) {
      ByRef.CloseableNativeLongByReference size = new ByRef.CloseableNativeLongByReference(new NativeLong(outputStructure.size()));

      int var5;
      try {
         var5 = IO.IOConnectCallStructMethod(conn, index, inputStructure, new NativeLong(inputStructure.size()), outputStructure, size);
      } catch (Throwable var8) {
         try {
            size.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      size.close();
      return var5;
   }
}
