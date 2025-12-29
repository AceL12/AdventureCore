package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.jna.ByRef;
import oshi.jna.Struct;

@Immutable
final class WindowsDisplay extends AbstractDisplay {
   private static final Logger LOG = LoggerFactory.getLogger(WindowsDisplay.class);
   private static final SetupApi SU = SetupApi.INSTANCE;
   private static final Advapi32 ADV = Advapi32.INSTANCE;
   private static final Guid.GUID GUID_DEVINTERFACE_MONITOR = new Guid.GUID("E6F07B5F-EE97-4a90-B076-33F57BF4EAA7");

   WindowsDisplay(byte[] edid) {
      super(edid);
      LOG.debug("Initialized WindowsDisplay");
   }

   public static List<Display> getDisplays() {
      List<Display> displays = new ArrayList<>();
      WinNT.HANDLE hDevInfo = SU.SetupDiGetClassDevs(GUID_DEVINTERFACE_MONITOR, null, null, 18);
      if (!hDevInfo.equals(WinBase.INVALID_HANDLE_VALUE)) {
         Struct.CloseableSpDeviceInterfaceData deviceInterfaceData = new Struct.CloseableSpDeviceInterfaceData();

         try {
            Struct.CloseableSpDevinfoData info = new Struct.CloseableSpDevinfoData();

            try {
               deviceInterfaceData.cbSize = deviceInterfaceData.size();

               for (int memberIndex = 0; SU.SetupDiEnumDeviceInfo(hDevInfo, memberIndex, info); memberIndex++) {
                  WinReg.HKEY key = SU.SetupDiOpenDevRegKey(hDevInfo, info, 1, 0, 1, 1);
                  byte[] edid = new byte[1];
                  ByRef.CloseableIntByReference pType = new ByRef.CloseableIntByReference();

                  try {
                     ByRef.CloseableIntByReference lpcbData = new ByRef.CloseableIntByReference();

                     try {
                        if (ADV.RegQueryValueEx(key, "EDID", 0, pType, edid, lpcbData) == 234) {
                           edid = new byte[lpcbData.getValue()];
                           if (ADV.RegQueryValueEx(key, "EDID", 0, pType, edid, lpcbData) == 0) {
                              Display display = new WindowsDisplay(edid);
                              displays.add(display);
                           }
                        }
                     } catch (Throwable var15) {
                        try {
                           lpcbData.close();
                        } catch (Throwable var14) {
                           var15.addSuppressed(var14);
                        }

                        throw var15;
                     }

                     lpcbData.close();
                  } catch (Throwable var16) {
                     try {
                        pType.close();
                     } catch (Throwable var13) {
                        var16.addSuppressed(var13);
                     }

                     throw var16;
                  }

                  pType.close();
                  Advapi32.INSTANCE.RegCloseKey(key);
               }
            } catch (Throwable var17) {
               try {
                  info.close();
               } catch (Throwable var12) {
                  var17.addSuppressed(var12);
               }

               throw var17;
            }

            info.close();
         } catch (Throwable var18) {
            try {
               deviceInterfaceData.close();
            } catch (Throwable var11) {
               var18.addSuppressed(var11);
            }

            throw var18;
         }

         deviceInterfaceData.close();
         SU.SetupDiDestroyDeviceInfoList(hDevInfo);
      }

      return displays;
   }
}
