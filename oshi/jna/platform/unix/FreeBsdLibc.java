package oshi.jna.platform.unix;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface FreeBsdLibc extends CLibrary {
   FreeBsdLibc INSTANCE = Native.load("libc", FreeBsdLibc.class);
   int UTX_USERSIZE = 32;
   int UTX_LINESIZE = 16;
   int UTX_IDSIZE = 8;
   int UTX_HOSTSIZE = 128;
   int UINT64_SIZE = Native.getNativeSize(long.class);
   int INT_SIZE = Native.getNativeSize(int.class);
   int CPUSTATES = 5;
   int CP_USER = 0;
   int CP_NICE = 1;
   int CP_SYS = 2;
   int CP_INTR = 3;
   int CP_IDLE = 4;

   FreeBsdLibc.FreeBsdUtmpx getutxent();

   @Structure.FieldOrder("cpu_ticks")
   public static class CpTime extends Structure implements AutoCloseable {
      public long[] cpu_ticks = new long[5];

      @Override
      public void close() {
         Pointer p = this.getPointer();
         if (p instanceof Memory) {
            ((Memory)p).close();
         }
      }
   }

   @Structure.FieldOrder({"ut_type", "ut_tv", "ut_id", "ut_pid", "ut_user", "ut_line", "ut_host", "ut_spare"})
   public static class FreeBsdUtmpx extends Structure {
      public short ut_type;
      public FreeBsdLibc.Timeval ut_tv;
      public byte[] ut_id = new byte[8];
      public int ut_pid;
      public byte[] ut_user = new byte[32];
      public byte[] ut_line = new byte[16];
      public byte[] ut_host = new byte[128];
      public byte[] ut_spare = new byte[64];
   }

   @Structure.FieldOrder({"tv_sec", "tv_usec"})
   public static class Timeval extends Structure {
      public long tv_sec;
      public long tv_usec;
   }
}
