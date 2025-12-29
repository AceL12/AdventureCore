package oshi.hardware.platform.windows;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APITypeMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.jna.platform.windows.PowrProf;

@ThreadSafe
public final class WindowsPowerSource extends AbstractPowerSource {
   private static final Guid.GUID GUID_DEVCLASS_BATTERY = Guid.GUID.fromString("{72631E54-78A4-11D0-BCF7-00AA00B7B32A}");
   private static final int CHAR_WIDTH = W32APITypeMapper.DEFAULT == W32APITypeMapper.UNICODE ? 2 : 1;
   private static final boolean X64 = Platform.is64Bit();
   private static final int BATTERY_SYSTEM_BATTERY = Integer.MIN_VALUE;
   private static final int BATTERY_IS_SHORT_TERM = 536870912;
   private static final int BATTERY_POWER_ON_LINE = 1;
   private static final int BATTERY_DISCHARGING = 2;
   private static final int BATTERY_CHARGING = 4;
   private static final int BATTERY_CAPACITY_RELATIVE = 1073741824;
   private static final int IOCTL_BATTERY_QUERY_TAG = 2703424;
   private static final int IOCTL_BATTERY_QUERY_STATUS = 2703436;
   private static final int IOCTL_BATTERY_QUERY_INFORMATION = 2703428;

   public WindowsPowerSource(
      String psName,
      String psDeviceName,
      double psRemainingCapacityPercent,
      double psTimeRemainingEstimated,
      double psTimeRemainingInstant,
      double psPowerUsageRate,
      double psVoltage,
      double psAmperage,
      boolean psPowerOnLine,
      boolean psCharging,
      boolean psDischarging,
      PowerSource.CapacityUnits psCapacityUnits,
      int psCurrentCapacity,
      int psMaxCapacity,
      int psDesignCapacity,
      int psCycleCount,
      String psChemistry,
      LocalDate psManufactureDate,
      String psManufacturer,
      String psSerialNumber,
      double psTemperature
   ) {
      super(
         psName,
         psDeviceName,
         psRemainingCapacityPercent,
         psTimeRemainingEstimated,
         psTimeRemainingInstant,
         psPowerUsageRate,
         psVoltage,
         psAmperage,
         psPowerOnLine,
         psCharging,
         psDischarging,
         psCapacityUnits,
         psCurrentCapacity,
         psMaxCapacity,
         psDesignCapacity,
         psCycleCount,
         psChemistry,
         psManufactureDate,
         psManufacturer,
         psSerialNumber,
         psTemperature
      );
   }

   public static List<PowerSource> getPowerSources() {
      return Arrays.asList(getPowerSource("System Battery"));
   }

   private static WindowsPowerSource getPowerSource(String name) {
      String psDeviceName = "unknown";
      double psRemainingCapacityPercent = 1.0;
      double psTimeRemainingEstimated = -1.0;
      double psTimeRemainingInstant = 0.0;
      int psPowerUsageRate = 0;
      double psVoltage = -1.0;
      double psAmperage = 0.0;
      boolean psPowerOnLine = false;
      boolean psCharging = false;
      boolean psDischarging = false;
      PowerSource.CapacityUnits psCapacityUnits = PowerSource.CapacityUnits.RELATIVE;
      int psCurrentCapacity = 0;
      int psMaxCapacity = 1;
      int psDesignCapacity = 1;
      int psCycleCount = -1;
      String psChemistry = "unknown";
      LocalDate psManufactureDate = null;
      String psManufacturer = "unknown";
      String psSerialNumber = "unknown";
      double psTemperature = 0.0;
      PowrProf.SystemBatteryState batteryState = new PowrProf.SystemBatteryState();

      try {
         if (0 == PowrProf.INSTANCE.CallNtPowerInformation(5, null, 0, batteryState.getPointer(), batteryState.size()) && batteryState.batteryPresent > 0) {
            if (batteryState.acOnLine == 0 && batteryState.charging == 0 && batteryState.discharging > 0) {
               psTimeRemainingEstimated = batteryState.estimatedTime;
            } else if (batteryState.charging > 0) {
               psTimeRemainingEstimated = -2.0;
            }

            psMaxCapacity = batteryState.maxCapacity;
            psCurrentCapacity = batteryState.remainingCapacity;
            psRemainingCapacityPercent = Math.min(1.0, (double)psCurrentCapacity / psMaxCapacity);
            psPowerUsageRate = batteryState.rate;
         }
      } catch (Throwable var74) {
         try {
            batteryState.close();
         } catch (Throwable var60) {
            var74.addSuppressed(var60);
         }

         throw var74;
      }

      batteryState.close();
      WinNT.HANDLE var75 = SetupApi.INSTANCE.SetupDiGetClassDevs(GUID_DEVCLASS_BATTERY, null, null, 18);
      if (!WinBase.INVALID_HANDLE_VALUE.equals(var75)) {
         boolean batteryFound = false;
         int idev = 0;

         label356: {
            Struct.CloseableSpDeviceInterfaceData did;
            while (true) {
               if (batteryFound || idev >= 100) {
                  break label356;
               }

               did = new Struct.CloseableSpDeviceInterfaceData();

               try {
                  ByRef.CloseableIntByReference requiredSize;
                  label378: {
                     requiredSize = new ByRef.CloseableIntByReference();

                     try {
                        ByRef.CloseableIntByReference dwWait = new ByRef.CloseableIntByReference();

                        label345: {
                           try {
                              ByRef.CloseableIntByReference dwTag;
                              label380: {
                                 dwTag = new ByRef.CloseableIntByReference();

                                 try {
                                    ByRef.CloseableIntByReference dwOut = new ByRef.CloseableIntByReference();

                                    label337: {
                                       try {
                                          did.cbSize = did.size();
                                          if (SetupApi.INSTANCE.SetupDiEnumDeviceInterfaces(var75, null, GUID_DEVCLASS_BATTERY, idev, did)) {
                                             SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(var75, did, null, 0, requiredSize, null);
                                             if (122 == Kernel32.INSTANCE.GetLastError()) {
                                                Memory pdidd = new Memory(requiredSize.getValue());

                                                try {
                                                   pdidd.setInt(0L, 4 + (X64 ? 4 : CHAR_WIDTH));
                                                   if (SetupApi.INSTANCE
                                                      .SetupDiGetDeviceInterfaceDetail(var75, did, pdidd, (int)pdidd.size(), requiredSize, null)) {
                                                      String devicePath = CHAR_WIDTH > 1 ? pdidd.getWideString(4L) : pdidd.getString(4L);
                                                      WinNT.HANDLE hBattery = Kernel32.INSTANCE.CreateFile(devicePath, -1073741824, 3, null, 3, 128, null);
                                                      if (!WinBase.INVALID_HANDLE_VALUE.equals(hBattery)) {
                                                         PowrProf.BATTERY_QUERY_INFORMATION bqi = new PowrProf.BATTERY_QUERY_INFORMATION();

                                                         try {
                                                            PowrProf.BATTERY_INFORMATION bi = new PowrProf.BATTERY_INFORMATION();

                                                            try {
                                                               PowrProf.BATTERY_WAIT_STATUS bws = new PowrProf.BATTERY_WAIT_STATUS();

                                                               try {
                                                                  PowrProf.BATTERY_STATUS bs = new PowrProf.BATTERY_STATUS();

                                                                  try {
                                                                     PowrProf.BATTERY_MANUFACTURE_DATE bmd = new PowrProf.BATTERY_MANUFACTURE_DATE();

                                                                     try {
                                                                        if (Kernel32.INSTANCE
                                                                           .DeviceIoControl(
                                                                              hBattery, 2703424, dwWait.getPointer(), 4, dwTag.getPointer(), 4, dwOut, null
                                                                           )) {
                                                                           bqi.BatteryTag = dwTag.getValue();
                                                                           if (bqi.BatteryTag > 0) {
                                                                              bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryInformation
                                                                                 .ordinal();
                                                                              bqi.write();
                                                                              if (Kernel32.INSTANCE
                                                                                 .DeviceIoControl(
                                                                                    hBattery,
                                                                                    2703428,
                                                                                    bqi.getPointer(),
                                                                                    bqi.size(),
                                                                                    bi.getPointer(),
                                                                                    bi.size(),
                                                                                    dwOut,
                                                                                    null
                                                                                 )) {
                                                                                 bi.read();
                                                                                 if (0 != (bi.Capabilities & -2147483648) && 0 == (bi.Capabilities & 536870912)
                                                                                    )
                                                                                  {
                                                                                    if (0 == (bi.Capabilities & 1073741824)) {
                                                                                       psCapacityUnits = PowerSource.CapacityUnits.MWH;
                                                                                    }

                                                                                    psChemistry = Native.toString(bi.Chemistry, StandardCharsets.US_ASCII);
                                                                                    psDesignCapacity = bi.DesignedCapacity;
                                                                                    psMaxCapacity = bi.FullChargedCapacity;
                                                                                    psCycleCount = bi.CycleCount;
                                                                                    bws.BatteryTag = bqi.BatteryTag;
                                                                                    bws.write();
                                                                                    if (Kernel32.INSTANCE
                                                                                       .DeviceIoControl(
                                                                                          hBattery,
                                                                                          2703436,
                                                                                          bws.getPointer(),
                                                                                          bws.size(),
                                                                                          bs.getPointer(),
                                                                                          bs.size(),
                                                                                          dwOut,
                                                                                          null
                                                                                       )) {
                                                                                       bs.read();
                                                                                       if (0 != (bs.PowerState & 1)) {
                                                                                          psPowerOnLine = true;
                                                                                       }

                                                                                       if (0 != (bs.PowerState & 2)) {
                                                                                          psDischarging = true;
                                                                                       }

                                                                                       if (0 != (bs.PowerState & 4)) {
                                                                                          psCharging = true;
                                                                                       }

                                                                                       psCurrentCapacity = bs.Capacity;
                                                                                       psVoltage = bs.Voltage > 0 ? bs.Voltage / 1000.0 : bs.Voltage;
                                                                                       psPowerUsageRate = bs.Rate;
                                                                                       if (psVoltage > 0.0) {
                                                                                          psAmperage = psPowerUsageRate / psVoltage;
                                                                                       }
                                                                                    }
                                                                                 }

                                                                                 psDeviceName = batteryQueryString(
                                                                                    hBattery,
                                                                                    dwTag.getValue(),
                                                                                    PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryDeviceName.ordinal()
                                                                                 );
                                                                                 psManufacturer = batteryQueryString(
                                                                                    hBattery,
                                                                                    dwTag.getValue(),
                                                                                    PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryManufactureName.ordinal()
                                                                                 );
                                                                                 psSerialNumber = batteryQueryString(
                                                                                    hBattery,
                                                                                    dwTag.getValue(),
                                                                                    PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatterySerialNumber.ordinal()
                                                                                 );
                                                                                 bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryManufactureDate
                                                                                    .ordinal();
                                                                                 bqi.write();
                                                                                 if (Kernel32.INSTANCE
                                                                                    .DeviceIoControl(
                                                                                       hBattery,
                                                                                       2703428,
                                                                                       bqi.getPointer(),
                                                                                       bqi.size(),
                                                                                       bmd.getPointer(),
                                                                                       bmd.size(),
                                                                                       dwOut,
                                                                                       null
                                                                                    )) {
                                                                                    bmd.read();
                                                                                    if (bmd.Year > 1900 && bmd.Month > 0 && bmd.Day > 0) {
                                                                                       psManufactureDate = LocalDate.of(bmd.Year, bmd.Month, bmd.Day);
                                                                                    }
                                                                                 }

                                                                                 bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryTemperature
                                                                                    .ordinal();
                                                                                 bqi.write();
                                                                                 ByRef.CloseableIntByReference tempK = new ByRef.CloseableIntByReference();

                                                                                 try {
                                                                                    if (Kernel32.INSTANCE
                                                                                       .DeviceIoControl(
                                                                                          hBattery,
                                                                                          2703428,
                                                                                          bqi.getPointer(),
                                                                                          bqi.size(),
                                                                                          tempK.getPointer(),
                                                                                          4,
                                                                                          dwOut,
                                                                                          null
                                                                                       )) {
                                                                                       psTemperature = tempK.getValue() / 10.0 - 273.15;
                                                                                    }
                                                                                 } catch (Throwable var61) {
                                                                                    try {
                                                                                       tempK.close();
                                                                                    } catch (Throwable var59) {
                                                                                       var61.addSuppressed(var59);
                                                                                    }

                                                                                    throw var61;
                                                                                 }

                                                                                 tempK.close();
                                                                                 bqi.InformationLevel = PowrProf.BATTERY_QUERY_INFORMATION_LEVEL.BatteryEstimatedTime
                                                                                    .ordinal();
                                                                                 if (psPowerUsageRate != 0) {
                                                                                    bqi.AtRate = psPowerUsageRate;
                                                                                 }

                                                                                 bqi.write();
                                                                                 tempK = new ByRef.CloseableIntByReference();

                                                                                 try {
                                                                                    if (Kernel32.INSTANCE
                                                                                       .DeviceIoControl(
                                                                                          hBattery,
                                                                                          2703428,
                                                                                          bqi.getPointer(),
                                                                                          bqi.size(),
                                                                                          tempK.getPointer(),
                                                                                          4,
                                                                                          dwOut,
                                                                                          null
                                                                                       )) {
                                                                                       psTimeRemainingInstant = tempK.getValue();
                                                                                    }
                                                                                 } catch (Throwable var62) {
                                                                                    try {
                                                                                       tempK.close();
                                                                                    } catch (Throwable var58) {
                                                                                       var62.addSuppressed(var58);
                                                                                    }

                                                                                    throw var62;
                                                                                 }

                                                                                 tempK.close();
                                                                                 if (psTimeRemainingInstant < 0.0 && psPowerUsageRate != 0) {
                                                                                    psTimeRemainingInstant = (psMaxCapacity - psCurrentCapacity)
                                                                                       * 3600.0
                                                                                       / psPowerUsageRate;
                                                                                    if (psTimeRemainingInstant < 0.0) {
                                                                                       psTimeRemainingInstant *= -1.0;
                                                                                    }
                                                                                 }

                                                                                 batteryFound = true;
                                                                              }
                                                                           }
                                                                        }
                                                                     } catch (Throwable var63) {
                                                                        try {
                                                                           bmd.close();
                                                                        } catch (Throwable var57) {
                                                                           var63.addSuppressed(var57);
                                                                        }

                                                                        throw var63;
                                                                     }

                                                                     bmd.close();
                                                                  } catch (Throwable var64) {
                                                                     try {
                                                                        bs.close();
                                                                     } catch (Throwable var56) {
                                                                        var64.addSuppressed(var56);
                                                                     }

                                                                     throw var64;
                                                                  }

                                                                  bs.close();
                                                               } catch (Throwable var65) {
                                                                  try {
                                                                     bws.close();
                                                                  } catch (Throwable var55) {
                                                                     var65.addSuppressed(var55);
                                                                  }

                                                                  throw var65;
                                                               }

                                                               bws.close();
                                                            } catch (Throwable var66) {
                                                               try {
                                                                  bi.close();
                                                               } catch (Throwable var54) {
                                                                  var66.addSuppressed(var54);
                                                               }

                                                               throw var66;
                                                            }

                                                            bi.close();
                                                         } catch (Throwable var67) {
                                                            try {
                                                               bqi.close();
                                                            } catch (Throwable var53) {
                                                               var67.addSuppressed(var53);
                                                            }

                                                            throw var67;
                                                         }

                                                         bqi.close();
                                                         Kernel32.INSTANCE.CloseHandle(hBattery);
                                                      }
                                                   }
                                                } catch (Throwable var68) {
                                                   try {
                                                      pdidd.close();
                                                   } catch (Throwable var52) {
                                                      var68.addSuppressed(var52);
                                                   }

                                                   throw var68;
                                                }

                                                pdidd.close();
                                             }
                                          } else if (259 == Kernel32.INSTANCE.GetLastError()) {
                                             break label337;
                                          }
                                       } catch (Throwable var69) {
                                          try {
                                             dwOut.close();
                                          } catch (Throwable var51) {
                                             var69.addSuppressed(var51);
                                          }

                                          throw var69;
                                       }

                                       dwOut.close();
                                       break label380;
                                    }

                                    dwOut.close();
                                 } catch (Throwable var70) {
                                    try {
                                       dwTag.close();
                                    } catch (Throwable var50) {
                                       var70.addSuppressed(var50);
                                    }

                                    throw var70;
                                 }

                                 dwTag.close();
                                 break label345;
                              }

                              dwTag.close();
                           } catch (Throwable var71) {
                              try {
                                 dwWait.close();
                              } catch (Throwable var49) {
                                 var71.addSuppressed(var49);
                              }

                              throw var71;
                           }

                           dwWait.close();
                           break label378;
                        }

                        dwWait.close();
                     } catch (Throwable var72) {
                        try {
                           requiredSize.close();
                        } catch (Throwable var48) {
                           var72.addSuppressed(var48);
                        }

                        throw var72;
                     }

                     requiredSize.close();
                     break;
                  }

                  requiredSize.close();
               } catch (Throwable var73) {
                  try {
                     did.close();
                  } catch (Throwable var47) {
                     var73.addSuppressed(var47);
                  }

                  throw var73;
               }

               did.close();
               idev++;
            }

            did.close();
         }

         SetupApi.INSTANCE.SetupDiDestroyDeviceInfoList(var75);
      }

      return new WindowsPowerSource(
         name,
         psDeviceName,
         psRemainingCapacityPercent,
         psTimeRemainingEstimated,
         psTimeRemainingInstant,
         psPowerUsageRate,
         psVoltage,
         psAmperage,
         psPowerOnLine,
         psCharging,
         psDischarging,
         psCapacityUnits,
         psCurrentCapacity,
         psMaxCapacity,
         psDesignCapacity,
         psCycleCount,
         psChemistry,
         psManufactureDate,
         psManufacturer,
         psSerialNumber,
         psTemperature
      );
   }

   private static String batteryQueryString(WinNT.HANDLE hBattery, int tag, int infoLevel) {
      PowrProf.BATTERY_QUERY_INFORMATION bqi = new PowrProf.BATTERY_QUERY_INFORMATION();

      String name;
      label65: {
         String var10;
         try {
            ByRef.CloseableIntByReference dwOut;
            label69: {
               dwOut = new ByRef.CloseableIntByReference();

               try {
                  bqi.BatteryTag = tag;
                  bqi.InformationLevel = infoLevel;
                  bqi.write();
                  boolean ret = false;
                  long bufSize = 256L;
                  Memory nameBuf = new Memory(bufSize);

                  while (true) {
                     ret = Kernel32.INSTANCE.DeviceIoControl(hBattery, 2703428, bqi.getPointer(), bqi.size(), nameBuf, (int)nameBuf.size(), dwOut, null);
                     if (!ret) {
                        bufSize += 256L;
                        nameBuf.close();
                        if (bufSize > 4096L) {
                           name = "";
                           break;
                        }

                        nameBuf = new Memory(bufSize);
                     }

                     if (ret) {
                        name = CHAR_WIDTH > 1 ? nameBuf.getWideString(0L) : nameBuf.getString(0L);
                        nameBuf.close();
                        var10 = name;
                        break label69;
                     }
                  }
               } catch (Throwable var13) {
                  try {
                     dwOut.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               dwOut.close();
               break label65;
            }

            dwOut.close();
         } catch (Throwable var14) {
            try {
               bqi.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }

            throw var14;
         }

         bqi.close();
         return var10;
      }

      bqi.close();
      return name;
   }
}
