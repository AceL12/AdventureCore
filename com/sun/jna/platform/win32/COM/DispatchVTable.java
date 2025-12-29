package com.sun.jna.platform.win32.COM;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

@Structure.FieldOrder(
   {"QueryInterfaceCallback", "AddRefCallback", "ReleaseCallback", "GetTypeInfoCountCallback", "GetTypeInfoCallback", "GetIDsOfNamesCallback", "InvokeCallback"}
)
public class DispatchVTable extends Structure {
   public DispatchVTable.QueryInterfaceCallback QueryInterfaceCallback;
   public DispatchVTable.AddRefCallback AddRefCallback;
   public DispatchVTable.ReleaseCallback ReleaseCallback;
   public DispatchVTable.GetTypeInfoCountCallback GetTypeInfoCountCallback;
   public DispatchVTable.GetTypeInfoCallback GetTypeInfoCallback;
   public DispatchVTable.GetIDsOfNamesCallback GetIDsOfNamesCallback;
   public DispatchVTable.InvokeCallback InvokeCallback;

   public interface AddRefCallback extends StdCallLibrary.StdCallCallback {
      int invoke(Pointer var1);
   }

   public static class ByReference extends DispatchVTable implements Structure.ByReference {
   }

   public interface GetIDsOfNamesCallback extends StdCallLibrary.StdCallCallback {
      WinNT.HRESULT invoke(Pointer var1, Guid.REFIID var2, WString[] var3, int var4, WinDef.LCID var5, OaIdl.DISPIDByReference var6);
   }

   public interface GetTypeInfoCallback extends StdCallLibrary.StdCallCallback {
      WinNT.HRESULT invoke(Pointer var1, WinDef.UINT var2, WinDef.LCID var3, PointerByReference var4);
   }

   public interface GetTypeInfoCountCallback extends StdCallLibrary.StdCallCallback {
      WinNT.HRESULT invoke(Pointer var1, WinDef.UINTByReference var2);
   }

   public interface InvokeCallback extends StdCallLibrary.StdCallCallback {
      WinNT.HRESULT invoke(
         Pointer var1,
         OaIdl.DISPID var2,
         Guid.REFIID var3,
         WinDef.LCID var4,
         WinDef.WORD var5,
         OleAuto.DISPPARAMS.ByReference var6,
         Variant.VARIANT.ByReference var7,
         OaIdl.EXCEPINFO.ByReference var8,
         IntByReference var9
      );
   }

   public interface QueryInterfaceCallback extends StdCallLibrary.StdCallCallback {
      WinNT.HRESULT invoke(Pointer var1, Guid.REFIID var2, PointerByReference var3);
   }

   public interface ReleaseCallback extends StdCallLibrary.StdCallCallback {
      int invoke(Pointer var1);
   }
}
