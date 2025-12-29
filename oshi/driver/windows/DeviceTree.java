package oshi.driver.windows;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Cfgmgr32;
import com.sun.jna.platform.win32.Cfgmgr32Util;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.jna.Struct;
import oshi.util.tuples.Quintet;

@ThreadSafe
public final class DeviceTree {
   private static final int MAX_PATH = 260;
   private static final SetupApi SA = SetupApi.INSTANCE;
   private static final Cfgmgr32 C32 = Cfgmgr32.INSTANCE;

   private DeviceTree() {
   }

   public static Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> queryDeviceTree(
      Guid.GUID guidDevInterface
   ) {
      Map<Integer, Integer> parentMap = new HashMap<>();
      Map<Integer, String> nameMap = new HashMap<>();
      Map<Integer, String> deviceIdMap = new HashMap<>();
      Map<Integer, String> mfgMap = new HashMap<>();
      WinNT.HANDLE hDevInfo = SA.SetupDiGetClassDevs(guidDevInterface, null, null, 18);
      if (!WinBase.INVALID_HANDLE_VALUE.equals(hDevInfo)) {
         try {
            Memory buf = new Memory(260L);

            try {
               ByRef.CloseableIntByReference size = new ByRef.CloseableIntByReference(260);

               try {
                  ByRef.CloseableIntByReference child = new ByRef.CloseableIntByReference();

                  try {
                     ByRef.CloseableIntByReference sibling = new ByRef.CloseableIntByReference();

                     try {
                        Struct.CloseableSpDevinfoData devInfoData = new Struct.CloseableSpDevinfoData();

                        try {
                           devInfoData.cbSize = devInfoData.size();
                           Queue<Integer> deviceTree = new ArrayDeque<>();

                           for (int i = 0; SA.SetupDiEnumDeviceInfo(hDevInfo, i, devInfoData); i++) {
                              deviceTree.add(devInfoData.DevInst);
                              int node = 0;

                              while (!deviceTree.isEmpty()) {
                                 node = deviceTree.poll();
                                 String deviceId = Cfgmgr32Util.CM_Get_Device_ID(node);
                                 deviceIdMap.put(node, deviceId);
                                 String name = getDevNodeProperty(node, 13, buf, size);
                                 if (name.isEmpty()) {
                                    name = getDevNodeProperty(node, 1, buf, size);
                                 }

                                 if (name.isEmpty()) {
                                    name = getDevNodeProperty(node, 8, buf, size);
                                    String svc = getDevNodeProperty(node, 5, buf, size);
                                    if (!svc.isEmpty()) {
                                       name = name + " (" + svc + ")";
                                    }
                                 }

                                 nameMap.put(node, name);
                                 mfgMap.put(node, getDevNodeProperty(node, 12, buf, size));
                                 if (0 == C32.CM_Get_Child(child, node, 0)) {
                                    parentMap.put(child.getValue(), node);
                                    deviceTree.add(child.getValue());

                                    while (0 == C32.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                                       parentMap.put(sibling.getValue(), node);
                                       deviceTree.add(sibling.getValue());
                                       child.setValue(sibling.getValue());
                                    }
                                 }
                              }
                           }
                        } catch (Throwable var34) {
                           try {
                              devInfoData.close();
                           } catch (Throwable var33) {
                              var34.addSuppressed(var33);
                           }

                           throw var34;
                        }

                        devInfoData.close();
                     } catch (Throwable var35) {
                        try {
                           sibling.close();
                        } catch (Throwable var32) {
                           var35.addSuppressed(var32);
                        }

                        throw var35;
                     }

                     sibling.close();
                  } catch (Throwable var36) {
                     try {
                        child.close();
                     } catch (Throwable var31) {
                        var36.addSuppressed(var31);
                     }

                     throw var36;
                  }

                  child.close();
               } catch (Throwable var37) {
                  try {
                     size.close();
                  } catch (Throwable var30) {
                     var37.addSuppressed(var30);
                  }

                  throw var37;
               }

               size.close();
            } catch (Throwable var38) {
               try {
                  buf.close();
               } catch (Throwable var29) {
                  var38.addSuppressed(var29);
               }

               throw var38;
            }

            buf.close();
         } finally {
            SA.SetupDiDestroyDeviceInfoList(hDevInfo);
         }
      }

      Set<Integer> controllerDevices = deviceIdMap.keySet().stream().filter(k -> !parentMap.containsKey(k)).collect(Collectors.toSet());
      return new Quintet<>(controllerDevices, parentMap, nameMap, deviceIdMap, mfgMap);
   }

   private static String getDevNodeProperty(int node, int cmDrp, Memory buf, IntByReference size) {
      buf.clear();
      size.setValue((int)buf.size());
      C32.CM_Get_DevNode_Registry_Property(node, cmDrp, null, buf, size, 0);
      return buf.getWideString(0L);
   }
}
