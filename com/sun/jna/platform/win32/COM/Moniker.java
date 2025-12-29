package com.sun.jna.platform.win32.COM;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.PointerByReference;

public class Moniker extends Unknown implements IMoniker {
   static final int vTableIdStart = 7;

   public Moniker() {
   }

   public Moniker(Pointer pointer) {
      super(pointer);
   }

   @Override
   public void BindToObject() {
      int vTableId = 8;
      throw new UnsupportedOperationException();
   }

   @Override
   public void BindToStorage() {
      int vTableId = 9;
      throw new UnsupportedOperationException();
   }

   @Override
   public void Reduce() {
      int vTableId = 10;
      throw new UnsupportedOperationException();
   }

   @Override
   public void ComposeWith() {
      int vTableId = 11;
      throw new UnsupportedOperationException();
   }

   @Override
   public void Enum() {
      int vTableId = 12;
      throw new UnsupportedOperationException();
   }

   @Override
   public void IsEqual() {
      int vTableId = 13;
      throw new UnsupportedOperationException();
   }

   @Override
   public void Hash() {
      int vTableId = 14;
      throw new UnsupportedOperationException();
   }

   @Override
   public void IsRunning() {
      int vTableId = 15;
      throw new UnsupportedOperationException();
   }

   @Override
   public void GetTimeOfLastChange() {
      int vTableId = 16;
      throw new UnsupportedOperationException();
   }

   @Override
   public void Inverse() {
      int vTableId = 17;
      throw new UnsupportedOperationException();
   }

   @Override
   public void CommonPrefixWith() {
      int vTableId = 18;
      throw new UnsupportedOperationException();
   }

   @Override
   public void RelativePathTo() {
      int vTableId = 19;
      throw new UnsupportedOperationException();
   }

   @Override
   public String GetDisplayName(Pointer pbc, Pointer pmkToLeft) {
      int vTableId = 20;
      PointerByReference ppszDisplayNameRef = new PointerByReference();
      WinNT.HRESULT hr = (WinNT.HRESULT)this._invokeNativeObject(20, new Object[]{this.getPointer(), pbc, pmkToLeft, ppszDisplayNameRef}, WinNT.HRESULT.class);
      COMUtils.checkRC(hr);
      Pointer ppszDisplayName = ppszDisplayNameRef.getValue();
      if (ppszDisplayName == null) {
         return null;
      } else {
         WTypes.LPOLESTR oleStr = new WTypes.LPOLESTR(ppszDisplayName);
         String name = oleStr.getValue();
         Ole32.INSTANCE.CoTaskMemFree(ppszDisplayName);
         return name;
      }
   }

   @Override
   public void ParseDisplayName() {
      int vTableId = 21;
      throw new UnsupportedOperationException();
   }

   @Override
   public void IsSystemMoniker() {
      int vTableId = 22;
      throw new UnsupportedOperationException();
   }

   @Override
   public boolean IsDirty() {
      throw new UnsupportedOperationException();
   }

   @Override
   public void Load(IStream stm) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void Save(IStream stm) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void GetSizeMax() {
      throw new UnsupportedOperationException();
   }

   @Override
   public Guid.CLSID GetClassID() {
      throw new UnsupportedOperationException();
   }

   public static class ByReference extends Moniker implements Structure.ByReference {
   }
}
