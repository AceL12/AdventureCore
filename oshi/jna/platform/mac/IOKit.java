package oshi.jna.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;
import com.sun.jna.ptr.NativeLongByReference;
import oshi.util.Util;

public interface IOKit extends com.sun.jna.platform.mac.IOKit {
   IOKit INSTANCE = Native.load("IOKit", IOKit.class);

   int IOConnectCallStructMethod(
      com.sun.jna.platform.mac.IOKit.IOConnect var1, int var2, Structure var3, NativeLong var4, Structure var5, NativeLongByReference var6
   );

   @Structure.FieldOrder({"key", "vers", "pLimitData", "keyInfo", "result", "status", "data8", "data32", "bytes"})
   public static class SMCKeyData extends Structure implements AutoCloseable {
      public int key;
      public IOKit.SMCKeyDataVers vers;
      public IOKit.SMCKeyDataPLimitData pLimitData;
      public IOKit.SMCKeyDataKeyInfo keyInfo;
      public byte result;
      public byte status;
      public byte data8;
      public int data32;
      public byte[] bytes = new byte[32];

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   @Structure.FieldOrder({"dataSize", "dataType", "dataAttributes"})
   public static class SMCKeyDataKeyInfo extends Structure {
      public int dataSize;
      public int dataType;
      public byte dataAttributes;
   }

   @Structure.FieldOrder({"version", "length", "cpuPLimit", "gpuPLimit", "memPLimit"})
   public static class SMCKeyDataPLimitData extends Structure {
      public short version;
      public short length;
      public int cpuPLimit;
      public int gpuPLimit;
      public int memPLimit;
   }

   @Structure.FieldOrder({"major", "minor", "build", "reserved", "release"})
   public static class SMCKeyDataVers extends Structure {
      public byte major;
      public byte minor;
      public byte build;
      public byte reserved;
      public short release;
   }

   @Structure.FieldOrder({"key", "dataSize", "dataType", "bytes"})
   public static class SMCVal extends Structure implements AutoCloseable {
      public byte[] key = new byte[5];
      public int dataSize;
      public byte[] dataType = new byte[5];
      public byte[] bytes = new byte[32];

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }
}
