package oshi.hardware.platform.windows;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.wmi.Win32PhysicalMemory;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.jna.Struct;
import oshi.util.Memoizer;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Triplet;

@ThreadSafe
final class WindowsGlobalMemory extends AbstractGlobalMemory {
   private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemory.class);
   private static final boolean IS_WINDOWS10_OR_GREATER = VersionHelpers.IsWindows10OrGreater();
   private final Supplier<Triplet<Long, Long, Long>> availTotalSize = Memoizer.memoize(WindowsGlobalMemory::readPerfInfo, Memoizer.defaultExpiration());
   private final Supplier<VirtualMemory> vm = Memoizer.memoize(this::createVirtualMemory);

   @Override
   public long getAvailable() {
      return this.availTotalSize.get().getA();
   }

   @Override
   public long getTotal() {
      return this.availTotalSize.get().getB();
   }

   @Override
   public long getPageSize() {
      return this.availTotalSize.get().getC();
   }

   @Override
   public VirtualMemory getVirtualMemory() {
      return this.vm.get();
   }

   private VirtualMemory createVirtualMemory() {
      return new WindowsVirtualMemory(this);
   }

   @Override
   public List<PhysicalMemory> getPhysicalMemory() {
      List<PhysicalMemory> physicalMemoryList = new ArrayList<>();
      if (IS_WINDOWS10_OR_GREATER) {
         WbemcliUtil.WmiResult<Win32PhysicalMemory.PhysicalMemoryProperty> bankMap = Win32PhysicalMemory.queryphysicalMemory();

         for (int index = 0; index < bankMap.getResultCount(); index++) {
            String bankLabel = WmiUtil.getString(bankMap, Win32PhysicalMemory.PhysicalMemoryProperty.BANKLABEL, index);
            long capacity = WmiUtil.getUint64(bankMap, Win32PhysicalMemory.PhysicalMemoryProperty.CAPACITY, index);
            long speed = WmiUtil.getUint32(bankMap, Win32PhysicalMemory.PhysicalMemoryProperty.SPEED, index) * 1000000L;
            String manufacturer = WmiUtil.getString(bankMap, Win32PhysicalMemory.PhysicalMemoryProperty.MANUFACTURER, index);
            String memoryType = smBiosMemoryType(WmiUtil.getUint32(bankMap, Win32PhysicalMemory.PhysicalMemoryProperty.SMBIOSMEMORYTYPE, index));
            physicalMemoryList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType));
         }
      } else {
         WbemcliUtil.WmiResult<Win32PhysicalMemory.PhysicalMemoryPropertyWin8> bankMap = Win32PhysicalMemory.queryphysicalMemoryWin8();

         for (int index = 0; index < bankMap.getResultCount(); index++) {
            String bankLabel = WmiUtil.getString(bankMap, Win32PhysicalMemory.PhysicalMemoryPropertyWin8.BANKLABEL, index);
            long capacity = WmiUtil.getUint64(bankMap, Win32PhysicalMemory.PhysicalMemoryPropertyWin8.CAPACITY, index);
            long speed = WmiUtil.getUint32(bankMap, Win32PhysicalMemory.PhysicalMemoryPropertyWin8.SPEED, index) * 1000000L;
            String manufacturer = WmiUtil.getString(bankMap, Win32PhysicalMemory.PhysicalMemoryPropertyWin8.MANUFACTURER, index);
            String memoryType = memoryType(WmiUtil.getUint16(bankMap, Win32PhysicalMemory.PhysicalMemoryPropertyWin8.MEMORYTYPE, index));
            physicalMemoryList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType));
         }
      }

      return physicalMemoryList;
   }

   private static String memoryType(int type) {
      switch (type) {
         case 1:
            return "Other";
         case 2:
            return "DRAM";
         case 3:
            return "Synchronous DRAM";
         case 4:
            return "Cache DRAM";
         case 5:
            return "EDO";
         case 6:
            return "EDRAM";
         case 7:
            return "VRAM";
         case 8:
            return "SRAM";
         case 9:
            return "RAM";
         case 10:
            return "ROM";
         case 11:
            return "Flash";
         case 12:
            return "EEPROM";
         case 13:
            return "FEPROM";
         case 14:
            return "EPROM";
         case 15:
            return "CDRAM";
         case 16:
            return "3DRAM";
         case 17:
            return "SDRAM";
         case 18:
            return "SGRAM";
         case 19:
            return "RDRAM";
         case 20:
            return "DDR";
         case 21:
            return "DDR2";
         case 22:
            return "DDR2-FB-DIMM";
         case 23:
         default:
            return "Unknown";
         case 24:
            return "DDR3";
         case 25:
            return "FBD2";
      }
   }

   private static String smBiosMemoryType(int type) {
      switch (type) {
         case 1:
            return "Other";
         case 2:
         case 21:
         case 22:
         case 23:
         default:
            return "Unknown";
         case 3:
            return "DRAM";
         case 4:
            return "EDRAM";
         case 5:
            return "VRAM";
         case 6:
            return "SRAM";
         case 7:
            return "RAM";
         case 8:
            return "ROM";
         case 9:
            return "FLASH";
         case 10:
            return "EEPROM";
         case 11:
            return "FEPROM";
         case 12:
            return "EPROM";
         case 13:
            return "CDRAM";
         case 14:
            return "3DRAM";
         case 15:
            return "SDRAM";
         case 16:
            return "SGRAM";
         case 17:
            return "RDRAM";
         case 18:
            return "DDR";
         case 19:
            return "DDR2";
         case 20:
            return "DDR2 FB-DIMM";
         case 24:
            return "DDR3";
         case 25:
            return "FBD2";
         case 26:
            return "DDR4";
         case 27:
            return "LPDDR";
         case 28:
            return "LPDDR2";
         case 29:
            return "LPDDR3";
         case 30:
            return "LPDDR4";
         case 31:
            return "Logical non-volatile device";
      }
   }

   private static Triplet<Long, Long, Long> readPerfInfo() {
      Struct.CloseablePerformanceInformation performanceInfo = new Struct.CloseablePerformanceInformation();

      Triplet var10;
      label29: {
         Triplet var7;
         try {
            if (!Psapi.INSTANCE.GetPerformanceInfo(performanceInfo, performanceInfo.size())) {
               LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
               var10 = new Triplet<>(0L, 0L, 4098L);
               break label29;
            }

            long pageSize = performanceInfo.PageSize.longValue();
            long memAvailable = pageSize * performanceInfo.PhysicalAvailable.longValue();
            long memTotal = pageSize * performanceInfo.PhysicalTotal.longValue();
            var7 = new Triplet<>(memAvailable, memTotal, pageSize);
         } catch (Throwable var9) {
            try {
               performanceInfo.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         performanceInfo.close();
         return var7;
      }

      performanceInfo.close();
      return var10;
   }
}
