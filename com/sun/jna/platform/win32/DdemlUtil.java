package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DdemlUtil {
   public interface AdvdataHandler {
      int onAdvdata(int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5, Ddeml.HDDEDATA var6);
   }

   public interface AdvreqHandler {
      Ddeml.HDDEDATA onAdvreq(int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5, int var6);
   }

   public interface AdvstartHandler {
      boolean onAdvstart(int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5);
   }

   public interface AdvstopHandler {
      void onAdvstop(int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5);
   }

   public interface ConnectConfirmHandler {
      void onConnectConfirm(int var1, Ddeml.HCONV var2, Ddeml.HSZ var3, Ddeml.HSZ var4, boolean var5);
   }

   public interface ConnectHandler {
      boolean onConnect(int var1, Ddeml.HSZ var2, Ddeml.HSZ var3, Ddeml.CONVCONTEXT var4, boolean var5);
   }

   public static class DdeAdapter implements Ddeml.DdeCallback {
      private static final Logger LOG = Logger.getLogger(DdemlUtil.DdeAdapter.class.getName());
      private int idInst;
      private final List<DdemlUtil.AdvstartHandler> advstartHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.AdvstopHandler> advstopHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.ConnectHandler> connectHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.AdvreqHandler> advReqHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.RequestHandler> requestHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.WildconnectHandler> wildconnectHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.AdvdataHandler> advdataHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.ExecuteHandler> executeHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.PokeHandler> pokeHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.ConnectConfirmHandler> connectConfirmHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.DisconnectHandler> disconnectHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.ErrorHandler> errorHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.RegisterHandler> registerHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.XactCompleteHandler> xactCompleteHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.UnregisterHandler> unregisterHandler = new CopyOnWriteArrayList<>();
      private final List<DdemlUtil.MonitorHandler> monitorHandler = new CopyOnWriteArrayList<>();

      public void setInstanceIdentifier(int idInst) {
         this.idInst = idInst;
      }

      @Override
      public WinDef.PVOID ddeCallback(
         int wType, int wFmt, Ddeml.HCONV hConv, Ddeml.HSZ hsz1, Ddeml.HSZ hsz2, Ddeml.HDDEDATA hData, BaseTSD.ULONG_PTR lData1, BaseTSD.ULONG_PTR lData2
      ) {
         String transactionTypeName = null;

         try {
            switch (wType) {
               case 4144: {
                  boolean booleanResult = this.onAdvstart(wType, wFmt, hConv, hsz1, hsz2);
                  return new WinDef.PVOID(Pointer.createConstant(new WinDef.BOOL(booleanResult).intValue()));
               }
               case 4194: {
                  Ddeml.CONVCONTEXT convcontextx = null;
                  if (lData1.toPointer() != null) {
                     convcontextx = new Ddeml.CONVCONTEXT(new Pointer(lData1.longValue()));
                  }

                  boolean booleanResult = this.onConnect(wType, hsz1, hsz2, convcontextx, lData2 != null && lData2.intValue() != 0);
                  return new WinDef.PVOID(Pointer.createConstant(new WinDef.BOOL(booleanResult).intValue()));
               }
               case 8226:
                  int count = lData1.intValue() & 65535;
                  Ddeml.HDDEDATA data = this.onAdvreq(wType, wFmt, hConv, hsz1, hsz2, count);
                  if (data == null) {
                     return new WinDef.PVOID();
                  }

                  return new WinDef.PVOID(data.getPointer());
               case 8368:
                  Ddeml.HDDEDATA data = this.onRequest(wType, wFmt, hConv, hsz1, hsz2);
                  if (data == null) {
                     return new WinDef.PVOID();
                  }

                  return new WinDef.PVOID(data.getPointer());
               case 8418:
                  Ddeml.CONVCONTEXT convcontext = null;
                  if (lData1.toPointer() != null) {
                     convcontext = new Ddeml.CONVCONTEXT(new Pointer(lData1.longValue()));
                  }

                  Ddeml.HSZPAIR[] hszPairs = this.onWildconnect(wType, hsz1, hsz2, convcontext, lData2 != null && lData2.intValue() != 0);
                  if (hszPairs != null && hszPairs.length != 0) {
                     int size = 0;

                     for (Ddeml.HSZPAIR hp : hszPairs) {
                        hp.write();
                        size += hp.size();
                     }

                     Ddeml.HDDEDATA data = Ddeml.INSTANCE.DdeCreateDataHandle(this.idInst, hszPairs[0].getPointer(), size, 0, null, wFmt, 0);
                     return new WinDef.PVOID(data.getPointer());
                  }

                  return new WinDef.PVOID();
               case 16400: {
                  int intResult = this.onAdvdata(wType, wFmt, hConv, hsz1, hsz2, hData);
                  return new WinDef.PVOID(Pointer.createConstant(intResult));
               }
               case 16464: {
                  int intResult = this.onExecute(wType, hConv, hsz1, hData);
                  Ddeml.INSTANCE.DdeFreeDataHandle(hData);
                  return new WinDef.PVOID(Pointer.createConstant(intResult));
               }
               case 16528: {
                  int intResult = this.onPoke(wType, wFmt, hConv, hsz1, hsz2, hData);
                  return new WinDef.PVOID(Pointer.createConstant(intResult));
               }
               case 32770:
                  this.onError(wType, hConv, (int)(lData2.longValue() & 65535L));
                  break;
               case 32832:
                  this.onAdvstop(wType, wFmt, hConv, hsz1, hsz2);
                  break;
               case 32882:
                  this.onConnectConfirm(wType, hConv, hsz1, hsz2, lData2 != null && lData2.intValue() != 0);
                  break;
               case 32896:
                  this.onXactComplete(wType, wFmt, hConv, hsz1, hsz2, hData, lData1, lData2);
                  break;
               case 32930:
                  this.onRegister(wType, hsz1, hsz2);
                  break;
               case 32962:
                  this.onDisconnect(wType, hConv, lData2 != null && lData2.intValue() != 0);
                  break;
               case 32978:
                  this.onUnregister(wType, hsz1, hsz2);
                  break;
               case 33010:
                  this.onMonitor(wType, hData, lData2.intValue());
                  break;
               default:
                  LOG.log(Level.FINE, String.format("Not implemented Operation - Transaction type: 0x%X (%s)", wType, transactionTypeName));
            }
         } catch (DdemlUtil.DdeAdapter.BlockException var21) {
            return new WinDef.PVOID(Pointer.createConstant(-1));
         } catch (Throwable var22) {
            LOG.log(Level.WARNING, "Exception in DDECallback", var22);
         }

         return new WinDef.PVOID();
      }

      public void registerAdvstartHandler(DdemlUtil.AdvstartHandler handler) {
         this.advstartHandler.add(handler);
      }

      public void unregisterAdvstartHandler(DdemlUtil.AdvstartHandler handler) {
         this.advstartHandler.remove(handler);
      }

      private boolean onAdvstart(int transactionType, int dataFormat, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ item) {
         boolean oneHandlerTrue = false;

         for (DdemlUtil.AdvstartHandler handler : this.advstartHandler) {
            if (handler.onAdvstart(transactionType, dataFormat, hconv, topic, item)) {
               oneHandlerTrue = true;
            }
         }

         return oneHandlerTrue;
      }

      public void registerAdvstopHandler(DdemlUtil.AdvstopHandler handler) {
         this.advstopHandler.add(handler);
      }

      public void unregisterAdvstopHandler(DdemlUtil.AdvstopHandler handler) {
         this.advstopHandler.remove(handler);
      }

      private void onAdvstop(int transactionType, int dataFormat, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ item) {
         for (DdemlUtil.AdvstopHandler handler : this.advstopHandler) {
            handler.onAdvstop(transactionType, dataFormat, hconv, topic, item);
         }
      }

      public void registerConnectHandler(DdemlUtil.ConnectHandler handler) {
         this.connectHandler.add(handler);
      }

      public void unregisterConnectHandler(DdemlUtil.ConnectHandler handler) {
         this.connectHandler.remove(handler);
      }

      private boolean onConnect(int transactionType, Ddeml.HSZ topic, Ddeml.HSZ service, Ddeml.CONVCONTEXT convcontext, boolean sameInstance) {
         boolean oneHandlerTrue = false;

         for (DdemlUtil.ConnectHandler handler : this.connectHandler) {
            if (handler.onConnect(transactionType, topic, service, convcontext, sameInstance)) {
               oneHandlerTrue = true;
            }
         }

         return oneHandlerTrue;
      }

      public void registerAdvReqHandler(DdemlUtil.AdvreqHandler handler) {
         this.advReqHandler.add(handler);
      }

      public void unregisterAdvReqHandler(DdemlUtil.AdvreqHandler handler) {
         this.advReqHandler.remove(handler);
      }

      private Ddeml.HDDEDATA onAdvreq(int transactionType, int dataFormat, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ item, int count) {
         for (DdemlUtil.AdvreqHandler handler : this.advReqHandler) {
            Ddeml.HDDEDATA result = handler.onAdvreq(transactionType, dataFormat, hconv, topic, item, count);
            if (result != null) {
               return result;
            }
         }

         return null;
      }

      public void registerRequestHandler(DdemlUtil.RequestHandler handler) {
         this.requestHandler.add(handler);
      }

      public void unregisterRequestHandler(DdemlUtil.RequestHandler handler) {
         this.requestHandler.remove(handler);
      }

      private Ddeml.HDDEDATA onRequest(int transactionType, int dataFormat, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ item) {
         for (DdemlUtil.RequestHandler handler : this.requestHandler) {
            Ddeml.HDDEDATA result = handler.onRequest(transactionType, dataFormat, hconv, topic, item);
            if (result != null) {
               return result;
            }
         }

         return null;
      }

      public void registerWildconnectHandler(DdemlUtil.WildconnectHandler handler) {
         this.wildconnectHandler.add(handler);
      }

      public void unregisterWildconnectHandler(DdemlUtil.WildconnectHandler handler) {
         this.wildconnectHandler.remove(handler);
      }

      private Ddeml.HSZPAIR[] onWildconnect(int transactionType, Ddeml.HSZ topic, Ddeml.HSZ service, Ddeml.CONVCONTEXT convcontext, boolean sameInstance) {
         List<Ddeml.HSZPAIR> hszpairs = new ArrayList<>(1);

         for (DdemlUtil.WildconnectHandler handler : this.wildconnectHandler) {
            hszpairs.addAll(handler.onWildconnect(transactionType, topic, service, convcontext, sameInstance));
         }

         return hszpairs.toArray(new Ddeml.HSZPAIR[0]);
      }

      public void registerAdvdataHandler(DdemlUtil.AdvdataHandler handler) {
         this.advdataHandler.add(handler);
      }

      public void unregisterAdvdataHandler(DdemlUtil.AdvdataHandler handler) {
         this.advdataHandler.remove(handler);
      }

      private int onAdvdata(int transactionType, int dataFormat, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ item, Ddeml.HDDEDATA hdata) {
         for (DdemlUtil.AdvdataHandler handler : this.advdataHandler) {
            int result = handler.onAdvdata(transactionType, dataFormat, hconv, topic, item, hdata);
            if (result != 0) {
               return result;
            }
         }

         return 0;
      }

      public void registerExecuteHandler(DdemlUtil.ExecuteHandler handler) {
         this.executeHandler.add(handler);
      }

      public void unregisterExecuteHandler(DdemlUtil.ExecuteHandler handler) {
         this.executeHandler.remove(handler);
      }

      private int onExecute(int transactionType, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HDDEDATA commandString) {
         for (DdemlUtil.ExecuteHandler handler : this.executeHandler) {
            int result = handler.onExecute(transactionType, hconv, topic, commandString);
            if (result != 0) {
               return result;
            }
         }

         return 0;
      }

      public void registerPokeHandler(DdemlUtil.PokeHandler handler) {
         this.pokeHandler.add(handler);
      }

      public void unregisterPokeHandler(DdemlUtil.PokeHandler handler) {
         this.pokeHandler.remove(handler);
      }

      private int onPoke(int transactionType, int dataFormat, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ item, Ddeml.HDDEDATA hdata) {
         for (DdemlUtil.PokeHandler handler : this.pokeHandler) {
            int result = handler.onPoke(transactionType, dataFormat, hconv, topic, item, hdata);
            if (result != 0) {
               return result;
            }
         }

         return 0;
      }

      public void registerConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler handler) {
         this.connectConfirmHandler.add(handler);
      }

      public void unregisterConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler handler) {
         this.connectConfirmHandler.remove(handler);
      }

      private void onConnectConfirm(int transactionType, Ddeml.HCONV hconv, Ddeml.HSZ topic, Ddeml.HSZ service, boolean sameInstance) {
         for (DdemlUtil.ConnectConfirmHandler handler : this.connectConfirmHandler) {
            handler.onConnectConfirm(transactionType, hconv, topic, service, sameInstance);
         }
      }

      public void registerDisconnectHandler(DdemlUtil.DisconnectHandler handler) {
         this.disconnectHandler.add(handler);
      }

      public void unregisterDisconnectHandler(DdemlUtil.DisconnectHandler handler) {
         this.disconnectHandler.remove(handler);
      }

      private void onDisconnect(int transactionType, Ddeml.HCONV hconv, boolean sameInstance) {
         for (DdemlUtil.DisconnectHandler handler : this.disconnectHandler) {
            handler.onDisconnect(transactionType, hconv, sameInstance);
         }
      }

      public void registerErrorHandler(DdemlUtil.ErrorHandler handler) {
         this.errorHandler.add(handler);
      }

      public void unregisterErrorHandler(DdemlUtil.ErrorHandler handler) {
         this.errorHandler.remove(handler);
      }

      private void onError(int transactionType, Ddeml.HCONV hconv, int errorCode) {
         for (DdemlUtil.ErrorHandler handler : this.errorHandler) {
            handler.onError(transactionType, hconv, errorCode);
         }
      }

      public void registerRegisterHandler(DdemlUtil.RegisterHandler handler) {
         this.registerHandler.add(handler);
      }

      public void unregisterRegisterHandler(DdemlUtil.RegisterHandler handler) {
         this.registerHandler.remove(handler);
      }

      private void onRegister(int transactionType, Ddeml.HSZ baseServiceName, Ddeml.HSZ instanceSpecificServiceName) {
         for (DdemlUtil.RegisterHandler handler : this.registerHandler) {
            handler.onRegister(transactionType, baseServiceName, instanceSpecificServiceName);
         }
      }

      public void registerXactCompleteHandler(DdemlUtil.XactCompleteHandler handler) {
         this.xactCompleteHandler.add(handler);
      }

      public void xactCompleteXactCompleteHandler(DdemlUtil.XactCompleteHandler handler) {
         this.xactCompleteHandler.remove(handler);
      }

      private void onXactComplete(
         int transactionType,
         int dataFormat,
         Ddeml.HCONV hConv,
         Ddeml.HSZ topic,
         Ddeml.HSZ item,
         Ddeml.HDDEDATA hdata,
         BaseTSD.ULONG_PTR transactionIdentifier,
         BaseTSD.ULONG_PTR statusFlag
      ) {
         for (DdemlUtil.XactCompleteHandler handler : this.xactCompleteHandler) {
            handler.onXactComplete(transactionType, dataFormat, hConv, topic, item, hdata, transactionIdentifier, statusFlag);
         }
      }

      public void registerUnregisterHandler(DdemlUtil.UnregisterHandler handler) {
         this.unregisterHandler.add(handler);
      }

      public void unregisterUnregisterHandler(DdemlUtil.UnregisterHandler handler) {
         this.unregisterHandler.remove(handler);
      }

      private void onUnregister(int transactionType, Ddeml.HSZ baseServiceName, Ddeml.HSZ instanceSpecificServiceName) {
         for (DdemlUtil.UnregisterHandler handler : this.unregisterHandler) {
            handler.onUnregister(transactionType, baseServiceName, instanceSpecificServiceName);
         }
      }

      public void registerMonitorHandler(DdemlUtil.MonitorHandler handler) {
         this.monitorHandler.add(handler);
      }

      public void unregisterMonitorHandler(DdemlUtil.MonitorHandler handler) {
         this.monitorHandler.remove(handler);
      }

      private void onMonitor(int transactionType, Ddeml.HDDEDATA hdata, int dwData2) {
         for (DdemlUtil.MonitorHandler handler : this.monitorHandler) {
            handler.onMonitor(transactionType, hdata, dwData2);
         }
      }

      public static class BlockException extends RuntimeException {
      }
   }

   public static class DdeClient implements DdemlUtil.IDdeClient {
      private Integer idInst;
      private final DdemlUtil.DdeAdapter ddeAdapter = new DdemlUtil.DdeAdapter();

      @Override
      public Integer getInstanceIdentitifier() {
         return this.idInst;
      }

      @Override
      public void initialize(int afCmd) throws DdemlUtil.DdemlException {
         WinDef.DWORDByReference pidInst = new WinDef.DWORDByReference();
         Integer result = Ddeml.INSTANCE.DdeInitialize(pidInst, this.ddeAdapter, afCmd, 0);
         if (result != 0) {
            throw DdemlUtil.DdemlException.create(result);
         } else {
            this.idInst = pidInst.getValue().intValue();
            if (this.ddeAdapter instanceof DdemlUtil.DdeAdapter) {
               this.ddeAdapter.setInstanceIdentifier(this.idInst);
            }
         }
      }

      @Override
      public Ddeml.HSZ createStringHandle(String value) throws DdemlUtil.DdemlException {
         if (value == null) {
            return null;
         } else {
            int codePage;
            if (W32APIOptions.DEFAULT_OPTIONS == W32APIOptions.UNICODE_OPTIONS) {
               codePage = 1200;
            } else {
               codePage = 1004;
            }

            Ddeml.HSZ handle = Ddeml.INSTANCE.DdeCreateStringHandle(this.idInst, value, codePage);
            if (handle == null) {
               throw DdemlUtil.DdemlException.create(this.getLastError());
            } else {
               return handle;
            }
         }
      }

      @Override
      public void nameService(Ddeml.HSZ name, int afCmd) throws DdemlUtil.DdemlException {
         Ddeml.HDDEDATA handle = Ddeml.INSTANCE.DdeNameService(this.idInst, name, new Ddeml.HSZ(), afCmd);
         if (handle == null) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         }
      }

      @Override
      public void nameService(String name, int afCmd) throws DdemlUtil.DdemlException {
         Ddeml.HSZ nameHSZ = null;

         try {
            nameHSZ = this.createStringHandle(name);
            this.nameService(nameHSZ, afCmd);
         } finally {
            this.freeStringHandle(nameHSZ);
         }
      }

      @Override
      public int getLastError() {
         return Ddeml.INSTANCE.DdeGetLastError(this.idInst);
      }

      @Override
      public DdemlUtil.IDdeConnection connect(Ddeml.HSZ service, Ddeml.HSZ topic, Ddeml.CONVCONTEXT convcontext) {
         Ddeml.HCONV hconv = Ddeml.INSTANCE.DdeConnect(this.idInst, service, topic, convcontext);
         if (hconv == null) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         } else {
            return new DdemlUtil.DdeConnection(this, hconv);
         }
      }

      @Override
      public DdemlUtil.IDdeConnection connect(String service, String topic, Ddeml.CONVCONTEXT convcontext) {
         Ddeml.HSZ serviceHSZ = null;
         Ddeml.HSZ topicHSZ = null;

         DdemlUtil.IDdeConnection var6;
         try {
            serviceHSZ = this.createStringHandle(service);
            topicHSZ = this.createStringHandle(topic);
            var6 = this.connect(serviceHSZ, topicHSZ, convcontext);
         } finally {
            this.freeStringHandle(topicHSZ);
            this.freeStringHandle(serviceHSZ);
         }

         return var6;
      }

      @Override
      public String queryString(Ddeml.HSZ value) throws DdemlUtil.DdemlException {
         int codePage;
         int byteWidth;
         if (W32APIOptions.DEFAULT_OPTIONS == W32APIOptions.UNICODE_OPTIONS) {
            codePage = 1200;
            byteWidth = 2;
         } else {
            codePage = 1004;
            byteWidth = 1;
         }

         Memory buffer = new Memory(257 * byteWidth);

         String var6;
         try {
            int length = Ddeml.INSTANCE.DdeQueryString(this.idInst, value, buffer, 256, codePage);
            if (W32APIOptions.DEFAULT_OPTIONS != W32APIOptions.UNICODE_OPTIONS) {
               return buffer.getString(0L);
            }

            var6 = buffer.getWideString(0L);
         } finally {
            buffer.valid();
         }

         return var6;
      }

      @Override
      public Ddeml.HDDEDATA createDataHandle(Pointer pSrc, int cb, int cbOff, Ddeml.HSZ hszItem, int wFmt, int afCmd) {
         Ddeml.HDDEDATA returnData = Ddeml.INSTANCE.DdeCreateDataHandle(this.idInst, pSrc, cb, cbOff, hszItem, wFmt, afCmd);
         if (returnData == null) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         } else {
            return returnData;
         }
      }

      @Override
      public void freeDataHandle(Ddeml.HDDEDATA hData) {
         boolean result = Ddeml.INSTANCE.DdeFreeDataHandle(hData);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         }
      }

      @Override
      public Ddeml.HDDEDATA addData(Ddeml.HDDEDATA hData, Pointer pSrc, int cb, int cbOff) {
         Ddeml.HDDEDATA newHandle = Ddeml.INSTANCE.DdeAddData(hData, pSrc, cb, cbOff);
         if (newHandle == null) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         } else {
            return newHandle;
         }
      }

      @Override
      public int getData(Ddeml.HDDEDATA hData, Pointer pDst, int cbMax, int cbOff) {
         int result = Ddeml.INSTANCE.DdeGetData(hData, pDst, cbMax, cbOff);
         int errorCode = this.getLastError();
         if (errorCode != 0) {
            throw DdemlUtil.DdemlException.create(errorCode);
         } else {
            return result;
         }
      }

      @Override
      public Pointer accessData(Ddeml.HDDEDATA hData, WinDef.DWORDByReference pcbDataSize) {
         Pointer result = Ddeml.INSTANCE.DdeAccessData(hData, pcbDataSize);
         if (result == null) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         } else {
            return result;
         }
      }

      @Override
      public void unaccessData(Ddeml.HDDEDATA hData) {
         boolean result = Ddeml.INSTANCE.DdeUnaccessData(hData);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         }
      }

      @Override
      public void postAdvise(Ddeml.HSZ hszTopic, Ddeml.HSZ hszItem) {
         boolean result = Ddeml.INSTANCE.DdePostAdvise(this.idInst, hszTopic, hszItem);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         }
      }

      @Override
      public void postAdvise(String topic, String item) {
         Ddeml.HSZ itemHSZ = null;
         Ddeml.HSZ topicHSZ = null;

         try {
            topicHSZ = this.createStringHandle(topic);
            itemHSZ = this.createStringHandle(item);
            this.postAdvise(topicHSZ, itemHSZ);
         } finally {
            this.freeStringHandle(topicHSZ);
            this.freeStringHandle(itemHSZ);
         }
      }

      @Override
      public boolean freeStringHandle(Ddeml.HSZ value) {
         return value == null ? true : Ddeml.INSTANCE.DdeFreeStringHandle(this.idInst, value);
      }

      @Override
      public boolean keepStringHandle(Ddeml.HSZ value) {
         return Ddeml.INSTANCE.DdeKeepStringHandle(this.idInst, value);
      }

      @Override
      public void abandonTransactions() {
         boolean result = Ddeml.INSTANCE.DdeAbandonTransaction(this.idInst, null, 0);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         }
      }

      @Override
      public DdemlUtil.IDdeConnectionList connectList(Ddeml.HSZ service, Ddeml.HSZ topic, DdemlUtil.IDdeConnectionList existingList, Ddeml.CONVCONTEXT ctx) {
         Ddeml.HCONVLIST convlist = Ddeml.INSTANCE.DdeConnectList(this.idInst, service, topic, existingList != null ? existingList.getHandle() : null, ctx);
         if (convlist == null) {
            throw DdemlUtil.DdemlException.create(this.getLastError());
         } else {
            return new DdemlUtil.DdeConnectionList(this, convlist);
         }
      }

      @Override
      public DdemlUtil.IDdeConnectionList connectList(String service, String topic, DdemlUtil.IDdeConnectionList existingList, Ddeml.CONVCONTEXT ctx) {
         Ddeml.HSZ serviceHSZ = null;
         Ddeml.HSZ topicHSZ = null;

         DdemlUtil.IDdeConnectionList var7;
         try {
            serviceHSZ = this.createStringHandle(service);
            topicHSZ = this.createStringHandle(topic);
            var7 = this.connectList(serviceHSZ, topicHSZ, existingList, ctx);
         } finally {
            this.freeStringHandle(topicHSZ);
            this.freeStringHandle(serviceHSZ);
         }

         return var7;
      }

      @Override
      public boolean enableCallback(int wCmd) {
         boolean result = Ddeml.INSTANCE.DdeEnableCallback(this.idInst, null, wCmd);
         if (!result && wCmd != 2) {
            int errorCode = this.getLastError();
            if (errorCode != 0) {
               throw DdemlUtil.DdemlException.create(this.getLastError());
            }
         }

         return result;
      }

      @Override
      public boolean uninitialize() {
         return Ddeml.INSTANCE.DdeUninitialize(this.idInst);
      }

      @Override
      public void close() {
         this.uninitialize();
      }

      @Override
      public DdemlUtil.IDdeConnection wrap(Ddeml.HCONV hconv) {
         return new DdemlUtil.DdeConnection(this, hconv);
      }

      @Override
      public void unregisterDisconnectHandler(DdemlUtil.DisconnectHandler handler) {
         this.ddeAdapter.unregisterDisconnectHandler(handler);
      }

      @Override
      public void registerAdvstartHandler(DdemlUtil.AdvstartHandler handler) {
         this.ddeAdapter.registerAdvstartHandler(handler);
      }

      @Override
      public void unregisterAdvstartHandler(DdemlUtil.AdvstartHandler handler) {
         this.ddeAdapter.unregisterAdvstartHandler(handler);
      }

      @Override
      public void registerAdvstopHandler(DdemlUtil.AdvstopHandler handler) {
         this.ddeAdapter.registerAdvstopHandler(handler);
      }

      @Override
      public void unregisterAdvstopHandler(DdemlUtil.AdvstopHandler handler) {
         this.ddeAdapter.unregisterAdvstopHandler(handler);
      }

      @Override
      public void registerConnectHandler(DdemlUtil.ConnectHandler handler) {
         this.ddeAdapter.registerConnectHandler(handler);
      }

      @Override
      public void unregisterConnectHandler(DdemlUtil.ConnectHandler handler) {
         this.ddeAdapter.unregisterConnectHandler(handler);
      }

      @Override
      public void registerAdvReqHandler(DdemlUtil.AdvreqHandler handler) {
         this.ddeAdapter.registerAdvReqHandler(handler);
      }

      @Override
      public void unregisterAdvReqHandler(DdemlUtil.AdvreqHandler handler) {
         this.ddeAdapter.unregisterAdvReqHandler(handler);
      }

      @Override
      public void registerRequestHandler(DdemlUtil.RequestHandler handler) {
         this.ddeAdapter.registerRequestHandler(handler);
      }

      @Override
      public void unregisterRequestHandler(DdemlUtil.RequestHandler handler) {
         this.ddeAdapter.unregisterRequestHandler(handler);
      }

      @Override
      public void registerWildconnectHandler(DdemlUtil.WildconnectHandler handler) {
         this.ddeAdapter.registerWildconnectHandler(handler);
      }

      @Override
      public void unregisterWildconnectHandler(DdemlUtil.WildconnectHandler handler) {
         this.ddeAdapter.unregisterWildconnectHandler(handler);
      }

      @Override
      public void registerAdvdataHandler(DdemlUtil.AdvdataHandler handler) {
         this.ddeAdapter.registerAdvdataHandler(handler);
      }

      @Override
      public void unregisterAdvdataHandler(DdemlUtil.AdvdataHandler handler) {
         this.ddeAdapter.unregisterAdvdataHandler(handler);
      }

      @Override
      public void registerExecuteHandler(DdemlUtil.ExecuteHandler handler) {
         this.ddeAdapter.registerExecuteHandler(handler);
      }

      @Override
      public void unregisterExecuteHandler(DdemlUtil.ExecuteHandler handler) {
         this.ddeAdapter.unregisterExecuteHandler(handler);
      }

      @Override
      public void registerPokeHandler(DdemlUtil.PokeHandler handler) {
         this.ddeAdapter.registerPokeHandler(handler);
      }

      @Override
      public void unregisterPokeHandler(DdemlUtil.PokeHandler handler) {
         this.ddeAdapter.unregisterPokeHandler(handler);
      }

      @Override
      public void registerConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler handler) {
         this.ddeAdapter.registerConnectConfirmHandler(handler);
      }

      @Override
      public void unregisterConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler handler) {
         this.ddeAdapter.unregisterConnectConfirmHandler(handler);
      }

      @Override
      public void registerDisconnectHandler(DdemlUtil.DisconnectHandler handler) {
         this.ddeAdapter.registerDisconnectHandler(handler);
      }

      @Override
      public void registerErrorHandler(DdemlUtil.ErrorHandler handler) {
         this.ddeAdapter.registerErrorHandler(handler);
      }

      @Override
      public void unregisterErrorHandler(DdemlUtil.ErrorHandler handler) {
         this.ddeAdapter.unregisterErrorHandler(handler);
      }

      @Override
      public void registerRegisterHandler(DdemlUtil.RegisterHandler handler) {
         this.ddeAdapter.registerRegisterHandler(handler);
      }

      @Override
      public void unregisterRegisterHandler(DdemlUtil.RegisterHandler handler) {
         this.ddeAdapter.unregisterRegisterHandler(handler);
      }

      @Override
      public void registerXactCompleteHandler(DdemlUtil.XactCompleteHandler handler) {
         this.ddeAdapter.registerXactCompleteHandler(handler);
      }

      @Override
      public void unregisterXactCompleteHandler(DdemlUtil.XactCompleteHandler handler) {
         this.ddeAdapter.xactCompleteXactCompleteHandler(handler);
      }

      @Override
      public void registerUnregisterHandler(DdemlUtil.UnregisterHandler handler) {
         this.ddeAdapter.registerUnregisterHandler(handler);
      }

      @Override
      public void unregisterUnregisterHandler(DdemlUtil.UnregisterHandler handler) {
         this.ddeAdapter.unregisterUnregisterHandler(handler);
      }

      @Override
      public void registerMonitorHandler(DdemlUtil.MonitorHandler handler) {
         this.ddeAdapter.registerMonitorHandler(handler);
      }

      @Override
      public void unregisterMonitorHandler(DdemlUtil.MonitorHandler handler) {
         this.ddeAdapter.unregisterMonitorHandler(handler);
      }
   }

   public static class DdeConnection implements DdemlUtil.IDdeConnection {
      private Ddeml.HCONV conv;
      private final DdemlUtil.IDdeClient client;

      public DdeConnection(DdemlUtil.IDdeClient client, Ddeml.HCONV conv) {
         this.conv = conv;
         this.client = client;
      }

      @Override
      public Ddeml.HCONV getConv() {
         return this.conv;
      }

      @Override
      public void abandonTransaction(int transactionId) {
         boolean result = Ddeml.INSTANCE.DdeAbandonTransaction(this.client.getInstanceIdentitifier(), this.conv, transactionId);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }

      @Override
      public void abandonTransactions() {
         boolean result = Ddeml.INSTANCE.DdeAbandonTransaction(this.client.getInstanceIdentitifier(), this.conv, 0);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }

      @Override
      public Ddeml.HDDEDATA clientTransaction(
         Pointer data, int dataLength, Ddeml.HSZ item, int wFmt, int transaction, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle
      ) {
         if (timeout == -1 && result == null) {
            result = new WinDef.DWORDByReference();
         }

         Ddeml.HDDEDATA returnData = Ddeml.INSTANCE.DdeClientTransaction(data, dataLength, this.conv, item, wFmt, transaction, timeout, result);
         if (returnData == null) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         } else {
            if (userHandle != null) {
               if (timeout != -1) {
                  this.setUserHandle(-1, userHandle);
               } else {
                  this.setUserHandle(result.getValue().intValue(), userHandle);
               }
            }

            return returnData;
         }
      }

      @Override
      public Ddeml.HDDEDATA clientTransaction(
         Pointer data, int dataLength, String item, int wFmt, int transaction, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle
      ) {
         Ddeml.HSZ itemHSZ = null;

         Ddeml.HDDEDATA var10;
         try {
            itemHSZ = this.client.createStringHandle(item);
            var10 = this.clientTransaction(data, dataLength, itemHSZ, wFmt, transaction, timeout, result, userHandle);
         } finally {
            this.client.freeStringHandle(itemHSZ);
         }

         return var10;
      }

      @Override
      public void poke(Pointer data, int dataLength, Ddeml.HSZ item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         this.clientTransaction(data, dataLength, item, wFmt, 16528, timeout, result, userHandle);
      }

      @Override
      public void poke(Pointer data, int dataLength, String item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         Ddeml.HSZ itemHSZ = null;

         try {
            itemHSZ = this.client.createStringHandle(item);
            this.poke(data, dataLength, itemHSZ, wFmt, timeout, result, userHandle);
         } finally {
            this.client.freeStringHandle(itemHSZ);
         }
      }

      @Override
      public Ddeml.HDDEDATA request(Ddeml.HSZ item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         return this.clientTransaction(Pointer.NULL, 0, item, wFmt, 8368, timeout, result, userHandle);
      }

      @Override
      public Ddeml.HDDEDATA request(String item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         Ddeml.HSZ itemHSZ = null;

         Ddeml.HDDEDATA var7;
         try {
            itemHSZ = this.client.createStringHandle(item);
            var7 = this.request(itemHSZ, wFmt, timeout, result, userHandle);
         } finally {
            this.client.freeStringHandle(itemHSZ);
         }

         return var7;
      }

      @Override
      public void execute(String executeString, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         Memory mem = new Memory(executeString.length() * 2 + 2);
         mem.setWideString(0L, executeString);
         this.clientTransaction(mem, (int)mem.size(), (Ddeml.HSZ)null, 0, 16464, timeout, result, userHandle);
      }

      @Override
      public void advstart(Ddeml.HSZ item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         this.clientTransaction(Pointer.NULL, 0, item, wFmt, 4144, timeout, result, userHandle);
      }

      @Override
      public void advstart(String item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         Ddeml.HSZ itemHSZ = null;

         try {
            itemHSZ = this.client.createStringHandle(item);
            this.advstart(itemHSZ, wFmt, timeout, result, userHandle);
         } finally {
            this.client.freeStringHandle(itemHSZ);
         }
      }

      @Override
      public void advstop(Ddeml.HSZ item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         this.clientTransaction(Pointer.NULL, 0, item, wFmt, 32832, timeout, result, userHandle);
      }

      @Override
      public void advstop(String item, int wFmt, int timeout, WinDef.DWORDByReference result, BaseTSD.DWORD_PTR userHandle) {
         Ddeml.HSZ itemHSZ = null;

         try {
            itemHSZ = this.client.createStringHandle(item);
            this.advstop(itemHSZ, wFmt, timeout, result, userHandle);
         } finally {
            this.client.freeStringHandle(itemHSZ);
         }
      }

      @Override
      public void impersonateClient() {
         boolean result = Ddeml.INSTANCE.DdeImpersonateClient(this.conv);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }

      @Override
      public void close() {
         boolean result = Ddeml.INSTANCE.DdeDisconnect(this.conv);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }

      @Override
      public void reconnect() {
         Ddeml.HCONV newConv = Ddeml.INSTANCE.DdeReconnect(this.conv);
         if (newConv != null) {
            this.conv = newConv;
         } else {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }

      @Override
      public boolean enableCallback(int wCmd) {
         boolean result = Ddeml.INSTANCE.DdeEnableCallback(this.client.getInstanceIdentitifier(), this.conv, wCmd);
         if (!result && wCmd == 2) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         } else {
            return result;
         }
      }

      @Override
      public void setUserHandle(int id, BaseTSD.DWORD_PTR hUser) throws DdemlUtil.DdemlException {
         boolean result = Ddeml.INSTANCE.DdeSetUserHandle(this.conv, id, hUser);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }

      @Override
      public Ddeml.CONVINFO queryConvInfo(int idTransaction) throws DdemlUtil.DdemlException {
         Ddeml.CONVINFO convInfo = new Ddeml.CONVINFO();
         convInfo.cb = convInfo.size();
         convInfo.ConvCtxt.cb = convInfo.ConvCtxt.size();
         convInfo.write();
         int result = Ddeml.INSTANCE.DdeQueryConvInfo(this.conv, idTransaction, convInfo);
         if (result == 0) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         } else {
            return convInfo;
         }
      }
   }

   public static class DdeConnectionList implements DdemlUtil.IDdeConnectionList {
      private final DdemlUtil.IDdeClient client;
      private final Ddeml.HCONVLIST convList;

      public DdeConnectionList(DdemlUtil.IDdeClient client, Ddeml.HCONVLIST convList) {
         this.convList = convList;
         this.client = client;
      }

      @Override
      public Ddeml.HCONVLIST getHandle() {
         return this.convList;
      }

      @Override
      public DdemlUtil.IDdeConnection queryNextServer(DdemlUtil.IDdeConnection prevConnection) {
         Ddeml.HCONV conv = Ddeml.INSTANCE.DdeQueryNextServer(this.convList, prevConnection != null ? prevConnection.getConv() : null);
         return conv != null ? new DdemlUtil.DdeConnection(this.client, conv) : null;
      }

      @Override
      public void close() {
         boolean result = Ddeml.INSTANCE.DdeDisconnectList(this.convList);
         if (!result) {
            throw DdemlUtil.DdemlException.create(this.client.getLastError());
         }
      }
   }

   public static class DdemlException extends RuntimeException {
      private static final Map<Integer, String> ERROR_CODE_MAP;
      private final int errorCode;

      public static DdemlUtil.DdemlException create(int errorCode) {
         String errorName = ERROR_CODE_MAP.get(errorCode);
         return new DdemlUtil.DdemlException(errorCode, String.format("%s (Code: 0x%X)", errorName != null ? errorName : "", errorCode));
      }

      public DdemlException(int errorCode, String message) {
         super(message);
         this.errorCode = errorCode;
      }

      public int getErrorCode() {
         return this.errorCode;
      }

      static {
         Map<Integer, String> errorCodeMapBuilder = new HashMap<>();

         for (Field f : Ddeml.class.getFields()) {
            String name = f.getName();
            if (name.startsWith("DMLERR_") && !name.equals("DMLERR_FIRST") && !name.equals("DMLERR_LAST")) {
               try {
                  errorCodeMapBuilder.put(f.getInt(null), name);
               } catch (IllegalArgumentException var7) {
                  throw new RuntimeException(var7);
               } catch (IllegalAccessException var8) {
                  throw new RuntimeException(var8);
               }
            }
         }

         ERROR_CODE_MAP = Collections.unmodifiableMap(errorCodeMapBuilder);
      }
   }

   public interface DisconnectHandler {
      void onDisconnect(int var1, Ddeml.HCONV var2, boolean var3);
   }

   public interface ErrorHandler {
      void onError(int var1, Ddeml.HCONV var2, int var3);
   }

   public interface ExecuteHandler {
      int onExecute(int var1, Ddeml.HCONV var2, Ddeml.HSZ var3, Ddeml.HDDEDATA var4);
   }

   public interface IDdeClient extends Closeable {
      Integer getInstanceIdentitifier();

      void initialize(int var1) throws DdemlUtil.DdemlException;

      Ddeml.HSZ createStringHandle(String var1) throws DdemlUtil.DdemlException;

      String queryString(Ddeml.HSZ var1) throws DdemlUtil.DdemlException;

      boolean freeStringHandle(Ddeml.HSZ var1);

      boolean keepStringHandle(Ddeml.HSZ var1);

      void nameService(Ddeml.HSZ var1, int var2) throws DdemlUtil.DdemlException;

      void nameService(String var1, int var2) throws DdemlUtil.DdemlException;

      int getLastError();

      DdemlUtil.IDdeConnection connect(Ddeml.HSZ var1, Ddeml.HSZ var2, Ddeml.CONVCONTEXT var3);

      DdemlUtil.IDdeConnection connect(String var1, String var2, Ddeml.CONVCONTEXT var3);

      Ddeml.HDDEDATA createDataHandle(Pointer var1, int var2, int var3, Ddeml.HSZ var4, int var5, int var6);

      void freeDataHandle(Ddeml.HDDEDATA var1);

      Ddeml.HDDEDATA addData(Ddeml.HDDEDATA var1, Pointer var2, int var3, int var4);

      int getData(Ddeml.HDDEDATA var1, Pointer var2, int var3, int var4);

      Pointer accessData(Ddeml.HDDEDATA var1, WinDef.DWORDByReference var2);

      void unaccessData(Ddeml.HDDEDATA var1);

      void postAdvise(Ddeml.HSZ var1, Ddeml.HSZ var2);

      void postAdvise(String var1, String var2);

      void abandonTransactions();

      DdemlUtil.IDdeConnectionList connectList(Ddeml.HSZ var1, Ddeml.HSZ var2, DdemlUtil.IDdeConnectionList var3, Ddeml.CONVCONTEXT var4);

      DdemlUtil.IDdeConnectionList connectList(String var1, String var2, DdemlUtil.IDdeConnectionList var3, Ddeml.CONVCONTEXT var4);

      boolean enableCallback(int var1);

      boolean uninitialize();

      DdemlUtil.IDdeConnection wrap(Ddeml.HCONV var1);

      void registerAdvstartHandler(DdemlUtil.AdvstartHandler var1);

      void unregisterAdvstartHandler(DdemlUtil.AdvstartHandler var1);

      void registerAdvstopHandler(DdemlUtil.AdvstopHandler var1);

      void unregisterAdvstopHandler(DdemlUtil.AdvstopHandler var1);

      void registerConnectHandler(DdemlUtil.ConnectHandler var1);

      void unregisterConnectHandler(DdemlUtil.ConnectHandler var1);

      void registerAdvReqHandler(DdemlUtil.AdvreqHandler var1);

      void unregisterAdvReqHandler(DdemlUtil.AdvreqHandler var1);

      void registerRequestHandler(DdemlUtil.RequestHandler var1);

      void unregisterRequestHandler(DdemlUtil.RequestHandler var1);

      void registerWildconnectHandler(DdemlUtil.WildconnectHandler var1);

      void unregisterWildconnectHandler(DdemlUtil.WildconnectHandler var1);

      void registerAdvdataHandler(DdemlUtil.AdvdataHandler var1);

      void unregisterAdvdataHandler(DdemlUtil.AdvdataHandler var1);

      void registerExecuteHandler(DdemlUtil.ExecuteHandler var1);

      void unregisterExecuteHandler(DdemlUtil.ExecuteHandler var1);

      void registerPokeHandler(DdemlUtil.PokeHandler var1);

      void unregisterPokeHandler(DdemlUtil.PokeHandler var1);

      void registerConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler var1);

      void unregisterConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler var1);

      void registerDisconnectHandler(DdemlUtil.DisconnectHandler var1);

      void unregisterDisconnectHandler(DdemlUtil.DisconnectHandler var1);

      void registerErrorHandler(DdemlUtil.ErrorHandler var1);

      void unregisterErrorHandler(DdemlUtil.ErrorHandler var1);

      void registerRegisterHandler(DdemlUtil.RegisterHandler var1);

      void unregisterRegisterHandler(DdemlUtil.RegisterHandler var1);

      void registerXactCompleteHandler(DdemlUtil.XactCompleteHandler var1);

      void unregisterXactCompleteHandler(DdemlUtil.XactCompleteHandler var1);

      void registerUnregisterHandler(DdemlUtil.UnregisterHandler var1);

      void unregisterUnregisterHandler(DdemlUtil.UnregisterHandler var1);

      void registerMonitorHandler(DdemlUtil.MonitorHandler var1);

      void unregisterMonitorHandler(DdemlUtil.MonitorHandler var1);
   }

   public interface IDdeConnection extends Closeable {
      Ddeml.HCONV getConv();

      void execute(String var1, int var2, WinDef.DWORDByReference var3, BaseTSD.DWORD_PTR var4);

      void poke(Pointer var1, int var2, Ddeml.HSZ var3, int var4, int var5, WinDef.DWORDByReference var6, BaseTSD.DWORD_PTR var7);

      void poke(Pointer var1, int var2, String var3, int var4, int var5, WinDef.DWORDByReference var6, BaseTSD.DWORD_PTR var7);

      Ddeml.HDDEDATA request(Ddeml.HSZ var1, int var2, int var3, WinDef.DWORDByReference var4, BaseTSD.DWORD_PTR var5);

      Ddeml.HDDEDATA request(String var1, int var2, int var3, WinDef.DWORDByReference var4, BaseTSD.DWORD_PTR var5);

      Ddeml.HDDEDATA clientTransaction(
         Pointer var1, int var2, Ddeml.HSZ var3, int var4, int var5, int var6, WinDef.DWORDByReference var7, BaseTSD.DWORD_PTR var8
      );

      Ddeml.HDDEDATA clientTransaction(Pointer var1, int var2, String var3, int var4, int var5, int var6, WinDef.DWORDByReference var7, BaseTSD.DWORD_PTR var8);

      void advstart(Ddeml.HSZ var1, int var2, int var3, WinDef.DWORDByReference var4, BaseTSD.DWORD_PTR var5);

      void advstart(String var1, int var2, int var3, WinDef.DWORDByReference var4, BaseTSD.DWORD_PTR var5);

      void advstop(Ddeml.HSZ var1, int var2, int var3, WinDef.DWORDByReference var4, BaseTSD.DWORD_PTR var5);

      void advstop(String var1, int var2, int var3, WinDef.DWORDByReference var4, BaseTSD.DWORD_PTR var5);

      void abandonTransaction(int var1);

      void abandonTransactions();

      void impersonateClient();

      @Override
      void close();

      void reconnect();

      boolean enableCallback(int var1);

      void setUserHandle(int var1, BaseTSD.DWORD_PTR var2) throws DdemlUtil.DdemlException;

      Ddeml.CONVINFO queryConvInfo(int var1) throws DdemlUtil.DdemlException;
   }

   public interface IDdeConnectionList extends Closeable {
      Ddeml.HCONVLIST getHandle();

      DdemlUtil.IDdeConnection queryNextServer(DdemlUtil.IDdeConnection var1);

      @Override
      void close();
   }

   private static class MessageLoopWrapper implements InvocationHandler {
      private final Object delegate;
      private final User32Util.MessageLoopThread loopThread;

      public MessageLoopWrapper(User32Util.MessageLoopThread thread, Object delegate) {
         this.loopThread = thread;
         this.delegate = delegate;
      }

      @Override
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         try {
            Object result = method.invoke(this.delegate, args);
            Class<?> wrapClass = null;
            if (result instanceof DdemlUtil.IDdeConnection) {
               wrapClass = DdemlUtil.IDdeConnection.class;
            } else if (result instanceof DdemlUtil.IDdeConnectionList) {
               wrapClass = DdemlUtil.IDdeConnectionList.class;
            } else if (result instanceof DdemlUtil.IDdeClient) {
               wrapClass = DdemlUtil.IDdeClient.class;
            }

            if (wrapClass != null && method.getReturnType().isAssignableFrom(wrapClass)) {
               result = this.wrap(result, wrapClass);
            }

            return result;
         } catch (InvocationTargetException var6) {
            Throwable cause = var6.getCause();
            if (cause instanceof Exception) {
               throw (Exception)cause;
            } else {
               throw var6;
            }
         }
      }

      private <V> V wrap(V delegate, Class clazz) {
         V messageLoopHandler = (V)Proxy.newProxyInstance(
            DdemlUtil.StandaloneDdeClient.class.getClassLoader(), new Class[]{clazz}, this.loopThread.new Handler(delegate)
         );
         return (V)Proxy.newProxyInstance(
            DdemlUtil.StandaloneDdeClient.class.getClassLoader(), new Class[]{clazz}, new DdemlUtil.MessageLoopWrapper(this.loopThread, messageLoopHandler)
         );
      }
   }

   public interface MonitorHandler {
      void onMonitor(int var1, Ddeml.HDDEDATA var2, int var3);
   }

   public interface PokeHandler {
      int onPoke(int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5, Ddeml.HDDEDATA var6);
   }

   public interface RegisterHandler {
      void onRegister(int var1, Ddeml.HSZ var2, Ddeml.HSZ var3);
   }

   public interface RequestHandler {
      Ddeml.HDDEDATA onRequest(int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5);
   }

   public static class StandaloneDdeClient implements DdemlUtil.IDdeClient, Closeable {
      private final User32Util.MessageLoopThread messageLoop = new User32Util.MessageLoopThread();
      private final DdemlUtil.IDdeClient ddeClient = new DdemlUtil.DdeClient();
      private final DdemlUtil.IDdeClient clientDelegate;

      public StandaloneDdeClient() {
         DdemlUtil.IDdeClient messageLoopHandler = (DdemlUtil.IDdeClient)Proxy.newProxyInstance(
            DdemlUtil.StandaloneDdeClient.class.getClassLoader(), new Class[]{DdemlUtil.IDdeClient.class}, this.messageLoop.new Handler(this.ddeClient)
         );
         this.clientDelegate = (DdemlUtil.IDdeClient)Proxy.newProxyInstance(
            DdemlUtil.StandaloneDdeClient.class.getClassLoader(),
            new Class[]{DdemlUtil.IDdeClient.class},
            new DdemlUtil.MessageLoopWrapper(this.messageLoop, messageLoopHandler)
         );
         this.messageLoop.setDaemon(true);
         this.messageLoop.start();
      }

      @Override
      public Integer getInstanceIdentitifier() {
         return this.ddeClient.getInstanceIdentitifier();
      }

      @Override
      public void initialize(int afCmd) throws DdemlUtil.DdemlException {
         this.clientDelegate.initialize(afCmd);
      }

      @Override
      public Ddeml.HSZ createStringHandle(String value) throws DdemlUtil.DdemlException {
         return this.clientDelegate.createStringHandle(value);
      }

      @Override
      public void nameService(Ddeml.HSZ name, int afCmd) throws DdemlUtil.DdemlException {
         this.clientDelegate.nameService(name, afCmd);
      }

      @Override
      public int getLastError() {
         return this.clientDelegate.getLastError();
      }

      @Override
      public DdemlUtil.IDdeConnection connect(Ddeml.HSZ service, Ddeml.HSZ topic, Ddeml.CONVCONTEXT convcontext) {
         return this.clientDelegate.connect(service, topic, convcontext);
      }

      @Override
      public String queryString(Ddeml.HSZ value) throws DdemlUtil.DdemlException {
         return this.clientDelegate.queryString(value);
      }

      @Override
      public Ddeml.HDDEDATA createDataHandle(Pointer pSrc, int cb, int cbOff, Ddeml.HSZ hszItem, int wFmt, int afCmd) {
         return this.clientDelegate.createDataHandle(pSrc, cb, cbOff, hszItem, wFmt, afCmd);
      }

      @Override
      public void freeDataHandle(Ddeml.HDDEDATA hData) {
         this.clientDelegate.freeDataHandle(hData);
      }

      @Override
      public Ddeml.HDDEDATA addData(Ddeml.HDDEDATA hData, Pointer pSrc, int cb, int cbOff) {
         return this.clientDelegate.addData(hData, pSrc, cb, cbOff);
      }

      @Override
      public int getData(Ddeml.HDDEDATA hData, Pointer pDst, int cbMax, int cbOff) {
         return this.clientDelegate.getData(hData, pDst, cbMax, cbOff);
      }

      @Override
      public Pointer accessData(Ddeml.HDDEDATA hData, WinDef.DWORDByReference pcbDataSize) {
         return this.clientDelegate.accessData(hData, pcbDataSize);
      }

      @Override
      public void unaccessData(Ddeml.HDDEDATA hData) {
         this.clientDelegate.unaccessData(hData);
      }

      @Override
      public void postAdvise(Ddeml.HSZ hszTopic, Ddeml.HSZ hszItem) {
         this.clientDelegate.postAdvise(hszTopic, hszItem);
      }

      @Override
      public void close() throws IOException {
         this.clientDelegate.uninitialize();
         this.messageLoop.exit();
      }

      @Override
      public boolean freeStringHandle(Ddeml.HSZ value) {
         return this.clientDelegate.freeStringHandle(value);
      }

      @Override
      public boolean keepStringHandle(Ddeml.HSZ value) {
         return this.clientDelegate.keepStringHandle(value);
      }

      @Override
      public void abandonTransactions() {
         this.clientDelegate.abandonTransactions();
      }

      @Override
      public DdemlUtil.IDdeConnectionList connectList(Ddeml.HSZ service, Ddeml.HSZ topic, DdemlUtil.IDdeConnectionList existingList, Ddeml.CONVCONTEXT ctx) {
         return this.clientDelegate.connectList(service, topic, existingList, ctx);
      }

      @Override
      public boolean enableCallback(int wCmd) {
         return this.clientDelegate.enableCallback(wCmd);
      }

      @Override
      public DdemlUtil.IDdeConnection wrap(Ddeml.HCONV conv) {
         return this.clientDelegate.wrap(conv);
      }

      @Override
      public DdemlUtil.IDdeConnection connect(String service, String topic, Ddeml.CONVCONTEXT convcontext) {
         return this.clientDelegate.connect(service, topic, convcontext);
      }

      @Override
      public boolean uninitialize() {
         return this.clientDelegate.uninitialize();
      }

      @Override
      public void postAdvise(String hszTopic, String hszItem) {
         this.clientDelegate.postAdvise(hszTopic, hszItem);
      }

      @Override
      public DdemlUtil.IDdeConnectionList connectList(String service, String topic, DdemlUtil.IDdeConnectionList existingList, Ddeml.CONVCONTEXT ctx) {
         return this.clientDelegate.connectList(service, topic, existingList, ctx);
      }

      @Override
      public void nameService(String name, int afCmd) throws DdemlUtil.DdemlException {
         this.clientDelegate.nameService(name, afCmd);
      }

      @Override
      public void registerAdvstartHandler(DdemlUtil.AdvstartHandler handler) {
         this.clientDelegate.registerAdvstartHandler(handler);
      }

      @Override
      public void unregisterAdvstartHandler(DdemlUtil.AdvstartHandler handler) {
         this.clientDelegate.unregisterAdvstartHandler(handler);
      }

      @Override
      public void registerAdvstopHandler(DdemlUtil.AdvstopHandler handler) {
         this.clientDelegate.registerAdvstopHandler(handler);
      }

      @Override
      public void unregisterAdvstopHandler(DdemlUtil.AdvstopHandler handler) {
         this.clientDelegate.unregisterAdvstopHandler(handler);
      }

      @Override
      public void registerConnectHandler(DdemlUtil.ConnectHandler handler) {
         this.clientDelegate.registerConnectHandler(handler);
      }

      @Override
      public void unregisterConnectHandler(DdemlUtil.ConnectHandler handler) {
         this.clientDelegate.unregisterConnectHandler(handler);
      }

      @Override
      public void registerAdvReqHandler(DdemlUtil.AdvreqHandler handler) {
         this.clientDelegate.registerAdvReqHandler(handler);
      }

      @Override
      public void unregisterAdvReqHandler(DdemlUtil.AdvreqHandler handler) {
         this.clientDelegate.unregisterAdvReqHandler(handler);
      }

      @Override
      public void registerRequestHandler(DdemlUtil.RequestHandler handler) {
         this.clientDelegate.registerRequestHandler(handler);
      }

      @Override
      public void unregisterRequestHandler(DdemlUtil.RequestHandler handler) {
         this.clientDelegate.unregisterRequestHandler(handler);
      }

      @Override
      public void registerWildconnectHandler(DdemlUtil.WildconnectHandler handler) {
         this.clientDelegate.registerWildconnectHandler(handler);
      }

      @Override
      public void unregisterWildconnectHandler(DdemlUtil.WildconnectHandler handler) {
         this.clientDelegate.unregisterWildconnectHandler(handler);
      }

      @Override
      public void registerAdvdataHandler(DdemlUtil.AdvdataHandler handler) {
         this.clientDelegate.registerAdvdataHandler(handler);
      }

      @Override
      public void unregisterAdvdataHandler(DdemlUtil.AdvdataHandler handler) {
         this.clientDelegate.unregisterAdvdataHandler(handler);
      }

      @Override
      public void registerExecuteHandler(DdemlUtil.ExecuteHandler handler) {
         this.clientDelegate.registerExecuteHandler(handler);
      }

      @Override
      public void unregisterExecuteHandler(DdemlUtil.ExecuteHandler handler) {
         this.clientDelegate.unregisterExecuteHandler(handler);
      }

      @Override
      public void registerPokeHandler(DdemlUtil.PokeHandler handler) {
         this.clientDelegate.registerPokeHandler(handler);
      }

      @Override
      public void unregisterPokeHandler(DdemlUtil.PokeHandler handler) {
         this.clientDelegate.unregisterPokeHandler(handler);
      }

      @Override
      public void registerConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler handler) {
         this.clientDelegate.registerConnectConfirmHandler(handler);
      }

      @Override
      public void unregisterConnectConfirmHandler(DdemlUtil.ConnectConfirmHandler handler) {
         this.clientDelegate.unregisterConnectConfirmHandler(handler);
      }

      @Override
      public void registerDisconnectHandler(DdemlUtil.DisconnectHandler handler) {
         this.clientDelegate.registerDisconnectHandler(handler);
      }

      @Override
      public void unregisterDisconnectHandler(DdemlUtil.DisconnectHandler handler) {
         this.clientDelegate.unregisterDisconnectHandler(handler);
      }

      @Override
      public void registerErrorHandler(DdemlUtil.ErrorHandler handler) {
         this.clientDelegate.registerErrorHandler(handler);
      }

      @Override
      public void unregisterErrorHandler(DdemlUtil.ErrorHandler handler) {
         this.clientDelegate.unregisterErrorHandler(handler);
      }

      @Override
      public void registerRegisterHandler(DdemlUtil.RegisterHandler handler) {
         this.clientDelegate.registerRegisterHandler(handler);
      }

      @Override
      public void unregisterRegisterHandler(DdemlUtil.RegisterHandler handler) {
         this.clientDelegate.unregisterRegisterHandler(handler);
      }

      @Override
      public void registerXactCompleteHandler(DdemlUtil.XactCompleteHandler handler) {
         this.clientDelegate.registerXactCompleteHandler(handler);
      }

      @Override
      public void unregisterXactCompleteHandler(DdemlUtil.XactCompleteHandler handler) {
         this.clientDelegate.unregisterXactCompleteHandler(handler);
      }

      @Override
      public void registerUnregisterHandler(DdemlUtil.UnregisterHandler handler) {
         this.clientDelegate.registerUnregisterHandler(handler);
      }

      @Override
      public void unregisterUnregisterHandler(DdemlUtil.UnregisterHandler handler) {
         this.clientDelegate.unregisterUnregisterHandler(handler);
      }

      @Override
      public void registerMonitorHandler(DdemlUtil.MonitorHandler handler) {
         this.clientDelegate.registerMonitorHandler(handler);
      }

      @Override
      public void unregisterMonitorHandler(DdemlUtil.MonitorHandler handler) {
         this.clientDelegate.unregisterMonitorHandler(handler);
      }
   }

   public interface UnregisterHandler {
      void onUnregister(int var1, Ddeml.HSZ var2, Ddeml.HSZ var3);
   }

   public interface WildconnectHandler {
      List<Ddeml.HSZPAIR> onWildconnect(int var1, Ddeml.HSZ var2, Ddeml.HSZ var3, Ddeml.CONVCONTEXT var4, boolean var5);
   }

   public interface XactCompleteHandler {
      void onXactComplete(
         int var1, int var2, Ddeml.HCONV var3, Ddeml.HSZ var4, Ddeml.HSZ var5, Ddeml.HDDEDATA var6, BaseTSD.ULONG_PTR var7, BaseTSD.ULONG_PTR var8
      );
   }
}
