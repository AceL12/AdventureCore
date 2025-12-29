package com.sun.jna.platform.win32.COM;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

@Structure.FieldOrder({"QueryInterfaceCallback", "AddRefCallback", "ReleaseCallback"})
public class UnknownVTable extends Structure {
   public UnknownVTable.QueryInterfaceCallback QueryInterfaceCallback;
   public UnknownVTable.AddRefCallback AddRefCallback;
   public UnknownVTable.ReleaseCallback ReleaseCallback;

   public interface AddRefCallback extends StdCallLibrary.StdCallCallback {
      int invoke(Pointer var1);
   }

   public static class ByReference extends UnknownVTable implements Structure.ByReference {
   }

   public interface QueryInterfaceCallback extends StdCallLibrary.StdCallCallback {
      WinNT.HRESULT invoke(Pointer var1, Guid.REFIID var2, PointerByReference var3);
   }

   public interface ReleaseCallback extends StdCallLibrary.StdCallCallback {
      int invoke(Pointer var1);
   }
}
