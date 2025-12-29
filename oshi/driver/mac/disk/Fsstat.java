package oshi.driver.mac.disk;

import com.sun.jna.Native;
import com.sun.jna.platform.mac.SystemB;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class Fsstat {
   private Fsstat() {
   }

   public static Map<String, String> queryPartitionToMountMap() {
      Map<String, String> mountPointMap = new HashMap<>();
      int numfs = queryFsstat(null, 0, 0);
      SystemB.Statfs s = new SystemB.Statfs();
      SystemB.Statfs[] fs = (SystemB.Statfs[])s.toArray(numfs);
      queryFsstat(fs, numfs * fs[0].size(), 16);

      for (SystemB.Statfs f : fs) {
         String mntFrom = Native.toString(f.f_mntfromname, StandardCharsets.UTF_8);
         mountPointMap.put(mntFrom.replace("/dev/", ""), Native.toString(f.f_mntonname, StandardCharsets.UTF_8));
      }

      return mountPointMap;
   }

   private static int queryFsstat(SystemB.Statfs[] buf, int bufsize, int flags) {
      return SystemB.INSTANCE.getfsstat64(buf, bufsize, flags);
   }
}
