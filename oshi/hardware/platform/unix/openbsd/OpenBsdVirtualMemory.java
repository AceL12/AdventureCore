package oshi.hardware.platform.unix.openbsd;

import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class OpenBsdVirtualMemory extends AbstractVirtualMemory {
   private final OpenBsdGlobalMemory global;
   private final Supplier<Triplet<Integer, Integer, Integer>> usedTotalPgin = Memoizer.memoize(OpenBsdVirtualMemory::queryVmstat, Memoizer.defaultExpiration());
   private final Supplier<Integer> pgout = Memoizer.memoize(OpenBsdVirtualMemory::queryUvm, Memoizer.defaultExpiration());

   OpenBsdVirtualMemory(OpenBsdGlobalMemory freeBsdGlobalMemory) {
      this.global = freeBsdGlobalMemory;
   }

   @Override
   public long getSwapUsed() {
      return this.usedTotalPgin.get().getA().intValue() * this.global.getPageSize();
   }

   @Override
   public long getSwapTotal() {
      return this.usedTotalPgin.get().getB().intValue() * this.global.getPageSize();
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
      return this.usedTotalPgin.get().getC().intValue() * this.global.getPageSize();
   }

   @Override
   public long getSwapPagesOut() {
      return this.pgout.get().intValue() * this.global.getPageSize();
   }

   private static Triplet<Integer, Integer, Integer> queryVmstat() {
      int used = 0;
      int total = 0;
      int swapIn = 0;

      for (String line : ExecutingCommand.runNative("vmstat -s")) {
         if (line.contains("swap pages in use")) {
            used = ParseUtil.getFirstIntValue(line);
         } else if (line.contains("swap pages")) {
            total = ParseUtil.getFirstIntValue(line);
         } else if (line.contains("pagein operations")) {
            swapIn = ParseUtil.getFirstIntValue(line);
         }
      }

      return new Triplet<>(used, total, swapIn);
   }

   private static int queryUvm() {
      for (String line : ExecutingCommand.runNative("systat -ab uvm")) {
         if (line.contains("pdpageouts")) {
            return ParseUtil.getFirstIntValue(line);
         }
      }

      return 0;
   }
}
