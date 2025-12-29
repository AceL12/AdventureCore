package com.sun.jna.platform.win32.COM.util;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Dispatch;
import com.sun.jna.platform.win32.COM.DispatchListener;
import com.sun.jna.platform.win32.COM.IDispatchCallback;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.COM.util.annotation.ComEventCallback;
import com.sun.jna.platform.win32.COM.util.annotation.ComInterface;
import com.sun.jna.platform.win32.COM.util.annotation.ComMethod;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallbackProxy implements IDispatchCallback {
   private static boolean DEFAULT_BOOLEAN;
   private static byte DEFAULT_BYTE;
   private static short DEFAULT_SHORT;
   private static int DEFAULT_INT;
   private static long DEFAULT_LONG;
   private static float DEFAULT_FLOAT;
   private static double DEFAULT_DOUBLE;
   ObjectFactory factory;
   Class<?> comEventCallbackInterface;
   IComEventCallbackListener comEventCallbackListener;
   Guid.REFIID listenedToRiid;
   public DispatchListener dispatchListener;
   Map<OaIdl.DISPID, Method> dsipIdMap;

   public CallbackProxy(ObjectFactory factory, Class<?> comEventCallbackInterface, IComEventCallbackListener comEventCallbackListener) {
      this.factory = factory;
      this.comEventCallbackInterface = comEventCallbackInterface;
      this.comEventCallbackListener = comEventCallbackListener;
      this.listenedToRiid = this.createRIID(comEventCallbackInterface);
      this.dsipIdMap = this.createDispIdMap(comEventCallbackInterface);
      this.dispatchListener = new DispatchListener(this);
   }

   Guid.REFIID createRIID(Class<?> comEventCallbackInterface) {
      ComInterface comInterfaceAnnotation = comEventCallbackInterface.getAnnotation(ComInterface.class);
      if (null == comInterfaceAnnotation) {
         throw new COMException("advise: Interface must define a value for either iid via the ComInterface annotation");
      } else {
         String iidStr = comInterfaceAnnotation.iid();
         if (null != iidStr && !iidStr.isEmpty()) {
            return new Guid.REFIID(new Guid.IID(iidStr).getPointer());
         } else {
            throw new COMException("ComInterface must define a value for iid");
         }
      }
   }

   Map<OaIdl.DISPID, Method> createDispIdMap(Class<?> comEventCallbackInterface) {
      Map<OaIdl.DISPID, Method> map = new HashMap<>();

      for (Method meth : comEventCallbackInterface.getMethods()) {
         ComEventCallback callbackAnnotation = meth.getAnnotation(ComEventCallback.class);
         ComMethod methodAnnotation = meth.getAnnotation(ComMethod.class);
         if (methodAnnotation != null) {
            int dispId = methodAnnotation.dispId();
            if (-1 == dispId) {
               dispId = this.fetchDispIdFromName(callbackAnnotation);
            }

            if (dispId == -1) {
               this.comEventCallbackListener.errorReceivingCallbackEvent("DISPID for " + meth.getName() + " not found", null);
            }

            map.put(new OaIdl.DISPID(dispId), meth);
         } else if (null != callbackAnnotation) {
            int dispIdx = callbackAnnotation.dispid();
            if (-1 == dispIdx) {
               dispIdx = this.fetchDispIdFromName(callbackAnnotation);
            }

            if (dispIdx == -1) {
               this.comEventCallbackListener.errorReceivingCallbackEvent("DISPID for " + meth.getName() + " not found", null);
            }

            map.put(new OaIdl.DISPID(dispIdx), meth);
         }
      }

      return map;
   }

   int fetchDispIdFromName(ComEventCallback annotation) {
      return -1;
   }

   void invokeOnThread(OaIdl.DISPID dispIdMember, Guid.REFIID riid, WinDef.LCID lcid, WinDef.WORD wFlags, OleAuto.DISPPARAMS.ByReference pDispParams) {
      Variant.VARIANT[] arguments = pDispParams.getArgs();
      Method eventMethod = this.dsipIdMap.get(dispIdMember);
      if (eventMethod == null) {
         this.comEventCallbackListener.errorReceivingCallbackEvent("No method found with dispId = " + dispIdMember, null);
      } else {
         OaIdl.DISPID[] positionMap = pDispParams.getRgdispidNamedArgs();
         Class<?>[] paramTypes = eventMethod.getParameterTypes();
         Object[] params = new Object[paramTypes.length];

         for (int i = 0; i < params.length && arguments.length - positionMap.length - i > 0; i++) {
            Class targetClass = paramTypes[i];
            Variant.VARIANT varg = arguments[arguments.length - i - 1];
            params[i] = Convert.toJavaObject(varg, targetClass, this.factory, true, false);
         }

         for (int i = 0; i < positionMap.length; i++) {
            int targetPosition = positionMap[i].intValue();
            if (targetPosition < params.length) {
               Class targetClass = paramTypes[targetPosition];
               Variant.VARIANT varg = arguments[i];
               params[targetPosition] = Convert.toJavaObject(varg, targetClass, this.factory, true, false);
            }
         }

         for (int ix = 0; ix < params.length; ix++) {
            if (params[ix] == null && paramTypes[ix].isPrimitive()) {
               if (paramTypes[ix].equals(boolean.class)) {
                  params[ix] = DEFAULT_BOOLEAN;
               } else if (paramTypes[ix].equals(byte.class)) {
                  params[ix] = DEFAULT_BYTE;
               } else if (paramTypes[ix].equals(short.class)) {
                  params[ix] = DEFAULT_SHORT;
               } else if (paramTypes[ix].equals(int.class)) {
                  params[ix] = DEFAULT_INT;
               } else if (paramTypes[ix].equals(long.class)) {
                  params[ix] = DEFAULT_LONG;
               } else if (paramTypes[ix].equals(float.class)) {
                  params[ix] = DEFAULT_FLOAT;
               } else {
                  if (!paramTypes[ix].equals(double.class)) {
                     throw new IllegalArgumentException("Class type " + paramTypes[ix].getName() + " not mapped to primitive default value.");
                  }

                  params[ix] = DEFAULT_DOUBLE;
               }
            }
         }

         try {
            eventMethod.invoke(this.comEventCallbackListener, params);
         } catch (Exception var17) {
            List<String> decodedClassNames = new ArrayList<>(params.length);

            for (Object o : params) {
               if (o == null) {
                  decodedClassNames.add("NULL");
               } else {
                  decodedClassNames.add(o.getClass().getName());
               }
            }

            this.comEventCallbackListener
               .errorReceivingCallbackEvent("Exception invoking method " + eventMethod + " supplied: " + decodedClassNames.toString(), var17);
         }
      }
   }

   @Override
   public Pointer getPointer() {
      return this.dispatchListener.getPointer();
   }

   @Override
   public WinNT.HRESULT GetTypeInfoCount(WinDef.UINTByReference pctinfo) {
      return new WinNT.HRESULT(-2147467263);
   }

   @Override
   public WinNT.HRESULT GetTypeInfo(WinDef.UINT iTInfo, WinDef.LCID lcid, PointerByReference ppTInfo) {
      return new WinNT.HRESULT(-2147467263);
   }

   @Override
   public WinNT.HRESULT GetIDsOfNames(Guid.REFIID riid, WString[] rgszNames, int cNames, WinDef.LCID lcid, OaIdl.DISPIDByReference rgDispId) {
      return new WinNT.HRESULT(-2147467263);
   }

   @Override
   public WinNT.HRESULT Invoke(
      OaIdl.DISPID dispIdMember,
      Guid.REFIID riid,
      WinDef.LCID lcid,
      WinDef.WORD wFlags,
      OleAuto.DISPPARAMS.ByReference pDispParams,
      Variant.VARIANT.ByReference pVarResult,
      OaIdl.EXCEPINFO.ByReference pExcepInfo,
      IntByReference puArgErr
   ) {
      assert COMUtils.comIsInitialized() : "Assumption about COM threading broken.";

      this.invokeOnThread(dispIdMember, riid, lcid, wFlags, pDispParams);
      return WinError.S_OK;
   }

   @Override
   public WinNT.HRESULT QueryInterface(Guid.REFIID refid, PointerByReference ppvObject) {
      if (null == ppvObject) {
         return new WinNT.HRESULT(-2147467261);
      } else if (refid.equals(this.listenedToRiid)) {
         ppvObject.setValue(this.getPointer());
         return WinError.S_OK;
      } else if (refid.getValue().equals(Unknown.IID_IUNKNOWN)) {
         ppvObject.setValue(this.getPointer());
         return WinError.S_OK;
      } else if (refid.getValue().equals(Dispatch.IID_IDISPATCH)) {
         ppvObject.setValue(this.getPointer());
         return WinError.S_OK;
      } else {
         return new WinNT.HRESULT(-2147467262);
      }
   }

   @Override
   public int AddRef() {
      return 0;
   }

   @Override
   public int Release() {
      return 0;
   }
}
