package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.wmi.Win32VideoController;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Triplet;

@Immutable
final class WindowsGraphicsCard extends AbstractGraphicsCard {
   private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

   WindowsGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
      super(name, deviceId, vendor, versionInfo, vram);
   }

   public static List<GraphicsCard> getGraphicsCards() {
      List<GraphicsCard> cardList = new ArrayList<>();
      if (IS_VISTA_OR_GREATER) {
         WbemcliUtil.WmiResult<Win32VideoController.VideoControllerProperty> cards = Win32VideoController.queryVideoController();

         for (int index = 0; index < cards.getResultCount(); index++) {
            String name = WmiUtil.getString(cards, Win32VideoController.VideoControllerProperty.NAME, index);
            Triplet<String, String, String> idPair = ParseUtil.parseDeviceIdToVendorProductSerial(
               WmiUtil.getString(cards, Win32VideoController.VideoControllerProperty.PNPDEVICEID, index)
            );
            String deviceId = idPair == null ? "unknown" : idPair.getB();
            String vendor = WmiUtil.getString(cards, Win32VideoController.VideoControllerProperty.ADAPTERCOMPATIBILITY, index);
            if (idPair != null) {
               if (Util.isBlank(vendor)) {
                  deviceId = idPair.getA();
               } else {
                  vendor = vendor + " (" + idPair.getA() + ")";
               }
            }

            String versionInfo = WmiUtil.getString(cards, Win32VideoController.VideoControllerProperty.DRIVERVERSION, index);
            if (!Util.isBlank(versionInfo)) {
               versionInfo = "DriverVersion=" + versionInfo;
            } else {
               versionInfo = "unknown";
            }

            long vram = WmiUtil.getUint32asLong(cards, Win32VideoController.VideoControllerProperty.ADAPTERRAM, index);
            cardList.add(new WindowsGraphicsCard(Util.isBlank(name) ? "unknown" : name, deviceId, Util.isBlank(vendor) ? "unknown" : vendor, versionInfo, vram));
         }
      }

      return cardList;
   }
}
