package com.sun.jna;

import java.lang.reflect.InvocationTargetException;

abstract class Klass {
   private Klass() {
   }

   public static <T> T newInstance(Class<T> klass) {
      try {
         return klass.getDeclaredConstructor().newInstance();
      } catch (IllegalAccessException var3) {
         String msg = "Can't create an instance of " + klass + ", requires a public no-arg constructor: " + var3;
         throw new IllegalArgumentException(msg, var3);
      } catch (IllegalArgumentException var4) {
         String msgx = "Can't create an instance of " + klass + ", requires a public no-arg constructor: " + var4;
         throw new IllegalArgumentException(msgx, var4);
      } catch (InstantiationException var5) {
         String msgxx = "Can't create an instance of " + klass + ", requires a public no-arg constructor: " + var5;
         throw new IllegalArgumentException(msgxx, var5);
      } catch (NoSuchMethodException var6) {
         String msgxxx = "Can't create an instance of " + klass + ", requires a public no-arg constructor: " + var6;
         throw new IllegalArgumentException(msgxxx, var6);
      } catch (SecurityException var7) {
         String msgxxxx = "Can't create an instance of " + klass + ", requires a public no-arg constructor: " + var7;
         throw new IllegalArgumentException(msgxxxx, var7);
      } catch (InvocationTargetException var8) {
         if (var8.getCause() instanceof RuntimeException) {
            throw (RuntimeException)var8.getCause();
         } else {
            String msgxxxxx = "Can't create an instance of " + klass + ", requires a public no-arg constructor: " + var8;
            throw new IllegalArgumentException(msgxxxxx, var8);
         }
      }
   }
}
