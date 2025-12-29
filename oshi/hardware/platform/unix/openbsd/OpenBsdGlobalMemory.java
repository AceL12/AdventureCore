package oshi.hardware.platform.unix.openbsd;

import com.sun.jna.Memory;
import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

@ThreadSafe
final class OpenBsdGlobalMemory extends AbstractGlobalMemory {
   private final Supplier<Long> available = Memoizer.memoize(OpenBsdGlobalMemory::queryAvailable, Memoizer.defaultExpiration());
   private final Supplier<Long> total = Memoizer.memoize(OpenBsdGlobalMemory::queryPhysMem);
   private final Supplier<Long> pageSize = Memoizer.memoize(OpenBsdGlobalMemory::queryPageSize);
   private final Supplier<VirtualMemory> vm = Memoizer.memoize(this::createVirtualMemory);

   @Override
   public long getAvailable() {
      return this.available.get() * this.getPageSize();
   }

   @Override
   public long getTotal() {
      return this.total.get();
   }

   @Override
   public long getPageSize() {
      return this.pageSize.get();
   }

   @Override
   public VirtualMemory getVirtualMemory() {
      return this.vm.get();
   }

   private static long queryAvailable() {
      long free = 0L;
      long inactive = 0L;

      for (String line : ExecutingCommand.runNative("vmstat -s")) {
         if (line.endsWith("pages free")) {
            free = ParseUtil.getFirstIntValue(line);
         } else if (line.endsWith("pages inactive")) {
            inactive = ParseUtil.getFirstIntValue(line);
         }
      }

      int[] mib = new int[]{10, 0, 3};
      Memory m = OpenBsdSysctlUtil.sysctl(mib);

      long var7;
      try {
         OpenBsdLibc.Bcachestats cache = new OpenBsdLibc.Bcachestats(m);
         var7 = cache.numbufpages + free + inactive;
      } catch (Throwable var10) {
         if (m != null) {
            try {
               m.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (m != null) {
         m.close();
      }

      return var7;
   }

   private static long queryPhysMem() {
      return OpenBsdSysctlUtil.sysctl("hw.physmem", 0L);
   }

   private static long queryPageSize() {
      return OpenBsdSysctlUtil.sysctl("hw.pagesize", 4096L);
   }

   private VirtualMemory createVirtualMemory() {
      return new OpenBsdVirtualMemory(this);
   }
}
