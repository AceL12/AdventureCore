package oshi.jna.platform.unix;

import com.sun.jna.Native;
import java.nio.ByteBuffer;
import oshi.util.FileUtil;

public interface AixLibc extends CLibrary {
   AixLibc INSTANCE = Native.load("c", AixLibc.class);
   int PRCLSZ = 8;
   int PRFNSZ = 16;
   int PRARGSZ = 80;

   public static class AixLwpsInfo {
      public long pr_lwpid;
      public long pr_addr;
      public long pr_wchan;
      public int pr_flag;
      public byte pr_wtype;
      public byte pr_state;
      public byte pr_sname;
      public byte pr_nice;
      public int pr_pri;
      public int pr_policy;
      public byte[] pr_clname = new byte[8];
      public int pr_onpro;
      public int pr_bindpro;

      public AixLwpsInfo(ByteBuffer buff) {
         this.pr_lwpid = FileUtil.readLongFromBuffer(buff);
         this.pr_addr = FileUtil.readLongFromBuffer(buff);
         this.pr_wchan = FileUtil.readLongFromBuffer(buff);
         this.pr_flag = FileUtil.readIntFromBuffer(buff);
         this.pr_wtype = FileUtil.readByteFromBuffer(buff);
         this.pr_state = FileUtil.readByteFromBuffer(buff);
         this.pr_sname = FileUtil.readByteFromBuffer(buff);
         this.pr_nice = FileUtil.readByteFromBuffer(buff);
         this.pr_pri = FileUtil.readIntFromBuffer(buff);
         this.pr_policy = FileUtil.readIntFromBuffer(buff);
         FileUtil.readByteArrayFromBuffer(buff, this.pr_clname);
         this.pr_onpro = FileUtil.readIntFromBuffer(buff);
         this.pr_bindpro = FileUtil.readIntFromBuffer(buff);
      }
   }

   public static class AixPsInfo {
      public int pr_flag;
      public int pr_flag2;
      public int pr_nlwp;
      public int pr__pad1;
      public long pr_uid;
      public long pr_euid;
      public long pr_gid;
      public long pr_egid;
      public long pr_pid;
      public long pr_ppid;
      public long pr_pgid;
      public long pr_sid;
      public long pr_ttydev;
      public long pr_addr;
      public long pr_size;
      public long pr_rssize;
      public AixLibc.Timestruc pr_start;
      public AixLibc.Timestruc pr_time;
      public short pr_cid;
      public short pr__pad2;
      public int pr_argc;
      public long pr_argv;
      public long pr_envp;
      public byte[] pr_fname = new byte[16];
      public byte[] pr_psargs = new byte[80];
      public long[] pr__pad = new long[8];
      public AixLibc.AixLwpsInfo pr_lwp;

      public AixPsInfo(ByteBuffer buff) {
         this.pr_flag = FileUtil.readIntFromBuffer(buff);
         this.pr_flag2 = FileUtil.readIntFromBuffer(buff);
         this.pr_nlwp = FileUtil.readIntFromBuffer(buff);
         this.pr__pad1 = FileUtil.readIntFromBuffer(buff);
         this.pr_uid = FileUtil.readLongFromBuffer(buff);
         this.pr_euid = FileUtil.readLongFromBuffer(buff);
         this.pr_gid = FileUtil.readLongFromBuffer(buff);
         this.pr_egid = FileUtil.readLongFromBuffer(buff);
         this.pr_pid = FileUtil.readLongFromBuffer(buff);
         this.pr_ppid = FileUtil.readLongFromBuffer(buff);
         this.pr_pgid = FileUtil.readLongFromBuffer(buff);
         this.pr_sid = FileUtil.readLongFromBuffer(buff);
         this.pr_ttydev = FileUtil.readLongFromBuffer(buff);
         this.pr_addr = FileUtil.readLongFromBuffer(buff);
         this.pr_size = FileUtil.readLongFromBuffer(buff);
         this.pr_rssize = FileUtil.readLongFromBuffer(buff);
         this.pr_start = new AixLibc.Timestruc(buff);
         this.pr_time = new AixLibc.Timestruc(buff);
         this.pr_cid = FileUtil.readShortFromBuffer(buff);
         this.pr__pad2 = FileUtil.readShortFromBuffer(buff);
         this.pr_argc = FileUtil.readIntFromBuffer(buff);
         this.pr_argv = FileUtil.readLongFromBuffer(buff);
         this.pr_envp = FileUtil.readLongFromBuffer(buff);
         FileUtil.readByteArrayFromBuffer(buff, this.pr_fname);
         FileUtil.readByteArrayFromBuffer(buff, this.pr_psargs);

         for (int i = 0; i < this.pr__pad.length; i++) {
            this.pr__pad[i] = FileUtil.readLongFromBuffer(buff);
         }

         this.pr_lwp = new AixLibc.AixLwpsInfo(buff);
      }
   }

   public static class Timestruc {
      public long tv_sec;
      public int tv_nsec;
      public int pad;

      public Timestruc(ByteBuffer buff) {
         this.tv_sec = FileUtil.readLongFromBuffer(buff);
         this.tv_nsec = FileUtil.readIntFromBuffer(buff);
         this.pad = FileUtil.readIntFromBuffer(buff);
      }
   }
}
