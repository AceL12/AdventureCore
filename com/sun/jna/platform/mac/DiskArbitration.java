package com.sun.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface DiskArbitration extends Library {
   DiskArbitration INSTANCE = Native.load("DiskArbitration", DiskArbitration.class);

   DiskArbitration.DASessionRef DASessionCreate(CoreFoundation.CFAllocatorRef var1);

   DiskArbitration.DADiskRef DADiskCreateFromBSDName(CoreFoundation.CFAllocatorRef var1, DiskArbitration.DASessionRef var2, String var3);

   DiskArbitration.DADiskRef DADiskCreateFromIOMedia(CoreFoundation.CFAllocatorRef var1, DiskArbitration.DASessionRef var2, IOKit.IOObject var3);

   CoreFoundation.CFDictionaryRef DADiskCopyDescription(DiskArbitration.DADiskRef var1);

   String DADiskGetBSDName(DiskArbitration.DADiskRef var1);

   public static class DADiskRef extends CoreFoundation.CFTypeRef {
   }

   public static class DASessionRef extends CoreFoundation.CFTypeRef {
   }
}
