package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

@ThreadSafe
public final class MSFTStorage {
   private static final String STORAGE_NAMESPACE = "ROOT\\Microsoft\\Windows\\Storage";
   private static final String MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE = "MSFT_StoragePool WHERE IsPrimordial=FALSE";
   private static final String MSFT_STORAGE_POOL_TO_PHYSICAL_DISK = "MSFT_StoragePoolToPhysicalDisk";
   private static final String MSFT_PHYSICAL_DISK = "MSFT_PhysicalDisk";
   private static final String MSFT_VIRTUAL_DISK = "MSFT_VirtualDisk";

   private MSFTStorage() {
   }

   public static WbemcliUtil.WmiResult<MSFTStorage.StoragePoolProperty> queryStoragePools(WmiQueryHandler h) {
      WbemcliUtil.WmiQuery<MSFTStorage.StoragePoolProperty> storagePoolQuery = new WbemcliUtil.WmiQuery<>(
         "ROOT\\Microsoft\\Windows\\Storage", "MSFT_StoragePool WHERE IsPrimordial=FALSE", MSFTStorage.StoragePoolProperty.class
      );
      return h.queryWMI(storagePoolQuery, false);
   }

   public static WbemcliUtil.WmiResult<MSFTStorage.StoragePoolToPhysicalDiskProperty> queryStoragePoolPhysicalDisks(WmiQueryHandler h) {
      WbemcliUtil.WmiQuery<MSFTStorage.StoragePoolToPhysicalDiskProperty> storagePoolToPhysicalDiskQuery = new WbemcliUtil.WmiQuery<>(
         "ROOT\\Microsoft\\Windows\\Storage", "MSFT_StoragePoolToPhysicalDisk", MSFTStorage.StoragePoolToPhysicalDiskProperty.class
      );
      return h.queryWMI(storagePoolToPhysicalDiskQuery, false);
   }

   public static WbemcliUtil.WmiResult<MSFTStorage.PhysicalDiskProperty> queryPhysicalDisks(WmiQueryHandler h) {
      WbemcliUtil.WmiQuery<MSFTStorage.PhysicalDiskProperty> physicalDiskQuery = new WbemcliUtil.WmiQuery<>(
         "ROOT\\Microsoft\\Windows\\Storage", "MSFT_PhysicalDisk", MSFTStorage.PhysicalDiskProperty.class
      );
      return h.queryWMI(physicalDiskQuery, false);
   }

   public static WbemcliUtil.WmiResult<MSFTStorage.VirtualDiskProperty> queryVirtualDisks(WmiQueryHandler h) {
      WbemcliUtil.WmiQuery<MSFTStorage.VirtualDiskProperty> virtualDiskQuery = new WbemcliUtil.WmiQuery<>(
         "ROOT\\Microsoft\\Windows\\Storage", "MSFT_VirtualDisk", MSFTStorage.VirtualDiskProperty.class
      );
      return h.queryWMI(virtualDiskQuery, false);
   }

   public static enum PhysicalDiskProperty {
      FRIENDLYNAME,
      PHYSICALLOCATION,
      OBJECTID;
   }

   public static enum StoragePoolProperty {
      FRIENDLYNAME,
      OBJECTID;
   }

   public static enum StoragePoolToPhysicalDiskProperty {
      STORAGEPOOL,
      PHYSICALDISK;
   }

   public static enum VirtualDiskProperty {
      FRIENDLYNAME,
      OBJECTID;
   }
}
