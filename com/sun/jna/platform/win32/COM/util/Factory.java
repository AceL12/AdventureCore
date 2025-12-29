package com.sun.jna.platform.win32.COM.util;

import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.OaIdl;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.IDispatchCallback;
import com.sun.jna.platform.win32.COM.util.annotation.ComObject;
import com.sun.jna.ptr.IntByReference;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Factory extends ObjectFactory {
   private ComThread comThread;

   public Factory() {
      this(new ComThread("Default Factory COM Thread", 5000L, new UncaughtExceptionHandler() {
         @Override
         public void uncaughtException(Thread t, Throwable e) {
         }
      }));
   }

   public Factory(ComThread comThread) {
      this.comThread = comThread;
   }

   @Override
   public <T> T createProxy(Class<T> comInterface, com.sun.jna.platform.win32.COM.IDispatch dispatch) {
      T result = super.createProxy(comInterface, dispatch);
      Factory.ProxyObject2 po2 = new Factory.ProxyObject2(result);
      return (T)Proxy.newProxyInstance(comInterface.getClassLoader(), new Class[]{comInterface}, po2);
   }

   @Override
   Guid.GUID discoverClsId(final ComObject annotation) {
      return this.runInComThread(new Callable<Guid.GUID>() {
         public Guid.GUID call() throws Exception {
            return Factory.super.discoverClsId(annotation);
         }
      });
   }

   @Override
   public <T> T fetchObject(final Class<T> comInterface) throws COMException {
      return this.runInComThread(new Callable<T>() {
         @Override
         public T call() throws Exception {
            return Factory.super.fetchObject(comInterface);
         }
      });
   }

   @Override
   public <T> T createObject(final Class<T> comInterface) {
      return this.runInComThread(new Callable<T>() {
         @Override
         public T call() throws Exception {
            return Factory.super.createObject(comInterface);
         }
      });
   }

   @Override
   IDispatchCallback createDispatchCallback(Class<?> comEventCallbackInterface, IComEventCallbackListener comEventCallbackListener) {
      return new Factory.CallbackProxy2(this, comEventCallbackInterface, comEventCallbackListener);
   }

   @Override
   public IRunningObjectTable getRunningObjectTable() {
      return super.getRunningObjectTable();
   }

   private <T> T runInComThread(Callable<T> callable) {
      try {
         return this.comThread.execute(callable);
      } catch (TimeoutException var4) {
         throw new RuntimeException(var4);
      } catch (InterruptedException var5) {
         throw new RuntimeException(var5);
      } catch (ExecutionException var6) {
         Throwable cause = var6.getCause();
         if (cause instanceof RuntimeException) {
            appendStacktrace(var6, cause);
            throw (RuntimeException)cause;
         } else {
            if (cause instanceof InvocationTargetException) {
               cause = ((InvocationTargetException)cause).getTargetException();
               if (cause instanceof RuntimeException) {
                  appendStacktrace(var6, cause);
                  throw (RuntimeException)cause;
               }
            }

            throw new RuntimeException(var6);
         }
      }
   }

   private static void appendStacktrace(Exception caughtException, Throwable toBeThrown) {
      StackTraceElement[] upperTrace = caughtException.getStackTrace();
      StackTraceElement[] lowerTrace = toBeThrown.getStackTrace();
      StackTraceElement[] trace = new StackTraceElement[upperTrace.length + lowerTrace.length];
      System.arraycopy(upperTrace, 0, trace, lowerTrace.length, upperTrace.length);
      System.arraycopy(lowerTrace, 0, trace, 0, lowerTrace.length);
      toBeThrown.setStackTrace(trace);
   }

   public ComThread getComThread() {
      return this.comThread;
   }

   private class CallbackProxy2 extends CallbackProxy {
      public CallbackProxy2(ObjectFactory factory, Class<?> comEventCallbackInterface, IComEventCallbackListener comEventCallbackListener) {
         super(factory, comEventCallbackInterface, comEventCallbackListener);
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
         ComThread.setComThread(true);

         WinNT.HRESULT var9;
         try {
            var9 = super.Invoke(dispIdMember, riid, lcid, wFlags, pDispParams, pVarResult, pExcepInfo, puArgErr);
         } finally {
            ComThread.setComThread(false);
         }

         return var9;
      }
   }

   private class ProxyObject2 implements InvocationHandler {
      private final Object delegate;

      public ProxyObject2(Object delegate) {
         this.delegate = delegate;
      }

      @Override
      public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
         if (args != null) {
            for (int i = 0; i < args.length; i++) {
               if (args[i] != null && Proxy.isProxyClass(args[i].getClass())) {
                  InvocationHandler ih = Proxy.getInvocationHandler(args[i]);
                  if (ih instanceof Factory.ProxyObject2) {
                     args[i] = ((Factory.ProxyObject2)ih).delegate;
                  }
               }
            }
         }

         return Factory.this.runInComThread(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
               return method.invoke(ProxyObject2.this.delegate, args);
            }
         });
      }
   }
}
