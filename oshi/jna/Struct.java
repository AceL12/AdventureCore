package oshi.jna;

import com.sun.jna.platform.linux.LibC;
import com.sun.jna.platform.mac.SystemB;
import com.sun.jna.platform.win32.IPHlpAPI;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import oshi.util.Util;

public interface Struct {
   public static class CloseableHostCpuLoadInfo extends SystemB.HostCpuLoadInfo implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableMibIfRow extends IPHlpAPI.MIB_IFROW implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableMibIfRow2 extends IPHlpAPI.MIB_IF_ROW2 implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableMibTcpStats extends IPHlpAPI.MIB_TCPSTATS implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableMibUdpStats extends IPHlpAPI.MIB_UDPSTATS implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseablePdhRawCounter extends Pdh.PDH_RAW_COUNTER implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseablePerformanceInformation extends Psapi.PERFORMANCE_INFORMATION implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableProcTaskAllInfo extends SystemB.ProcTaskAllInfo implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableProcTaskInfo extends SystemB.ProcTaskInfo implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableRUsageInfoV2 extends SystemB.RUsageInfoV2 implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableSpDeviceInterfaceData extends SetupApi.SP_DEVICE_INTERFACE_DATA implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableSpDevinfoData extends SetupApi.SP_DEVINFO_DATA implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableSysinfo extends LibC.Sysinfo implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableSystemInfo extends WinBase.SYSTEM_INFO implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableTimeval extends SystemB.Timeval implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableVMStatistics extends SystemB.VMStatistics implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableVnodePathInfo extends SystemB.VnodePathInfo implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }

   public static class CloseableXswUsage extends SystemB.XswUsage implements AutoCloseable {
      @Override
      public void close() {
         Util.freeMemory(this.getPointer());
      }
   }
}
