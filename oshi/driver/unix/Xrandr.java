package oshi.driver.unix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@ThreadSafe
public final class Xrandr {
   private static final String[] XRANDR_VERBOSE = new String[]{"xrandr", "--verbose"};

   private Xrandr() {
   }

   public static List<byte[]> getEdidArrays() {
      List<String> xrandr = ExecutingCommand.runNative(XRANDR_VERBOSE, null);
      if (xrandr.isEmpty()) {
         return Collections.emptyList();
      } else {
         List<byte[]> displays = new ArrayList<>();
         StringBuilder sb = null;

         for (String s : xrandr) {
            if (s.contains("EDID")) {
               sb = new StringBuilder();
            } else if (sb != null) {
               sb.append(s.trim());
               if (sb.length() >= 256) {
                  String edidStr = sb.toString();
                  byte[] edid = ParseUtil.hexStringToByteArray(edidStr);
                  if (edid.length >= 128) {
                     displays.add(edid);
                  }

                  sb = null;
               }
            }
         }

         return displays;
      }
   }
}
