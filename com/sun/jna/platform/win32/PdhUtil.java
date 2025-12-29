package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import java.util.ArrayList;
import java.util.List;

public abstract class PdhUtil {
   private static final int CHAR_TO_BYTES = Boolean.getBoolean("w32.ascii") ? 1 : Native.WCHAR_SIZE;
   private static final String ENGLISH_COUNTER_KEY = "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009";
   private static final String ENGLISH_COUNTER_VALUE = "Counter";

   public static String PdhLookupPerfNameByIndex(String szMachineName, int dwNameIndex) {
      WinDef.DWORDByReference pcchNameBufferSize = new WinDef.DWORDByReference(new WinDef.DWORD(0L));
      int result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, null, pcchNameBufferSize);
      Memory mem = null;
      if (result != -1073738819) {
         if (result != 0 && result != -2147481646) {
            throw new PdhUtil.PdhException(result);
         }

         if (pcchNameBufferSize.getValue().intValue() < 1) {
            return "";
         }

         mem = new Memory(pcchNameBufferSize.getValue().intValue() * CHAR_TO_BYTES);
         result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, mem, pcchNameBufferSize);
      } else {
         for (int bufferSize = 32; bufferSize <= 1024; bufferSize *= 2) {
            pcchNameBufferSize = new WinDef.DWORDByReference(new WinDef.DWORD(bufferSize));
            mem = new Memory(bufferSize * CHAR_TO_BYTES);
            result = Pdh.INSTANCE.PdhLookupPerfNameByIndex(szMachineName, dwNameIndex, mem, pcchNameBufferSize);
            if (result != -1073738819 && result != -1073738814) {
               break;
            }
         }
      }

      if (result != 0) {
         throw new PdhUtil.PdhException(result);
      } else {
         return CHAR_TO_BYTES == 1 ? mem.getString(0L) : mem.getWideString(0L);
      }
   }

   public static int PdhLookupPerfIndexByEnglishName(String szNameBuffer) {
      String[] counters = Advapi32Util.registryGetStringArray(
         WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Perflib\\009", "Counter"
      );

      for (int i = 1; i < counters.length; i += 2) {
         if (counters[i].equals(szNameBuffer)) {
            try {
               return Integer.parseInt(counters[i - 1]);
            } catch (NumberFormatException var4) {
               return 0;
            }
         }
      }

      return 0;
   }

   public static PdhUtil.PdhEnumObjectItems PdhEnumObjectItems(String szDataSource, String szMachineName, String szObjectName, int dwDetailLevel) {
      List<String> counters = new ArrayList<>();
      List<String> instances = new ArrayList<>();
      WinDef.DWORDByReference pcchCounterListLength = new WinDef.DWORDByReference(new WinDef.DWORD(0L));
      WinDef.DWORDByReference pcchInstanceListLength = new WinDef.DWORDByReference(new WinDef.DWORD(0L));
      int result = Pdh.INSTANCE
         .PdhEnumObjectItems(szDataSource, szMachineName, szObjectName, null, pcchCounterListLength, null, pcchInstanceListLength, dwDetailLevel, 0);
      if (result != 0 && result != -2147481646) {
         throw new PdhUtil.PdhException(result);
      } else {
         Memory mszCounterList = null;
         Memory mszInstanceList = null;

         do {
            if (pcchCounterListLength.getValue().intValue() > 0) {
               mszCounterList = new Memory(pcchCounterListLength.getValue().intValue() * CHAR_TO_BYTES);
            }

            if (pcchInstanceListLength.getValue().intValue() > 0) {
               mszInstanceList = new Memory(pcchInstanceListLength.getValue().intValue() * CHAR_TO_BYTES);
            }

            result = Pdh.INSTANCE
               .PdhEnumObjectItems(
                  szDataSource, szMachineName, szObjectName, mszCounterList, pcchCounterListLength, mszInstanceList, pcchInstanceListLength, dwDetailLevel, 0
               );
            if (result == -2147481646) {
               if (mszCounterList != null) {
                  long tooSmallSize = mszCounterList.size() / CHAR_TO_BYTES;
                  pcchCounterListLength.setValue(new WinDef.DWORD(tooSmallSize + 1024L));
                  mszCounterList.close();
               }

               if (mszInstanceList != null) {
                  long tooSmallSize = mszInstanceList.size() / CHAR_TO_BYTES;
                  pcchInstanceListLength.setValue(new WinDef.DWORD(tooSmallSize + 1024L));
                  mszInstanceList.close();
               }
            }
         } while (result == -2147481646);

         if (result != 0) {
            throw new PdhUtil.PdhException(result);
         } else {
            if (mszCounterList != null) {
               int offset = 0;

               while (offset < mszCounterList.size()) {
                  String s = null;
                  if (CHAR_TO_BYTES == 1) {
                     s = mszCounterList.getString(offset);
                  } else {
                     s = mszCounterList.getWideString(offset);
                  }

                  if (s.isEmpty()) {
                     break;
                  }

                  counters.add(s);
                  offset += (s.length() + 1) * CHAR_TO_BYTES;
               }
            }

            if (mszInstanceList != null) {
               int offset = 0;

               while (offset < mszInstanceList.size()) {
                  String sx = null;
                  if (CHAR_TO_BYTES == 1) {
                     sx = mszInstanceList.getString(offset);
                  } else {
                     sx = mszInstanceList.getWideString(offset);
                  }

                  if (sx.isEmpty()) {
                     break;
                  }

                  instances.add(sx);
                  offset += (sx.length() + 1) * CHAR_TO_BYTES;
               }
            }

            return new PdhUtil.PdhEnumObjectItems(counters, instances);
         }
      }
   }

   public static class PdhEnumObjectItems {
      private final List<String> counters;
      private final List<String> instances;

      public PdhEnumObjectItems(List<String> counters, List<String> instances) {
         this.counters = this.copyAndEmptyListForNullList(counters);
         this.instances = this.copyAndEmptyListForNullList(instances);
      }

      public List<String> getCounters() {
         return this.counters;
      }

      public List<String> getInstances() {
         return this.instances;
      }

      private List<String> copyAndEmptyListForNullList(List<String> inputList) {
         return inputList == null ? new ArrayList<>() : new ArrayList<>(inputList);
      }

      @Override
      public String toString() {
         return "PdhEnumObjectItems{counters=" + this.counters + ", instances=" + this.instances + '}';
      }
   }

   public static final class PdhException extends RuntimeException {
      private final int errorCode;

      public PdhException(int errorCode) {
         super(String.format("Pdh call failed with error code 0x%08X", errorCode));
         this.errorCode = errorCode;
      }

      public int getErrorCode() {
         return this.errorCode;
      }
   }
}
