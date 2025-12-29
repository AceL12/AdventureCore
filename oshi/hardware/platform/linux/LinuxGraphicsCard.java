package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@Immutable
final class LinuxGraphicsCard extends AbstractGraphicsCard {
   LinuxGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
      super(name, deviceId, vendor, versionInfo, vram);
   }

   public static List<GraphicsCard> getGraphicsCards() {
      List<GraphicsCard> cardList = getGraphicsCardsFromLspci();
      if (cardList.isEmpty()) {
         cardList = getGraphicsCardsFromLshw();
      }

      return cardList;
   }

   private static List<GraphicsCard> getGraphicsCardsFromLspci() {
      List<GraphicsCard> cardList = new ArrayList<>();
      List<String> lspci = ExecutingCommand.runNative("lspci -vnnm");
      String name = "unknown";
      String deviceId = "unknown";
      String vendor = "unknown";
      List<String> versionInfoList = new ArrayList<>();
      boolean found = false;
      String lookupDevice = null;

      for (String line : lspci) {
         String[] split = line.trim().split(":", 2);
         String prefix = split[0];
         if (prefix.equals("Class") && line.contains("VGA")) {
            found = true;
         } else if (prefix.equals("Device") && !found && split.length > 1) {
            lookupDevice = split[1].trim();
         }

         if (found) {
            if (split.length < 2) {
               cardList.add(
                  new LinuxGraphicsCard(
                     name, deviceId, vendor, versionInfoList.isEmpty() ? "unknown" : String.join(", ", versionInfoList), queryLspciMemorySize(lookupDevice)
                  )
               );
               versionInfoList.clear();
               found = false;
            } else if (prefix.equals("Device")) {
               Pair<String, String> pair = ParseUtil.parseLspciMachineReadable(split[1].trim());
               if (pair != null) {
                  name = pair.getA();
                  deviceId = "0x" + pair.getB();
               }
            } else if (prefix.equals("Vendor")) {
               Pair<String, String> pair = ParseUtil.parseLspciMachineReadable(split[1].trim());
               if (pair != null) {
                  vendor = pair.getA() + " (0x" + pair.getB() + ")";
               } else {
                  vendor = split[1].trim();
               }
            } else if (prefix.equals("Rev:")) {
               versionInfoList.add(line.trim());
            }
         }
      }

      if (found) {
         cardList.add(
            new LinuxGraphicsCard(
               name, deviceId, vendor, versionInfoList.isEmpty() ? "unknown" : String.join(", ", versionInfoList), queryLspciMemorySize(lookupDevice)
            )
         );
      }

      return cardList;
   }

   private static long queryLspciMemorySize(String lookupDevice) {
      long vram = 0L;

      for (String mem : ExecutingCommand.runNative("lspci -v -s " + lookupDevice)) {
         if (mem.contains(" prefetchable")) {
            vram += ParseUtil.parseLspciMemorySize(mem);
         }
      }

      return vram;
   }

   private static List<GraphicsCard> getGraphicsCardsFromLshw() {
      List<GraphicsCard> cardList = new ArrayList<>();
      List<String> lshw = ExecutingCommand.runNative("lshw -C display");
      String name = "unknown";
      String deviceId = "unknown";
      String vendor = "unknown";
      List<String> versionInfoList = new ArrayList<>();
      long vram = 0L;
      int cardNum = 0;

      for (String line : lshw) {
         String[] split = line.trim().split(":");
         if (split[0].startsWith("*-display")) {
            if (cardNum++ > 0) {
               cardList.add(new LinuxGraphicsCard(name, deviceId, vendor, versionInfoList.isEmpty() ? "unknown" : String.join(", ", versionInfoList), vram));
               versionInfoList.clear();
            }
         } else if (split.length == 2) {
            String prefix = split[0];
            if (prefix.equals("product")) {
               name = split[1].trim();
            } else if (prefix.equals("vendor")) {
               vendor = split[1].trim();
            } else if (prefix.equals("version")) {
               versionInfoList.add(line.trim());
            } else if (prefix.startsWith("resources")) {
               vram = ParseUtil.parseLshwResourceString(split[1].trim());
            }
         }
      }

      cardList.add(new LinuxGraphicsCard(name, deviceId, vendor, versionInfoList.isEmpty() ? "unknown" : String.join(", ", versionInfoList), vram));
      return cardList;
   }
}
