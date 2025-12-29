package oshi.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;

public interface SystemConfiguration extends Library {
   SystemConfiguration INSTANCE = Native.load("SystemConfiguration", SystemConfiguration.class);

   CoreFoundation.CFArrayRef SCNetworkInterfaceCopyAll();

   CoreFoundation.CFStringRef SCNetworkInterfaceGetBSDName(SystemConfiguration.SCNetworkInterfaceRef var1);

   CoreFoundation.CFStringRef SCNetworkInterfaceGetLocalizedDisplayName(SystemConfiguration.SCNetworkInterfaceRef var1);

   public static class SCNetworkInterfaceRef extends CoreFoundation.CFTypeRef {
      public SCNetworkInterfaceRef() {
      }

      public SCNetworkInterfaceRef(Pointer p) {
         super(p);
      }
   }
}
