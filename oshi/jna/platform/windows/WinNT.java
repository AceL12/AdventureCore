package oshi.jna.platform.windows;

import com.sun.jna.Structure;
import oshi.util.Util;

public interface WinNT extends com.sun.jna.platform.win32.WinNT {
   @Structure.FieldOrder("TokenIsElevated")
   public static class TOKEN_ELEVATION extends Structure implements AutoCloseable {
      public int TokenIsElevated;

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }
}
