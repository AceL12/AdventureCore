package com.endernerds.AdventureCore;

public class AuthStorage {
   private static String storedValue;
   private static AuthStorage instance;

   public static AuthStorage getInstance() {
      if (instance == null) {
         instance = new AuthStorage();
      }

      return instance;
   }

   public void setValue(String value) {
      storedValue = value;
   }

   public String getValue() {
      return storedValue;
   }

   public static String getStoredValueForSkript() {
      return storedValue;
   }
}
