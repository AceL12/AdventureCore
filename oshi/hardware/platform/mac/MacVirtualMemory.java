package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
final class MacVirtualMemory extends AbstractVirtualMemory {
   private static final Logger LOG = LoggerFactory.getLogger(MacVirtualMemory.class);
   private final MacGlobalMemory global;
   private final Supplier<Pair<Long, Long>> usedTotal = Memoizer.memoize(MacVirtualMemory::querySwapUsage, Memoizer.defaultExpiration());
   private final Supplier<Pair<Long, Long>> inOut = Memoizer.memoize(MacVirtualMemory::queryVmStat, Memoizer.defaultExpiration());

   MacVirtualMemory(MacGlobalMemory macGlobalMemory) {
      this.global = macGlobalMemory;
   }

   @Override
   public long getSwapUsed() {
      return this.usedTotal.get().getA();
   }

   @Override
   public long getSwapTotal() {
      return this.usedTotal.get().getB();
   }

   @Override
   public long getVirtualMax() {
      return this.global.getTotal() + this.getSwapTotal();
   }

   @Override
   public long getVirtualInUse() {
      return this.global.getTotal() - this.global.getAvailable() + this.getSwapUsed();
   }

   @Override
   public long getSwapPagesIn() {
      return this.inOut.get().getA();
   }

   @Override
   public long getSwapPagesOut() {
      return this.inOut.get().getB();
   }

   private static Pair<Long, Long> querySwapUsage() {
      long swapUsed = 0L;
      long swapTotal = 0L;
      Struct.CloseableXswUsage xswUsage = new Struct.CloseableXswUsage();

      try {
         if (SysctlUtil.sysctl("vm.swapusage", xswUsage)) {
            swapUsed = xswUsage.xsu_used;
            swapTotal = xswUsage.xsu_total;
         }
      } catch (Throwable var8) {
         try {
            xswUsage.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      xswUsage.close();
      return new Pair<>(swapUsed, swapTotal);
   }

   private static Pair<Long, Long> queryVmStat() {
      long swapPagesIn = 0L;
      long swapPagesOut = 0L;
      Struct.CloseableVMStatistics vmStats = new Struct.CloseableVMStatistics();

      try {
         ByRef.CloseableIntByReference size = new ByRef.CloseableIntByReference(vmStats.size() / SystemB.INT_SIZE);

         try {
            if (0 == SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), 2, vmStats, size)) {
               swapPagesIn = ParseUtil.unsignedIntToLong(vmStats.pageins);
               swapPagesOut = ParseUtil.unsignedIntToLong(vmStats.pageouts);
            } else {
               LOG.error("Failed to get host VM info. Error code: {}", Native.getLastError());
            }
         } catch (Throwable var10) {
            try {
               size.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }

            throw var10;
         }

         size.close();
      } catch (Throwable var11) {
         try {
            vmStats.close();
         } catch (Throwable var8) {
            var11.addSuppressed(var8);
         }

         throw var11;
      }

      vmStats.close();
      return new Pair<>(swapPagesIn, swapPagesOut);
   }
}
