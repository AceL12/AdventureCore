package com.sun.jna.platform.unix;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface LibCAPI extends Reboot, Resource {
   int HOST_NAME_MAX = 255;

   int getuid();

   int geteuid();

   int getgid();

   int getegid();

   int setuid(int var1);

   int seteuid(int var1);

   int setgid(int var1);

   int setegid(int var1);

   int gethostname(byte[] var1, int var2);

   int sethostname(String var1, int var2);

   int getdomainname(byte[] var1, int var2);

   int setdomainname(String var1, int var2);

   String getenv(String var1);

   int setenv(String var1, String var2, int var3);

   int unsetenv(String var1);

   int getloadavg(double[] var1, int var2);

   int close(int var1);

   int msync(Pointer var1, LibCAPI.size_t var2, int var3);

   int munmap(Pointer var1, LibCAPI.size_t var2);

   public static class size_t extends IntegerType {
      public static final LibCAPI.size_t ZERO = new LibCAPI.size_t();
      private static final long serialVersionUID = 1L;

      public size_t() {
         this(0L);
      }

      public size_t(long value) {
         super(Native.SIZE_T_SIZE, value, true);
      }

      public static class ByReference extends com.sun.jna.ptr.ByReference {
         public ByReference() {
            this(0L);
         }

         public ByReference(long value) {
            this(new LibCAPI.size_t(value));
         }

         public ByReference(LibCAPI.size_t value) {
            super(Native.SIZE_T_SIZE);
            this.setValue(value);
         }

         public void setValue(long value) {
            this.setValue(new LibCAPI.size_t(value));
         }

         public void setValue(LibCAPI.size_t value) {
            if (Native.SIZE_T_SIZE > 4) {
               this.getPointer().setLong(0L, value.longValue());
            } else {
               this.getPointer().setInt(0L, value.intValue());
            }
         }

         public long longValue() {
            return Native.SIZE_T_SIZE > 4 ? this.getPointer().getLong(0L) : this.getPointer().getInt(0L);
         }

         public LibCAPI.size_t getValue() {
            return new LibCAPI.size_t(this.longValue());
         }
      }
   }

   public static class ssize_t extends IntegerType {
      public static final LibCAPI.ssize_t ZERO = new LibCAPI.ssize_t();
      private static final long serialVersionUID = 1L;

      public ssize_t() {
         this(0L);
      }

      public ssize_t(long value) {
         super(Native.SIZE_T_SIZE, value, false);
      }
   }
}
