package oshi.driver.windows.registry;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Netapi32;
import java.util.ArrayList;
import java.util.List;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.ByRef;
import oshi.software.os.OSSession;

@ThreadSafe
public final class NetSessionData {
   private static final Netapi32 NET = Netapi32.INSTANCE;

   private NetSessionData() {
   }

   public static List<OSSession> queryUserSessions() {
      List<OSSession> sessions = new ArrayList<>();
      ByRef.CloseablePointerByReference bufptr = new ByRef.CloseablePointerByReference();

      try {
         ByRef.CloseableIntByReference entriesread = new ByRef.CloseableIntByReference();

         try {
            ByRef.CloseableIntByReference totalentries = new ByRef.CloseableIntByReference();

            try {
               if (0 == NET.NetSessionEnum(null, null, null, 10, bufptr, -1, entriesread, totalentries, null)) {
                  Pointer buf = bufptr.getValue();
                  Netapi32.SESSION_INFO_10 si10 = new Netapi32.SESSION_INFO_10(buf);
                  if (entriesread.getValue() > 0) {
                     Netapi32.SESSION_INFO_10[] sessionInfo = (Netapi32.SESSION_INFO_10[])si10.toArray(entriesread.getValue());

                     for (Netapi32.SESSION_INFO_10 si : sessionInfo) {
                        long logonTime = System.currentTimeMillis() - 1000L * si.sesi10_time;
                        sessions.add(new OSSession(si.sesi10_username, "Network session", logonTime, si.sesi10_cname));
                     }
                  }

                  NET.NetApiBufferFree(buf);
               }
            } catch (Throwable var16) {
               try {
                  totalentries.close();
               } catch (Throwable var15) {
                  var16.addSuppressed(var15);
               }

               throw var16;
            }

            totalentries.close();
         } catch (Throwable var17) {
            try {
               entriesread.close();
            } catch (Throwable var14) {
               var17.addSuppressed(var14);
            }

            throw var17;
         }

         entriesread.close();
      } catch (Throwable var18) {
         try {
            bufptr.close();
         } catch (Throwable var13) {
            var18.addSuppressed(var13);
         }

         throw var18;
      }

      bufptr.close();
      return sessions;
   }
}
