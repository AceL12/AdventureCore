package com.sun.jna.platform.win32.COM;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

@Structure.FieldOrder("vtbl")
public class UnknownListener extends Structure {
   public UnknownVTable.ByReference vtbl = this.constructVTable();

   public UnknownListener(IUnknownCallback callback) {
      this.initVTable(callback);
      super.write();
   }

   protected UnknownVTable.ByReference constructVTable() {
      return new UnknownVTable.ByReference();
   }

   protected void initVTable(final IUnknownCallback callback) {
      this.vtbl.QueryInterfaceCallback = new UnknownVTable.QueryInterfaceCallback() {
         @Override
         public WinNT.HRESULT invoke(Pointer thisPointer, Guid.REFIID refid, PointerByReference ppvObject) {
            return callback.QueryInterface(refid, ppvObject);
         }
      };
      this.vtbl.AddRefCallback = new UnknownVTable.AddRefCallback() {
         @Override
         public int invoke(Pointer thisPointer) {
            return callback.AddRef();
         }
      };
      this.vtbl.ReleaseCallback = new UnknownVTable.ReleaseCallback() {
         @Override
         public int invoke(Pointer thisPointer) {
            return callback.Release();
         }
      };
   }
}
