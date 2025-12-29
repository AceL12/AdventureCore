package oshi.util;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class Util {
   private static final Logger LOG = LoggerFactory.getLogger(Util.class);

   private Util() {
   }

   public static void sleep(long ms) {
      try {
         LOG.trace("Sleeping for {} ms", ms);
         Thread.sleep(ms);
      } catch (InterruptedException var3) {
         LOG.warn("Interrupted while sleeping for {} ms: {}", ms, var3.getMessage());
         Thread.currentThread().interrupt();
      }
   }

   public static boolean wildcardMatch(String text, String pattern) {
      return pattern.length() > 0 && pattern.charAt(0) == '^'
         ? !wildcardMatch(text, pattern.substring(1))
         : text.matches(pattern.replace("?", ".?").replace("*", ".*?"));
   }

   public static boolean isBlank(String s) {
      return s == null || s.isEmpty();
   }

   public static boolean isBlankOrUnknown(String s) {
      return isBlank(s) || "unknown".equals(s);
   }

   public static void freeMemory(Pointer p) {
      if (p instanceof Memory) {
         ((Memory)p).close();
      }
   }
}
