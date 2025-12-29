package com.sun.jna.platform.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.PointerType;

public interface Udev extends Library {
   Udev INSTANCE = Native.load("udev", Udev.class);

   Udev.UdevContext udev_new();

   Udev.UdevContext udev_ref(Udev.UdevContext var1);

   Udev.UdevContext udev_unref(Udev.UdevContext var1);

   Udev.UdevDevice udev_device_new_from_syspath(Udev.UdevContext var1, String var2);

   Udev.UdevEnumerate udev_enumerate_new(Udev.UdevContext var1);

   Udev.UdevEnumerate udev_enumerate_ref(Udev.UdevEnumerate var1);

   Udev.UdevEnumerate udev_enumerate_unref(Udev.UdevEnumerate var1);

   int udev_enumerate_add_match_subsystem(Udev.UdevEnumerate var1, String var2);

   int udev_enumerate_scan_devices(Udev.UdevEnumerate var1);

   Udev.UdevListEntry udev_enumerate_get_list_entry(Udev.UdevEnumerate var1);

   Udev.UdevListEntry udev_list_entry_get_next(Udev.UdevListEntry var1);

   String udev_list_entry_get_name(Udev.UdevListEntry var1);

   Udev.UdevDevice udev_device_ref(Udev.UdevDevice var1);

   Udev.UdevDevice udev_device_unref(Udev.UdevDevice var1);

   Udev.UdevDevice udev_device_get_parent(Udev.UdevDevice var1);

   Udev.UdevDevice udev_device_get_parent_with_subsystem_devtype(Udev.UdevDevice var1, String var2, String var3);

   String udev_device_get_syspath(Udev.UdevDevice var1);

   String udev_device_get_sysname(Udev.UdevDevice var1);

   String udev_device_get_devnode(Udev.UdevDevice var1);

   String udev_device_get_devtype(Udev.UdevDevice var1);

   String udev_device_get_subsystem(Udev.UdevDevice var1);

   String udev_device_get_sysattr_value(Udev.UdevDevice var1, String var2);

   String udev_device_get_property_value(Udev.UdevDevice var1, String var2);

   public static class UdevContext extends PointerType {
      public Udev.UdevContext ref() {
         return Udev.INSTANCE.udev_ref(this);
      }

      public void unref() {
         Udev.INSTANCE.udev_unref(this);
      }

      public Udev.UdevEnumerate enumerateNew() {
         return Udev.INSTANCE.udev_enumerate_new(this);
      }

      public Udev.UdevDevice deviceNewFromSyspath(String syspath) {
         return Udev.INSTANCE.udev_device_new_from_syspath(this, syspath);
      }
   }

   public static class UdevDevice extends PointerType {
      public Udev.UdevDevice ref() {
         return Udev.INSTANCE.udev_device_ref(this);
      }

      public void unref() {
         Udev.INSTANCE.udev_device_unref(this);
      }

      public Udev.UdevDevice getParent() {
         return Udev.INSTANCE.udev_device_get_parent(this);
      }

      public Udev.UdevDevice getParentWithSubsystemDevtype(String subsystem, String devtype) {
         return Udev.INSTANCE.udev_device_get_parent_with_subsystem_devtype(this, subsystem, devtype);
      }

      public String getSyspath() {
         return Udev.INSTANCE.udev_device_get_syspath(this);
      }

      public String getSysname() {
         return Udev.INSTANCE.udev_device_get_syspath(this);
      }

      public String getDevnode() {
         return Udev.INSTANCE.udev_device_get_devnode(this);
      }

      public String getDevtype() {
         return Udev.INSTANCE.udev_device_get_devtype(this);
      }

      public String getSubsystem() {
         return Udev.INSTANCE.udev_device_get_subsystem(this);
      }

      public String getSysattrValue(String sysattr) {
         return Udev.INSTANCE.udev_device_get_sysattr_value(this, sysattr);
      }

      public String getPropertyValue(String key) {
         return Udev.INSTANCE.udev_device_get_property_value(this, key);
      }
   }

   public static class UdevEnumerate extends PointerType {
      public Udev.UdevEnumerate ref() {
         return Udev.INSTANCE.udev_enumerate_ref(this);
      }

      public void unref() {
         Udev.INSTANCE.udev_enumerate_unref(this);
      }

      public int addMatchSubsystem(String subsystem) {
         return Udev.INSTANCE.udev_enumerate_add_match_subsystem(this, subsystem);
      }

      public int scanDevices() {
         return Udev.INSTANCE.udev_enumerate_scan_devices(this);
      }

      public Udev.UdevListEntry getListEntry() {
         return Udev.INSTANCE.udev_enumerate_get_list_entry(this);
      }
   }

   public static class UdevListEntry extends PointerType {
      public Udev.UdevListEntry getNext() {
         return Udev.INSTANCE.udev_list_entry_get_next(this);
      }

      public String getName() {
         return Udev.INSTANCE.udev_list_entry_get_name(this);
      }
   }
}
