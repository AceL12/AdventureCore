package oshi.hardware.platform.unix.aix;

import com.sun.jna.platform.unix.aix.Perfstat;
import java.util.List;
import java.util.function.Supplier;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.Lscfg;
import oshi.driver.unix.aix.perfstat.PerfstatDisk;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.platform.unix.UnixDisplay;
import oshi.util.Memoizer;

@ThreadSafe
public final class AixHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {
   private final Supplier<List<String>> lscfg = Memoizer.memoize(Lscfg::queryAllDevices, Memoizer.defaultExpiration());
   private final Supplier<Perfstat.perfstat_disk_t[]> diskStats = Memoizer.memoize(PerfstatDisk::queryDiskStats, Memoizer.defaultExpiration());

   @Override
   public ComputerSystem createComputerSystem() {
      return new AixComputerSystem(this.lscfg);
   }

   @Override
   public GlobalMemory createMemory() {
      return new AixGlobalMemory(this.lscfg);
   }

   @Override
   public CentralProcessor createProcessor() {
      return new AixCentralProcessor();
   }

   @Override
   public Sensors createSensors() {
      return new AixSensors(this.lscfg);
   }

   @Override
   public List<PowerSource> getPowerSources() {
      return AixPowerSource.getPowerSources();
   }

   @Override
   public List<HWDiskStore> getDiskStores() {
      return AixHWDiskStore.getDisks(this.diskStats);
   }

   @Override
   public List<Display> getDisplays() {
      return UnixDisplay.getDisplays();
   }

   @Override
   public List<NetworkIF> getNetworkIFs(boolean includeLocalInterfaces) {
      return AixNetworkIF.getNetworks(includeLocalInterfaces);
   }

   @Override
   public List<UsbDevice> getUsbDevices(boolean tree) {
      return AixUsbDevice.getUsbDevices(tree, this.lscfg);
   }

   @Override
   public List<SoundCard> getSoundCards() {
      return AixSoundCard.getSoundCards(this.lscfg);
   }

   @Override
   public List<GraphicsCard> getGraphicsCards() {
      return AixGraphicsCard.getGraphicsCards(this.lscfg);
   }
}
