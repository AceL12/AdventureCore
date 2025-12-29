package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.mac.CoreFoundation;
import oshi.util.Util;

public interface CoreGraphics extends Library {
   CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
   int kCGNullWindowID = 0;
   int kCGWindowListOptionAll = 0;
   int kCGWindowListOptionOnScreenOnly = 1;
   int kCGWindowListOptionOnScreenAboveWindow = 2;
   int kCGWindowListOptionOnScreenBelowWindow = 4;
   int kCGWindowListOptionIncludingWindow = 8;
   int kCGWindowListExcludeDesktopElements = 16;

   CoreFoundation.CFArrayRef CGWindowListCopyWindowInfo(int var1, int var2);

   boolean CGRectMakeWithDictionaryRepresentation(CoreFoundation.CFDictionaryRef var1, CoreGraphics.CGRect var2);

   @Structure.FieldOrder({"x", "y"})
   public static class CGPoint extends Structure {
      public double x;
      public double y;
   }

   @Structure.FieldOrder({"origin", "size"})
   public static class CGRect extends Structure implements AutoCloseable {
      public CoreGraphics.CGPoint origin;
      public CoreGraphics.CGSize size;

      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   @Structure.FieldOrder({"width", "height"})
   public static class CGSize extends Structure {
      public double width;
      public double height;
   }
}
