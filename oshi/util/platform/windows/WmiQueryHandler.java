package oshi.util.platform.windows;

import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

@ThreadSafe
public class WmiQueryHandler {
   private static final Logger LOG = LoggerFactory.getLogger(WmiQueryHandler.class);
   private static int globalTimeout = GlobalConfig.get("oshi.util.wmi.timeout", -1);
   private int wmiTimeout = globalTimeout;
   private final Set<String> failedWmiClassNames = new HashSet<>();
   private int comThreading = 0;
   private boolean securityInitialized = false;
   private static final Class<?>[] EMPTY_CLASS_ARRAY;
   private static final Object[] EMPTY_OBJECT_ARRAY;
   private static Class<? extends WmiQueryHandler> customClass;

   protected WmiQueryHandler() {
   }

   public static synchronized WmiQueryHandler createInstance() {
      if (customClass == null) {
         return new WmiQueryHandler();
      } else {
         try {
            return customClass.getConstructor(EMPTY_CLASS_ARRAY).newInstance(EMPTY_OBJECT_ARRAY);
         } catch (SecurityException | NoSuchMethodException var1) {
            LOG.error("Failed to find or access a no-arg constructor for {}", customClass);
         } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException var2) {
            LOG.error("Failed to create a new instance of {}", customClass);
         }

         return null;
      }
   }

   public static synchronized void setInstanceClass(Class<? extends WmiQueryHandler> instanceClass) {
      customClass = instanceClass;
   }

   public <T extends Enum<T>> WbemcliUtil.WmiResult<T> queryWMI(WbemcliUtil.WmiQuery<T> query) {
      return this.queryWMI(query, true);
   }

   public <T extends Enum<T>> WbemcliUtil.WmiResult<T> queryWMI(WbemcliUtil.WmiQuery<T> query, boolean initCom) {
      WbemcliUtil.WmiResult<T> result = WbemcliUtil.INSTANCE.new WmiResult<>(query.getPropertyEnum());
      if (this.failedWmiClassNames.contains(query.getWmiClassName())) {
         return result;
      } else {
         boolean comInit = false;

         try {
            if (initCom) {
               comInit = this.initCOM();
            }

            result = query.execute(this.wmiTimeout);
         } catch (COMException var11) {
            if (!"ROOT\\OpenHardwareMonitor".equals(query.getNameSpace())) {
               int hresult = var11.getHresult() == null ? -1 : var11.getHresult().intValue();
               switch (hresult) {
                  case -2147217394:
                     LOG.warn("COM exception: Invalid Namespace {}", query.getNameSpace());
                     break;
                  case -2147217392:
                     LOG.warn("COM exception: Invalid Class {}", query.getWmiClassName());
                     break;
                  case -2147217385:
                     LOG.warn("COM exception: Invalid Query: {}", WmiUtil.queryToString(query));
                     break;
                  default:
                     this.handleComException(query, var11);
               }

               this.failedWmiClassNames.add(query.getWmiClassName());
            }
         } catch (TimeoutException var12) {
            LOG.warn("WMI query timed out after {} ms: {}", this.wmiTimeout, WmiUtil.queryToString(query));
         } finally {
            if (comInit) {
               this.unInitCOM();
            }
         }

         return result;
      }
   }

   protected void handleComException(WbemcliUtil.WmiQuery<?> query, COMException ex) {
      LOG.warn(
         "COM exception querying {}, which might not be on your system. Will not attempt to query it again. Error was {}: {}",
         query.getWmiClassName(),
         ex.getHresult() == null ? null : ex.getHresult().intValue(),
         ex.getMessage()
      );
   }

   public boolean initCOM() {
      boolean comInit = false;
      comInit = this.initCOM(this.getComThreading());
      if (!comInit) {
         comInit = this.initCOM(this.switchComThreading());
      }

      if (comInit && !this.isSecurityInitialized()) {
         WinNT.HRESULT hres = Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null, 0, 3, null, 0, null);
         if (COMUtils.FAILED(hres) && hres.intValue() != -2147417831) {
            Ole32.INSTANCE.CoUninitialize();
            throw new COMException("Failed to initialize security.", hres);
         }

         this.securityInitialized = true;
      }

      return comInit;
   }

   protected boolean initCOM(int coInitThreading) {
      WinNT.HRESULT hres = Ole32.INSTANCE.CoInitializeEx(null, coInitThreading);
      switch (hres.intValue()) {
         case -2147417850:
            return false;
         case 0:
         case 1:
            return true;
         default:
            throw new COMException("Failed to initialize COM library.", hres);
      }
   }

   public void unInitCOM() {
      Ole32.INSTANCE.CoUninitialize();
   }

   public int getComThreading() {
      return this.comThreading;
   }

   public int switchComThreading() {
      if (this.comThreading == 2) {
         this.comThreading = 0;
      } else {
         this.comThreading = 2;
      }

      return this.comThreading;
   }

   public boolean isSecurityInitialized() {
      return this.securityInitialized;
   }

   public int getWmiTimeout() {
      return this.wmiTimeout;
   }

   public void setWmiTimeout(int wmiTimeout) {
      this.wmiTimeout = wmiTimeout;
   }

   static {
      if (globalTimeout != 0 && globalTimeout >= -1) {
         EMPTY_CLASS_ARRAY = new Class[0];
         EMPTY_OBJECT_ARRAY = new Object[0];
         customClass = null;
      } else {
         throw new GlobalConfig.PropertyException("oshi.util.wmi.timeout");
      }
   }
}
