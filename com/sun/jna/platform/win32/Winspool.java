package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface Winspool extends StdCallLibrary {
   Winspool INSTANCE = Native.load("Winspool.drv", Winspool.class, W32APIOptions.DEFAULT_OPTIONS);
   int CCHDEVICENAME = 32;
   int PRINTER_STATUS_PAUSED = 1;
   int PRINTER_STATUS_ERROR = 2;
   int PRINTER_STATUS_PENDING_DELETION = 4;
   int PRINTER_STATUS_PAPER_JAM = 8;
   int PRINTER_STATUS_PAPER_OUT = 16;
   int PRINTER_STATUS_MANUAL_FEED = 32;
   int PRINTER_STATUS_PAPER_PROBLEM = 64;
   int PRINTER_STATUS_OFFLINE = 128;
   int PRINTER_STATUS_IO_ACTIVE = 256;
   int PRINTER_STATUS_BUSY = 512;
   int PRINTER_STATUS_PRINTING = 1024;
   int PRINTER_STATUS_OUTPUT_BIN_FULL = 2048;
   int PRINTER_STATUS_NOT_AVAILABLE = 4096;
   int PRINTER_STATUS_WAITING = 8192;
   int PRINTER_STATUS_PROCESSING = 16384;
   int PRINTER_STATUS_INITIALIZING = 32768;
   int PRINTER_STATUS_WARMING_UP = 65536;
   int PRINTER_STATUS_TONER_LOW = 131072;
   int PRINTER_STATUS_NO_TONER = 262144;
   int PRINTER_STATUS_PAGE_PUNT = 524288;
   int PRINTER_STATUS_USER_INTERVENTION = 1048576;
   int PRINTER_STATUS_OUT_OF_MEMORY = 2097152;
   int PRINTER_STATUS_DOOR_OPEN = 4194304;
   int PRINTER_STATUS_SERVER_UNKNOWN = 8388608;
   int PRINTER_STATUS_POWER_SAVE = 16777216;
   int PRINTER_ATTRIBUTE_QUEUED = 1;
   int PRINTER_ATTRIBUTE_DIRECT = 2;
   int PRINTER_ATTRIBUTE_DEFAULT = 4;
   int PRINTER_ATTRIBUTE_SHARED = 8;
   int PRINTER_ATTRIBUTE_NETWORK = 16;
   int PRINTER_ATTRIBUTE_HIDDEN = 32;
   int PRINTER_ATTRIBUTE_LOCAL = 64;
   int PRINTER_ATTRIBUTE_ENABLE_DEVQ = 128;
   int PRINTER_ATTRIBUTE_KEEPPRINTEDJOBS = 256;
   int PRINTER_ATTRIBUTE_DO_COMPLETE_FIRST = 512;
   int PRINTER_ATTRIBUTE_WORK_OFFLINE = 1024;
   int PRINTER_ATTRIBUTE_ENABLE_BIDI = 2048;
   int PRINTER_ATTRIBUTE_RAW_ONLY = 4096;
   int PRINTER_ATTRIBUTE_PUBLISHED = 8192;
   int PRINTER_ATTRIBUTE_FAX = 16384;
   int PRINTER_ATTRIBUTE_TS = 32768;
   int PRINTER_ATTRIBUTE_PUSHED_USER = 131072;
   int PRINTER_ATTRIBUTE_PUSHED_MACHINE = 262144;
   int PRINTER_ATTRIBUTE_MACHINE = 524288;
   int PRINTER_ATTRIBUTE_FRIENDLY_NAME = 1048576;
   int PRINTER_ATTRIBUTE_TS_GENERIC_DRIVER = 2097152;
   int PRINTER_CHANGE_ADD_PRINTER = 1;
   int PRINTER_CHANGE_SET_PRINTER = 2;
   int PRINTER_CHANGE_DELETE_PRINTER = 4;
   int PRINTER_CHANGE_FAILED_CONNECTION_PRINTER = 8;
   int PRINTER_CHANGE_PRINTER = 255;
   int PRINTER_CHANGE_ADD_JOB = 256;
   int PRINTER_CHANGE_SET_JOB = 512;
   int PRINTER_CHANGE_DELETE_JOB = 1024;
   int PRINTER_CHANGE_WRITE_JOB = 2048;
   int PRINTER_CHANGE_JOB = 65280;
   int PRINTER_CHANGE_ADD_FORM = 65536;
   int PRINTER_CHANGE_SET_FORM = 131072;
   int PRINTER_CHANGE_DELETE_FORM = 262144;
   int PRINTER_CHANGE_FORM = 458752;
   int PRINTER_CHANGE_ADD_PORT = 1048576;
   int PRINTER_CHANGE_CONFIGURE_PORT = 2097152;
   int PRINTER_CHANGE_DELETE_PORT = 4194304;
   int PRINTER_CHANGE_PORT = 7340032;
   int PRINTER_CHANGE_ADD_PRINT_PROCESSOR = 16777216;
   int PRINTER_CHANGE_DELETE_PRINT_PROCESSOR = 67108864;
   int PRINTER_CHANGE_PRINT_PROCESSOR = 117440512;
   int PRINTER_CHANGE_SERVER = 134217728;
   int PRINTER_CHANGE_ADD_PRINTER_DRIVER = 268435456;
   int PRINTER_CHANGE_SET_PRINTER_DRIVER = 536870912;
   int PRINTER_CHANGE_DELETE_PRINTER_DRIVER = 1073741824;
   int PRINTER_CHANGE_PRINTER_DRIVER = 1879048192;
   int PRINTER_CHANGE_TIMEOUT = Integer.MIN_VALUE;
   int PRINTER_CHANGE_ALL_WIN7 = 2138570751;
   int PRINTER_CHANGE_ALL = 2004353023;
   int PRINTER_ENUM_DEFAULT = 1;
   int PRINTER_ENUM_LOCAL = 2;
   int PRINTER_ENUM_CONNECTIONS = 4;
   int PRINTER_ENUM_FAVORITE = 4;
   int PRINTER_ENUM_NAME = 8;
   int PRINTER_ENUM_REMOTE = 16;
   int PRINTER_ENUM_SHARED = 32;
   int PRINTER_ENUM_NETWORK = 64;
   int PRINTER_ENUM_EXPAND = 16384;
   int PRINTER_ENUM_CONTAINER = 32768;
   int PRINTER_ENUM_ICONMASK = 16711680;
   int PRINTER_ENUM_ICON1 = 65536;
   int PRINTER_ENUM_ICON2 = 131072;
   int PRINTER_ENUM_ICON3 = 262144;
   int PRINTER_ENUM_ICON4 = 524288;
   int PRINTER_ENUM_ICON5 = 1048576;
   int PRINTER_ENUM_ICON6 = 2097152;
   int PRINTER_ENUM_ICON7 = 4194304;
   int PRINTER_ENUM_ICON8 = 8388608;
   int PRINTER_ENUM_HIDE = 16777216;
   int PRINTER_NOTIFY_OPTIONS_REFRESH = 1;
   int PRINTER_NOTIFY_INFO_DISCARDED = 1;
   int PRINTER_NOTIFY_TYPE = 0;
   int JOB_NOTIFY_TYPE = 1;
   short PRINTER_NOTIFY_FIELD_SERVER_NAME = 0;
   short PRINTER_NOTIFY_FIELD_PRINTER_NAME = 1;
   short PRINTER_NOTIFY_FIELD_SHARE_NAME = 2;
   short PRINTER_NOTIFY_FIELD_PORT_NAME = 3;
   short PRINTER_NOTIFY_FIELD_DRIVER_NAME = 4;
   short PRINTER_NOTIFY_FIELD_COMMENT = 5;
   short PRINTER_NOTIFY_FIELD_LOCATION = 6;
   short PRINTER_NOTIFY_FIELD_DEVMODE = 7;
   short PRINTER_NOTIFY_FIELD_SEPFILE = 8;
   short PRINTER_NOTIFY_FIELD_PRINT_PROCESSOR = 9;
   short PRINTER_NOTIFY_FIELD_PARAMETERS = 10;
   short PRINTER_NOTIFY_FIELD_DATATYPE = 11;
   short PRINTER_NOTIFY_FIELD_SECURITY_DESCRIPTOR = 12;
   short PRINTER_NOTIFY_FIELD_ATTRIBUTES = 13;
   short PRINTER_NOTIFY_FIELD_PRIORITY = 14;
   short PRINTER_NOTIFY_FIELD_DEFAULT_PRIORITY = 15;
   short PRINTER_NOTIFY_FIELD_START_TIME = 16;
   short PRINTER_NOTIFY_FIELD_UNTIL_TIME = 17;
   short PRINTER_NOTIFY_FIELD_STATUS = 18;
   short PRINTER_NOTIFY_FIELD_STATUS_STRING = 19;
   short PRINTER_NOTIFY_FIELD_CJOBS = 20;
   short PRINTER_NOTIFY_FIELD_AVERAGE_PPM = 21;
   short PRINTER_NOTIFY_FIELD_TOTAL_PAGES = 22;
   short PRINTER_NOTIFY_FIELD_PAGES_PRINTED = 23;
   short PRINTER_NOTIFY_FIELD_TOTAL_BYTES = 24;
   short PRINTER_NOTIFY_FIELD_BYTES_PRINTED = 25;
   short PRINTER_NOTIFY_FIELD_OBJECT_GUID = 26;
   short PRINTER_NOTIFY_FIELD_FRIENDLY_NAME = 27;
   short PRINTER_NOTIFY_FIELD_BRANCH_OFFICE_PRINTING = 28;
   short JOB_NOTIFY_FIELD_PRINTER_NAME = 0;
   short JOB_NOTIFY_FIELD_MACHINE_NAME = 1;
   short JOB_NOTIFY_FIELD_PORT_NAME = 2;
   short JOB_NOTIFY_FIELD_USER_NAME = 3;
   short JOB_NOTIFY_FIELD_NOTIFY_NAME = 4;
   short JOB_NOTIFY_FIELD_DATATYPE = 5;
   short JOB_NOTIFY_FIELD_PRINT_PROCESSOR = 6;
   short JOB_NOTIFY_FIELD_PARAMETERS = 7;
   short JOB_NOTIFY_FIELD_DRIVER_NAME = 8;
   short JOB_NOTIFY_FIELD_DEVMODE = 9;
   short JOB_NOTIFY_FIELD_STATUS = 10;
   short JOB_NOTIFY_FIELD_STATUS_STRING = 11;
   short JOB_NOTIFY_FIELD_SECURITY_DESCRIPTOR = 12;
   short JOB_NOTIFY_FIELD_DOCUMENT = 13;
   short JOB_NOTIFY_FIELD_PRIORITY = 14;
   short JOB_NOTIFY_FIELD_POSITION = 15;
   short JOB_NOTIFY_FIELD_SUBMITTED = 16;
   short JOB_NOTIFY_FIELD_START_TIME = 17;
   short JOB_NOTIFY_FIELD_UNTIL_TIME = 18;
   short JOB_NOTIFY_FIELD_TIME = 19;
   short JOB_NOTIFY_FIELD_TOTAL_PAGES = 20;
   short JOB_NOTIFY_FIELD_PAGES_PRINTED = 21;
   short JOB_NOTIFY_FIELD_TOTAL_BYTES = 22;
   short JOB_NOTIFY_FIELD_BYTES_PRINTED = 23;
   short JOB_NOTIFY_FIELD_REMOTE_JOB_ID = 24;
   int PRINTER_NOTIFY_CATEGORY_ALL = 4096;
   int PRINTER_NOTIFY_CATEGORY_3D = 8192;

   boolean EnumPrinters(int var1, String var2, int var3, Pointer var4, int var5, IntByReference var6, IntByReference var7);

   boolean GetPrinter(WinNT.HANDLE var1, int var2, Pointer var3, int var4, IntByReference var5);

   boolean OpenPrinter(String var1, WinNT.HANDLEByReference var2, Winspool.LPPRINTER_DEFAULTS var3);

   boolean ClosePrinter(WinNT.HANDLE var1);

   @Deprecated
   WinNT.HANDLE FindFirstPrinterChangeNotification(WinNT.HANDLE var1, int var2, int var3, WinDef.LPVOID var4);

   WinNT.HANDLE FindFirstPrinterChangeNotification(WinNT.HANDLE var1, int var2, int var3, Winspool.PRINTER_NOTIFY_OPTIONS var4);

   @Deprecated
   boolean FindNextPrinterChangeNotification(WinNT.HANDLE var1, WinDef.DWORDByReference var2, WinDef.LPVOID var3, WinDef.LPVOID var4);

   boolean FindNextPrinterChangeNotification(WinNT.HANDLE var1, WinDef.DWORDByReference var2, Winspool.PRINTER_NOTIFY_OPTIONS var3, PointerByReference var4);

   boolean FindClosePrinterChangeNotification(WinNT.HANDLE var1);

   boolean FreePrinterNotifyInfo(Pointer var1);

   boolean EnumJobs(WinNT.HANDLE var1, int var2, int var3, int var4, Pointer var5, int var6, IntByReference var7, IntByReference var8);

   @Structure.FieldOrder(
      {
            "JobId",
            "pPrinterName",
            "pMachineName",
            "pUserName",
            "pDocument",
            "pDatatype",
            "pStatus",
            "Status",
            "Priority",
            "Position",
            "TotalPages",
            "PagesPrinted",
            "Submitted"
      }
   )
   public static class JOB_INFO_1 extends Structure {
      public int JobId;
      public String pPrinterName;
      public String pMachineName;
      public String pUserName;
      public String pDocument;
      public String pDatatype;
      public String pStatus;
      public int Status;
      public int Priority;
      public int Position;
      public int TotalPages;
      public int PagesPrinted;
      public WinBase.SYSTEMTIME Submitted;

      public JOB_INFO_1() {
      }

      public JOB_INFO_1(int size) {
         super(new Memory(size));
      }
   }

   @Structure.FieldOrder({"pDatatype", "pDevMode", "DesiredAccess"})
   public static class LPPRINTER_DEFAULTS extends Structure {
      public String pDatatype;
      public Pointer pDevMode;
      public int DesiredAccess;
   }

   public static class NOTIFY_DATA extends Union {
      public int[] adwData = new int[2];
      public Winspool.NOTIFY_DATA_DATA Data;
   }

   @Structure.FieldOrder({"cbBuf", "pBuf"})
   public static class NOTIFY_DATA_DATA extends Structure {
      public int cbBuf;
      public Pointer pBuf;
   }

   @Structure.FieldOrder({"Flags", "pDescription", "pName", "pComment"})
   public static class PRINTER_INFO_1 extends Structure {
      public int Flags;
      public String pDescription;
      public String pName;
      public String pComment;

      public PRINTER_INFO_1() {
      }

      public PRINTER_INFO_1(int size) {
         super(new Memory(size));
      }
   }

   @Structure.FieldOrder(
      {
            "pServerName",
            "pPrinterName",
            "pShareName",
            "pPortName",
            "pDriverName",
            "pComment",
            "pLocation",
            "pDevMode",
            "pSepFile",
            "pPrintProcessor",
            "pDatatype",
            "pParameters",
            "pSecurityDescriptor",
            "Attributes",
            "Priority",
            "DefaultPriority",
            "StartTime",
            "UntilTime",
            "Status",
            "cJobs",
            "AveragePPM"
      }
   )
   public static class PRINTER_INFO_2 extends Structure {
      public String pServerName;
      public String pPrinterName;
      public String pShareName;
      public String pPortName;
      public String pDriverName;
      public String pComment;
      public String pLocation;
      public WinDef.INT_PTR pDevMode;
      public String pSepFile;
      public String pPrintProcessor;
      public String pDatatype;
      public String pParameters;
      public WinDef.INT_PTR pSecurityDescriptor;
      public int Attributes;
      public int Priority;
      public int DefaultPriority;
      public int StartTime;
      public int UntilTime;
      public int Status;
      public int cJobs;
      public int AveragePPM;

      public PRINTER_INFO_2() {
      }

      public PRINTER_INFO_2(int size) {
         super(new Memory(size));
      }

      public boolean hasAttribute(int value) {
         return (this.Attributes & value) == value;
      }
   }

   @Structure.FieldOrder({"pPrinterName", "pServerName", "Attributes"})
   public static class PRINTER_INFO_4 extends Structure {
      public String pPrinterName;
      public String pServerName;
      public WinDef.DWORD Attributes;

      public PRINTER_INFO_4() {
      }

      public PRINTER_INFO_4(int size) {
         super(new Memory(size));
      }
   }

   @Structure.FieldOrder({"Version", "Flags", "Count", "aData"})
   public static class PRINTER_NOTIFY_INFO extends Structure {
      public int Version;
      public int Flags;
      public int Count;
      public Winspool.PRINTER_NOTIFY_INFO_DATA[] aData = new Winspool.PRINTER_NOTIFY_INFO_DATA[1];

      @Override
      public void read() {
         int count = (Integer)this.readField("Count");
         this.aData = new Winspool.PRINTER_NOTIFY_INFO_DATA[count];
         if (count == 0) {
            this.Count = count;
            this.Version = (Integer)this.readField("Version");
            this.Flags = (Integer)this.readField("Flags");
         } else {
            super.read();
         }
      }
   }

   @Structure.FieldOrder({"Type", "Field", "Reserved", "Id", "NotifyData"})
   public static class PRINTER_NOTIFY_INFO_DATA extends Structure {
      public short Type;
      public short Field;
      public int Reserved;
      public int Id;
      public Winspool.NOTIFY_DATA NotifyData;

      @Override
      public void read() {
         super.read();
         boolean numericData;
         if (this.Type == 0) {
            switch (this.Field) {
               case 13:
               case 14:
               case 15:
               case 16:
               case 17:
               case 18:
               case 20:
               case 21:
                  numericData = true;
               case 19:
               default:
                  numericData = false;
            }
         } else {
            switch (this.Field) {
               case 10:
               case 14:
               case 15:
               case 17:
               case 18:
               case 19:
               case 20:
               case 21:
               case 22:
               case 23:
                  numericData = true;
               case 11:
               case 12:
               case 13:
               case 16:
               default:
                  numericData = false;
            }
         }

         if (numericData) {
            this.NotifyData.setType(int[].class);
         } else {
            this.NotifyData.setType(Winspool.NOTIFY_DATA_DATA.class);
         }

         this.NotifyData.read();
      }
   }

   @Structure.FieldOrder({"Version", "Flags", "Count", "pTypes"})
   public static class PRINTER_NOTIFY_OPTIONS extends Structure {
      public int Version = 2;
      public int Flags;
      public int Count;
      public Winspool.PRINTER_NOTIFY_OPTIONS_TYPE.ByReference pTypes;
   }

   @Structure.FieldOrder({"Type", "Reserved0", "Reserved1", "Reserved2", "Count", "pFields"})
   public static class PRINTER_NOTIFY_OPTIONS_TYPE extends Structure {
      public short Type;
      public short Reserved0;
      public int Reserved1;
      public int Reserved2;
      public int Count;
      public Pointer pFields;

      public void setFields(short[] fields) {
         long shortSizeInBytes = 2L;
         Memory fieldsMemory = new Memory(fields.length * 2L);
         fieldsMemory.write(0L, fields, 0, fields.length);
         this.pFields = fieldsMemory;
         this.Count = fields.length;
      }

      public short[] getFields() {
         return this.pFields.getShortArray(0L, this.Count);
      }

      public static class ByReference extends Winspool.PRINTER_NOTIFY_OPTIONS_TYPE implements Structure.ByReference {
      }
   }
}
