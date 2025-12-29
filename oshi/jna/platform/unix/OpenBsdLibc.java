package oshi.jna.platform.unix;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface OpenBsdLibc extends CLibrary {
   OpenBsdLibc INSTANCE = Native.load(null, OpenBsdLibc.class);
   int CTL_KERN = 1;
   int CTL_VM = 1;
   int CTL_HW = 6;
   int CTL_MACHDEP = 7;
   int CTL_VFS = 10;
   int KERN_OSTYPE = 1;
   int KERN_OSRELEASE = 2;
   int KERN_OSREV = 3;
   int KERN_VERSION = 4;
   int KERN_MAXVNODES = 5;
   int KERN_MAXPROC = 6;
   int KERN_ARGMAX = 8;
   int KERN_CPTIME = 40;
   int KERN_CPTIME2 = 71;
   int VM_UVMEXP = 4;
   int HW_MACHINE = 1;
   int HW_MODEL = 2;
   int HW_PAGESIZE = 7;
   int HW_CPUSPEED = 12;
   int HW_NCPUFOUND = 21;
   int HW_SMT = 24;
   int HW_NCPUONLINE = 25;
   int VFS_GENERIC = 0;
   int VFS_BCACHESTAT = 3;
   int CPUSTATES = 5;
   int CP_USER = 0;
   int CP_NICE = 1;
   int CP_SYS = 2;
   int CP_INTR = 3;
   int CP_IDLE = 4;
   int UINT64_SIZE = Native.getNativeSize(long.class);
   int INT_SIZE = Native.getNativeSize(int.class);

   @Structure.FieldOrder(
      {
            "numbufs",
            "numbufpages",
            "numdirtypages",
            "numcleanpages",
            "pendingwrites",
            "pendingreads",
            "numwrites",
            "numreads",
            "cachehits",
            "busymapped",
            "dmapages",
            "highpages",
            "delwribufs",
            "kvaslots",
            "kvaslots_avail",
            "highflips",
            "highflops",
            "dmaflips"
      }
   )
   public static class Bcachestats extends Structure {
      public long numbufs;
      public long numbufpages;
      public long numdirtypages;
      public long numcleanpages;
      public long pendingwrites;
      public long pendingreads;
      public long numwrites;
      public long numreads;
      public long cachehits;
      public long busymapped;
      public long dmapages;
      public long highpages;
      public long delwribufs;
      public long kvaslots;
      public long kvaslots_avail;
      public long highflips;
      public long highflops;
      public long dmaflips;

      public Bcachestats(Pointer p) {
         super(p);
         this.read();
      }
   }

   @Structure.FieldOrder({"tv_sec", "tv_usec"})
   public static class Timeval extends Structure {
      public long tv_sec;
      public long tv_usec;
   }
}
