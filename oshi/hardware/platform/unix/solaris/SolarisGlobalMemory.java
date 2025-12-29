package oshi.hardware.platform.unix.solaris;

import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.kstat.SystemPages;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

@ThreadSafe
final class SolarisGlobalMemory extends AbstractGlobalMemory {
   private final Supplier<Pair<Long, Long>> availTotal = Memoizer.memoize(SystemPages::queryAvailableTotal, Memoizer.defaultExpiration());
   private final Supplier<Long> pageSize = Memoizer.memoize(SolarisGlobalMemory::queryPageSize);
   private final Supplier<VirtualMemory> vm = Memoizer.memoize(this::createVirtualMemory);

   @Override
   public long getAvailable() {
      return this.availTotal.get().getA() * this.getPageSize();
   }

   @Override
   public long getTotal() {
      return this.availTotal.get().getB() * this.getPageSize();
   }

   @Override
   public long getPageSize() {
      return this.pageSize.get();
   }

   @Override
   public VirtualMemory getVirtualMemory() {
      return this.vm.get();
   }

   private static long queryPageSize() {
      return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("pagesize"), 4096L);
   }

   private VirtualMemory createVirtualMemory() {
      return new SolarisVirtualMemory(this);
   }
}
