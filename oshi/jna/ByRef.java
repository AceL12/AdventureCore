package oshi.jna;

import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.LibCAPI;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Tlhelp32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import oshi.util.Util;

public interface ByRef {
   public static class CloseableHANDLEByReference extends WinNT.HANDLEByReference implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableIntByReference extends IntByReference implements AutoCloseable {
      public CloseableIntByReference() {
      }

      public CloseableIntByReference(int value) {
         super(value);
      }

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableLONGLONGByReference extends WinDef.LONGLONGByReference implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableLongByReference extends LongByReference implements AutoCloseable {
      public CloseableLongByReference() {
      }

      public CloseableLongByReference(long value) {
         super(value);
      }

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableNativeLongByReference extends NativeLongByReference implements AutoCloseable {
      public CloseableNativeLongByReference() {
      }

      public CloseableNativeLongByReference(NativeLong nativeLong) {
         super(nativeLong);
      }

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseablePROCESSENTRY32ByReference extends Tlhelp32.PROCESSENTRY32.ByReference implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseablePointerByReference extends PointerByReference implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableSizeTByReference extends LibCAPI.size_t.ByReference implements AutoCloseable {
      public CloseableSizeTByReference() {
      }

      public CloseableSizeTByReference(long value) {
         super(value);
      }

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableULONGptrByReference extends BaseTSD.ULONG_PTRByReference implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }
}
