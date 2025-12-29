package oshi.hardware.platform.unix;

import java.util.List;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.Xrandr;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;

@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {
   UnixDisplay(byte[] edid) {
      super(edid);
   }

   public static List<Display> getDisplays() {
      return Xrandr.getEdidArrays().stream().map(UnixDisplay::new).collect(Collectors.toList());
   }
}
