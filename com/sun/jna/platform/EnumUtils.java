package com.sun.jna.platform;

import com.sun.jna.platform.win32.FlagEnum;
import java.util.HashSet;
import java.util.Set;

public class EnumUtils {
   public static final int UNINITIALIZED = -1;

   public static <E extends Enum<E>> int toInteger(E val) {
      E[] vals = (E[])val.getClass().getEnumConstants();

      for (int idx = 0; idx < vals.length; idx++) {
         if (vals[idx] == val) {
            return idx;
         }
      }

      throw new IllegalArgumentException();
   }

   public static <E extends Enum<E>> E fromInteger(int idx, Class<E> clazz) {
      if (idx == -1) {
         return null;
      } else {
         E[] vals = (E[])clazz.getEnumConstants();
         return vals[idx];
      }
   }

   public static <T extends FlagEnum> Set<T> setFromInteger(int flags, Class<T> clazz) {
      T[] vals = (T[])clazz.getEnumConstants();
      Set<T> result = new HashSet<>();

      for (T val : vals) {
         if ((flags & val.getFlag()) != 0) {
            result.add(val);
         }
      }

      return result;
   }

   public static <T extends FlagEnum> int setToInteger(Set<T> set) {
      int sum = 0;

      for (T t : set) {
         sum |= t.getFlag();
      }

      return sum;
   }
}
