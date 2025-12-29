package oshi.driver.mac;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.mac.CoreGraphics;
import oshi.software.os.OSDesktopWindow;
import oshi.util.FormatUtil;
import oshi.util.platform.mac.CFUtil;

@ThreadSafe
public final class WindowInfo {
   private WindowInfo() {
   }

   public static List<OSDesktopWindow> queryDesktopWindows(boolean visibleOnly) {
      CoreFoundation.CFArrayRef windowInfo = CoreGraphics.INSTANCE.CGWindowListCopyWindowInfo(visibleOnly ? 17 : 0, 0);
      int numWindows = windowInfo.getCount();
      List<OSDesktopWindow> windowList = new ArrayList<>();
      CoreFoundation.CFStringRef kCGWindowIsOnscreen = CoreFoundation.CFStringRef.createCFString("kCGWindowIsOnscreen");
      CoreFoundation.CFStringRef kCGWindowNumber = CoreFoundation.CFStringRef.createCFString("kCGWindowNumber");
      CoreFoundation.CFStringRef kCGWindowOwnerPID = CoreFoundation.CFStringRef.createCFString("kCGWindowOwnerPID");
      CoreFoundation.CFStringRef kCGWindowLayer = CoreFoundation.CFStringRef.createCFString("kCGWindowLayer");
      CoreFoundation.CFStringRef kCGWindowBounds = CoreFoundation.CFStringRef.createCFString("kCGWindowBounds");
      CoreFoundation.CFStringRef kCGWindowName = CoreFoundation.CFStringRef.createCFString("kCGWindowName");
      CoreFoundation.CFStringRef kCGWindowOwnerName = CoreFoundation.CFStringRef.createCFString("kCGWindowOwnerName");

      try {
         for (int i = 0; i < numWindows; i++) {
            Pointer result = windowInfo.getValueAtIndex(i);
            CoreFoundation.CFDictionaryRef windowRef = new CoreFoundation.CFDictionaryRef(result);
            result = windowRef.getValue(kCGWindowIsOnscreen);
            boolean visible = result == null || new CoreFoundation.CFBooleanRef(result).booleanValue();
            if (!visibleOnly || visible) {
               result = windowRef.getValue(kCGWindowNumber);
               long windowNumber = new CoreFoundation.CFNumberRef(result).longValue();
               result = windowRef.getValue(kCGWindowOwnerPID);
               long windowOwnerPID = new CoreFoundation.CFNumberRef(result).longValue();
               result = windowRef.getValue(kCGWindowLayer);
               int windowLayer = new CoreFoundation.CFNumberRef(result).intValue();
               result = windowRef.getValue(kCGWindowBounds);
               CoreGraphics.CGRect rect = new CoreGraphics.CGRect();

               try {
                  CoreGraphics.INSTANCE.CGRectMakeWithDictionaryRepresentation(new CoreFoundation.CFDictionaryRef(result), rect);
                  Rectangle windowBounds = new Rectangle(
                     FormatUtil.roundToInt(rect.origin.x),
                     FormatUtil.roundToInt(rect.origin.y),
                     FormatUtil.roundToInt(rect.size.width),
                     FormatUtil.roundToInt(rect.size.height)
                  );
                  result = windowRef.getValue(kCGWindowName);
                  String windowName = CFUtil.cfPointerToString(result, false);
                  result = windowRef.getValue(kCGWindowOwnerName);
                  String windowOwnerName = CFUtil.cfPointerToString(result, false);
                  if (windowName.isEmpty()) {
                     windowName = windowOwnerName;
                  } else {
                     windowName = windowName + "(" + windowOwnerName + ")";
                  }

                  windowList.add(new OSDesktopWindow(windowNumber, windowName, windowOwnerName, windowBounds, windowOwnerPID, windowLayer, visible));
               } catch (Throwable var29) {
                  try {
                     rect.close();
                  } catch (Throwable var28) {
                     var29.addSuppressed(var28);
                  }

                  throw var29;
               }

               rect.close();
            }
         }
      } finally {
         kCGWindowIsOnscreen.release();
         kCGWindowNumber.release();
         kCGWindowOwnerPID.release();
         kCGWindowLayer.release();
         kCGWindowBounds.release();
         kCGWindowName.release();
         kCGWindowOwnerName.release();
         windowInfo.release();
      }

      return windowList;
   }
}
