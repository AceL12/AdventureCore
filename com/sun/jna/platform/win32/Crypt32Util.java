package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import java.util.Arrays;

public abstract class Crypt32Util {
   public static byte[] cryptProtectData(byte[] data) {
      return cryptProtectData(data, 0);
   }

   public static byte[] cryptProtectData(byte[] data, int flags) {
      return cryptProtectData(data, null, flags, "", null);
   }

   public static byte[] cryptProtectData(byte[] data, byte[] entropy, int flags, String description, WinCrypt.CRYPTPROTECT_PROMPTSTRUCT prompt) {
      WinCrypt.DATA_BLOB pDataIn = new WinCrypt.DATA_BLOB(data);
      WinCrypt.DATA_BLOB pDataProtected = new WinCrypt.DATA_BLOB();
      WinCrypt.DATA_BLOB pEntropy = entropy == null ? null : new WinCrypt.DATA_BLOB(entropy);
      Win32Exception err = null;
      byte[] protectedData = null;

      try {
         if (!Crypt32.INSTANCE.CryptProtectData(pDataIn, description, pEntropy, null, prompt, flags, pDataProtected)) {
            err = new Win32Exception(Kernel32.INSTANCE.GetLastError());
         } else {
            protectedData = pDataProtected.getData();
         }
      } finally {
         if (pDataIn.pbData != null) {
            pDataIn.pbData.clear(pDataIn.cbData);
         }

         if (pEntropy != null && pEntropy.pbData != null) {
            pEntropy.pbData.clear(pEntropy.cbData);
         }

         if (pDataProtected.pbData != null) {
            pDataProtected.pbData.clear(pDataProtected.cbData);

            try {
               Kernel32Util.freeLocalMemory(pDataProtected.pbData);
            } catch (Win32Exception var16) {
               if (err != null) {
                  err.addSuppressedReflected(var16);
               }
            }
         }
      }

      if (err != null) {
         if (protectedData != null) {
            Arrays.fill(protectedData, (byte)0);
         }

         throw err;
      } else {
         return protectedData;
      }
   }

   public static byte[] cryptUnprotectData(byte[] data) {
      return cryptUnprotectData(data, 0);
   }

   public static byte[] cryptUnprotectData(byte[] data, int flags) {
      return cryptUnprotectData(data, null, flags, null);
   }

   public static byte[] cryptUnprotectData(byte[] data, byte[] entropy, int flags, WinCrypt.CRYPTPROTECT_PROMPTSTRUCT prompt) {
      WinCrypt.DATA_BLOB pDataIn = new WinCrypt.DATA_BLOB(data);
      WinCrypt.DATA_BLOB pDataUnprotected = new WinCrypt.DATA_BLOB();
      WinCrypt.DATA_BLOB pEntropy = entropy == null ? null : new WinCrypt.DATA_BLOB(entropy);
      Win32Exception err = null;
      byte[] unProtectedData = null;

      try {
         if (!Crypt32.INSTANCE.CryptUnprotectData(pDataIn, null, pEntropy, null, prompt, flags, pDataUnprotected)) {
            err = new Win32Exception(Kernel32.INSTANCE.GetLastError());
         } else {
            unProtectedData = pDataUnprotected.getData();
         }
      } finally {
         if (pDataIn.pbData != null) {
            pDataIn.pbData.clear(pDataIn.cbData);
         }

         if (pEntropy != null && pEntropy.pbData != null) {
            pEntropy.pbData.clear(pEntropy.cbData);
         }

         if (pDataUnprotected.pbData != null) {
            pDataUnprotected.pbData.clear(pDataUnprotected.cbData);

            try {
               Kernel32Util.freeLocalMemory(pDataUnprotected.pbData);
            } catch (Win32Exception var15) {
               if (err != null) {
                  err.addSuppressedReflected(var15);
               }
            }
         }
      }

      if (err != null) {
         if (unProtectedData != null) {
            Arrays.fill(unProtectedData, (byte)0);
         }

         throw err;
      } else {
         return unProtectedData;
      }
   }

   public static String CertNameToStr(int dwCertEncodingType, int dwStrType, WinCrypt.DATA_BLOB pName) {
      int charToBytes = Boolean.getBoolean("w32.ascii") ? 1 : Native.WCHAR_SIZE;
      int requiredSize = Crypt32.INSTANCE.CertNameToStr(dwCertEncodingType, pName, dwStrType, Pointer.NULL, 0);
      Memory mem = new Memory(requiredSize * charToBytes);
      int resultBytes = Crypt32.INSTANCE.CertNameToStr(dwCertEncodingType, pName, dwStrType, mem, requiredSize);

      assert resultBytes == requiredSize;

      return Boolean.getBoolean("w32.ascii") ? mem.getString(0L) : mem.getWideString(0L);
   }
}
