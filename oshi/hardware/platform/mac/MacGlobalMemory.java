package oshi.hardware.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.util.ExecutingCommand;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.platform.mac.SysctlUtil;

@ThreadSafe
final class MacGlobalMemory extends AbstractGlobalMemory {
   private static final Logger LOG = LoggerFactory.getLogger(MacGlobalMemory.class);
   private final Supplier<Long> available = Memoizer.memoize(this::queryVmStats, Memoizer.defaultExpiration());
   private final Supplier<Long> total = Memoizer.memoize(MacGlobalMemory::queryPhysMem);
   private final Supplier<Long> pageSize = Memoizer.memoize(MacGlobalMemory::queryPageSize);
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

   @Override
   public List<PhysicalMemory> getPhysicalMemory() {
      List<PhysicalMemory> pmList = new ArrayList<>();
      List<String> sp = ExecutingCommand.runNative("system_profiler SPMemoryDataType");
      int bank = 0;
      String bankLabel = "unknown";
      long capacity = 0L;
      long speed = 0L;
      String manufacturer = "unknown";
      String memoryType = "unknown";

      for (String line : sp) {
         if (line.trim().startsWith("BANK")) {
            if (bank++ > 0) {
               pmList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType));
            }

            bankLabel = line.trim();
            int colon = bankLabel.lastIndexOf(58);
            if (colon > 0) {
               bankLabel = bankLabel.substring(0, colon - 1);
            }
         } else if (bank > 0) {
            String[] split = line.trim().split(":");
            if (split.length == 2) {
               String var14 = split[0];
               switch (var14) {
                  case "Size":
                     capacity = ParseUtil.parseDecimalMemorySizeToBinary(split[1].trim());
                     break;
                  case "Type":
                     memoryType = split[1].trim();
                     break;
                  case "Speed":
                     speed = ParseUtil.parseHertz(split[1]);
                     break;
                  case "Manufacturer":
                     manufacturer = split[1].trim();
               }
            }
         }
      }

      pmList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType));
      return pmList;
   }

   private long queryVmStats() {
      Struct.CloseableVMStatistics vmStats = new Struct.CloseableVMStatistics();

      long var3;
      label50: {
         try {
            ByRef.CloseableIntByReference size = new ByRef.CloseableIntByReference(vmStats.size() / SystemB.INT_SIZE);

            label46: {
               try {
                  if (0 == SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), 2, vmStats, size)) {
                     var3 = (vmStats.free_count + vmStats.inactive_count) * this.getPageSize();
                     break label46;
                  }

                  LOG.error("Failed to get host VM info. Error code: {}", Native.getLastError());
                  var3 = 0L;
               } catch (Throwable var7) {
                  try {
                     size.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }

                  throw var7;
               }

               size.close();
               break label50;
            }

            size.close();
         } catch (Throwable var8) {
            try {
               vmStats.close();
            } catch (Throwable var5) {
               var8.addSuppressed(var5);
            }

            throw var8;
         }

         vmStats.close();
         return var3;
      }

      vmStats.close();
      return var3;
   }

   private static long queryPhysMem() {
      return SysctlUtil.sysctl("hw.memsize", 0L);
   }

   private static long queryPageSize() {
      ByRef.CloseableLongByReference pPageSize = new ByRef.CloseableLongByReference();

      long var1;
      label27: {
         try {
            if (0 == SystemB.INSTANCE.host_page_size(SystemB.INSTANCE.mach_host_self(), pPageSize)) {
               var1 = pPageSize.getValue();
               break label27;
            }
         } catch (Throwable var4) {
            try {
               pPageSize.close();
            } catch (Throwable var3) {
               var4.addSuppressed(var3);
            }

            throw var4;
         }

         pPageSize.close();
         LOG.error("Failed to get host page size. Error code: {}", Native.getLastError());
         return 4098L;
      }

      pPageSize.close();
      return var1;
   }

   private VirtualMemory createVirtualMemory() {
      return new MacVirtualMemory(this);
   }
}
