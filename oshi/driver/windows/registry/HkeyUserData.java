package oshi.driver.windows.registry;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.os.OSSession;

@ThreadSafe
public final class HkeyUserData {
   private static final String PATH_DELIMITER = "\\";
   private static final String DEFAULT_DEVICE = "Console";
   private static final String VOLATILE_ENV_SUBKEY = "Volatile Environment";
   private static final String CLIENTNAME = "CLIENTNAME";
   private static final String SESSIONNAME = "SESSIONNAME";
   private static final Logger LOG = LoggerFactory.getLogger(HkeyUserData.class);

   private HkeyUserData() {
   }

   public static List<OSSession> queryUserSessions() {
      List<OSSession> sessions = new ArrayList<>();

      for (String sidKey : Advapi32Util.registryGetKeys(WinReg.HKEY_USERS)) {
         if (!sidKey.startsWith(".") && !sidKey.endsWith("_Classes")) {
            try {
               Advapi32Util.Account a = Advapi32Util.getAccountBySid(sidKey);
               String name = a.name;
               String device = "Console";
               String host = a.domain;
               long loginTime = 0L;
               String keyPath = sidKey + "\\" + "Volatile Environment";
               if (Advapi32Util.registryKeyExists(WinReg.HKEY_USERS, keyPath)) {
                  WinReg.HKEY hKey = Advapi32Util.registryGetKey(WinReg.HKEY_USERS, keyPath, 131097).getValue();
                  Advapi32Util.InfoKey info = Advapi32Util.registryQueryInfoKey(hKey, 0);
                  loginTime = info.lpftLastWriteTime.toTime();

                  for (String subKey : Advapi32Util.registryGetKeys(hKey)) {
                     String subKeyPath = keyPath + "\\" + subKey;
                     if (Advapi32Util.registryValueExists(WinReg.HKEY_USERS, subKeyPath, "SESSIONNAME")) {
                        String session = Advapi32Util.registryGetStringValue(WinReg.HKEY_USERS, subKeyPath, "SESSIONNAME");
                        if (!session.isEmpty()) {
                           device = session;
                        }
                     }

                     if (Advapi32Util.registryValueExists(WinReg.HKEY_USERS, subKeyPath, "CLIENTNAME")) {
                        String client = Advapi32Util.registryGetStringValue(WinReg.HKEY_USERS, subKeyPath, "CLIENTNAME");
                        if (!client.isEmpty() && !"Console".equals(client)) {
                           host = client;
                        }
                     }
                  }

                  Advapi32Util.registryCloseKey(hKey);
               }

               sessions.add(new OSSession(name, device, loginTime, host));
            } catch (Win32Exception var20) {
               LOG.warn("Error querying SID {} from registry: {}", sidKey, var20.getMessage());
            }
         }
      }

      return sessions;
   }
}
