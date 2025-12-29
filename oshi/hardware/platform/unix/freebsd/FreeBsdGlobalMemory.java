package oshi.hardware.platform.unix.freebsd;

import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

@ThreadSafe
final class FreeBsdGlobalMemory extends AbstractGlobalMemory {
   private final Supplier<Long> available = Memoizer.memoize(this::queryVmStats, Memoizer.defaultExpiration());
   private final Supplier<Long> total = Memoizer.memoize(FreeBsdGlobalMemory::queryPhysMem);
   private final Supplier<Long> pageSize = Memoizer.memoize(FreeBsdGlobalMemory::queryPageSize);
   private final Supplier<VirtualMemory> vm = Memoizer.memoize(this::createVirtualMemory);

   @Override
   public long getAvailable() {
      return this.available.get();
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

   private long queryVmStats() {
      int inactive = BsdSysctlUtil.sysctl("vm.stats.vm.v_inactive_count", 0);
      int free = BsdSysctlUtil.sysctl("vm.stats.vm.v_free_count", 0);
      return (inactive + free) * this.getPageSize();
   }

   private static long queryPhysMem() {
      return BsdSysctlUtil.sysctl("hw.physmem", 0L);
   }

   private static long queryPageSize() {
      return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("sysconf PAGESIZE"), 4096L);
   }

   private VirtualMemory createVirtualMemory() {
      return new FreeBsdVirtualMemory(this);
   }
}
