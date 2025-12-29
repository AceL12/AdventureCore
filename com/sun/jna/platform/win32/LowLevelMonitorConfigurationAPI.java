package com.sun.jna.platform.win32;

import com.sun.jna.Structure;
import com.sun.jna.platform.EnumUtils;

public interface LowLevelMonitorConfigurationAPI {
   @Structure.FieldOrder({"dwHorizontalFrequencyInHZ", "dwVerticalFrequencyInHZ", "bTimingStatusByte"})
   public static class MC_TIMING_REPORT extends Structure {
      public WinDef.DWORD dwHorizontalFrequencyInHZ;
      public WinDef.DWORD dwVerticalFrequencyInHZ;
      public WinDef.BYTE bTimingStatusByte;
   }

   public static enum MC_VCP_CODE_TYPE {
      MC_MOMENTARY,
      MC_SET_PARAMETER;

      public static class ByReference extends com.sun.jna.ptr.ByReference {
         public ByReference() {
            super(4);
         }

         public ByReference(LowLevelMonitorConfigurationAPI.MC_VCP_CODE_TYPE value) {
            super(4);
            this.setValue(value);
         }

         public void setValue(LowLevelMonitorConfigurationAPI.MC_VCP_CODE_TYPE value) {
            this.getPointer().setInt(0L, EnumUtils.toInteger(value));
         }

         public LowLevelMonitorConfigurationAPI.MC_VCP_CODE_TYPE getValue() {
            return EnumUtils.fromInteger(this.getPointer().getInt(0L), LowLevelMonitorConfigurationAPI.MC_VCP_CODE_TYPE.class);
         }
      }
   }
}
