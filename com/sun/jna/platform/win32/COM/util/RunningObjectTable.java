package com.sun.jna.platform.win32.COM.util;

import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.ptr.PointerByReference;
import java.util.ArrayList;
import java.util.List;

public class RunningObjectTable implements IRunningObjectTable {
   ObjectFactory factory;
   com.sun.jna.platform.win32.COM.RunningObjectTable raw;

   protected RunningObjectTable(com.sun.jna.platform.win32.COM.RunningObjectTable raw, ObjectFactory factory) {
      this.raw = raw;
      this.factory = factory;
   }

   @Override
   public Iterable<IDispatch> enumRunning() {
      assert COMUtils.comIsInitialized() : "COM not initialized";

      PointerByReference ppenumMoniker = new PointerByReference();
      WinNT.HRESULT hr = this.raw.EnumRunning(ppenumMoniker);
      COMUtils.checkRC(hr);
      com.sun.jna.platform.win32.COM.EnumMoniker raw = new com.sun.jna.platform.win32.COM.EnumMoniker(ppenumMoniker.getValue());
      return new EnumMoniker(raw, this.raw, this.factory);
   }

   @Override
   public <T> List<T> getActiveObjectsByInterface(Class<T> comInterface) {
      assert COMUtils.comIsInitialized() : "COM not initialized";

      List<T> result = new ArrayList<>();

      for (IDispatch obj : this.enumRunning()) {
         try {
            T dobj = obj.queryInterface(comInterface);
            result.add(dobj);
         } catch (COMException var6) {
         }
      }

      return result;
   }
}
