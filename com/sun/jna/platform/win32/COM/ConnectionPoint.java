package com.sun.jna.platform.win32.COM;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

public class ConnectionPoint extends Unknown implements IConnectionPoint {
   public ConnectionPoint(Pointer pointer) {
      super(pointer);
   }

   @Override
   public WinNT.HRESULT GetConnectionInterface(Guid.IID iid) {
      int vTableId = 3;
      return (WinNT.HRESULT)this._invokeNativeObject(3, new Object[]{this.getPointer(), iid}, WinNT.HRESULT.class);
   }

   void GetConnectionPointContainer() {
      int vTableId = 4;
   }

   @Override
   public WinNT.HRESULT Advise(IUnknownCallback pUnkSink, WinDef.DWORDByReference pdwCookie) {
      int vTableId = 5;
      return (WinNT.HRESULT)this._invokeNativeObject(5, new Object[]{this.getPointer(), pUnkSink.getPointer(), pdwCookie}, WinNT.HRESULT.class);
   }

   @Override
   public WinNT.HRESULT Unadvise(WinDef.DWORD dwCookie) {
      int vTableId = 6;
      return (WinNT.HRESULT)this._invokeNativeObject(6, new Object[]{this.getPointer(), dwCookie}, WinNT.HRESULT.class);
   }

   void EnumConnections() {
      int vTableId = 7;
   }
}
