package com.sun.jna.platform.unix.aix;

import com.sun.jna.Native;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class SharedObjectLoader {
   private SharedObjectLoader() {
   }

   static Perfstat getPerfstatInstance() {
      Map<String, Object> options = getOptions();

      try {
         return Native.load("/usr/lib/libperfstat.a(shr_64.o)", Perfstat.class, options);
      } catch (UnsatisfiedLinkError var2) {
         return Native.load("/usr/lib/libperfstat.a(shr.o)", Perfstat.class, options);
      }
   }

   private static Map<String, Object> getOptions() {
      int RTLD_MEMBER = 262144;
      int RTLD_GLOBAL = 65536;
      int RTLD_LAZY = 4;
      Map<String, Object> options = new HashMap<>();
      options.put("open-flags", RTLD_MEMBER | RTLD_GLOBAL | RTLD_LAZY);
      return Collections.unmodifiableMap(options);
   }
}
