package com.sun.jna.platform.win32;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WininetUtil {
   public static Map<String, String> getCache() {
      List<Wininet.INTERNET_CACHE_ENTRY_INFO> items = new ArrayList<>();
      WinNT.HANDLE cacheHandle = null;
      Win32Exception we = null;
      int lastError = 0;
      Map<String, String> cacheItems = new LinkedHashMap<>();

      try {
         IntByReference size = new IntByReference();
         cacheHandle = Wininet.INSTANCE.FindFirstUrlCacheEntry(null, null, size);
         lastError = Native.getLastError();
         if (lastError == 259) {
            return cacheItems;
         }

         if (lastError != 0 && lastError != 122) {
            throw new Win32Exception(lastError);
         }

         Wininet.INTERNET_CACHE_ENTRY_INFO entry = new Wininet.INTERNET_CACHE_ENTRY_INFO(size.getValue());
         cacheHandle = Wininet.INSTANCE.FindFirstUrlCacheEntry(null, entry, size);
         if (cacheHandle == null) {
            throw new Win32Exception(Native.getLastError());
         }

         items.add(entry);

         while (true) {
            size = new IntByReference();
            boolean result = Wininet.INSTANCE.FindNextUrlCacheEntry(cacheHandle, null, size);
            if (!result) {
               lastError = Native.getLastError();
               if (lastError == 259) {
                  break;
               }

               if (lastError != 0 && lastError != 122) {
                  throw new Win32Exception(lastError);
               }
            }

            entry = new Wininet.INTERNET_CACHE_ENTRY_INFO(size.getValue());
            result = Wininet.INSTANCE.FindNextUrlCacheEntry(cacheHandle, entry, size);
            if (!result) {
               lastError = Native.getLastError();
               if (lastError == 259) {
                  break;
               }

               if (lastError != 0 && lastError != 122) {
                  throw new Win32Exception(lastError);
               }
            }

            items.add(entry);
         }

         for (Wininet.INTERNET_CACHE_ENTRY_INFO item : items) {
            cacheItems.put(item.lpszSourceUrlName.getWideString(0L), item.lpszLocalFileName == null ? "" : item.lpszLocalFileName.getWideString(0L));
         }
      } catch (Win32Exception var13) {
         we = var13;
      } finally {
         if (cacheHandle != null && !Wininet.INSTANCE.FindCloseUrlCache(cacheHandle) && we != null) {
            Win32Exception e = new Win32Exception(Native.getLastError());
            e.addSuppressedReflected(we);
         }
      }

      if (we != null) {
         throw we;
      } else {
         return cacheItems;
      }
   }
}
