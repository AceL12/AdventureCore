package com.sun.jna.platform.win32;

import com.sun.jna.ptr.IntByReference;

public abstract class WinspoolUtil {
   public static Winspool.PRINTER_INFO_1[] getPrinterInfo1() {
      IntByReference pcbNeeded = new IntByReference();
      IntByReference pcReturned = new IntByReference();
      Winspool.INSTANCE.EnumPrinters(2, null, 1, null, 0, pcbNeeded, pcReturned);
      if (pcbNeeded.getValue() <= 0) {
         return new Winspool.PRINTER_INFO_1[0];
      } else {
         Winspool.PRINTER_INFO_1 pPrinterEnum = new Winspool.PRINTER_INFO_1(pcbNeeded.getValue());
         if (!Winspool.INSTANCE.EnumPrinters(2, null, 1, pPrinterEnum.getPointer(), pcbNeeded.getValue(), pcbNeeded, pcReturned)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
         } else {
            pPrinterEnum.read();
            return (Winspool.PRINTER_INFO_1[])pPrinterEnum.toArray(pcReturned.getValue());
         }
      }
   }

   public static Winspool.PRINTER_INFO_2[] getPrinterInfo2() {
      return getPrinterInfo2(2);
   }

   public static Winspool.PRINTER_INFO_2[] getAllPrinterInfo2() {
      return getPrinterInfo2(6);
   }

   private static Winspool.PRINTER_INFO_2[] getPrinterInfo2(int flags) {
      IntByReference pcbNeeded = new IntByReference();
      IntByReference pcReturned = new IntByReference();
      Winspool.INSTANCE.EnumPrinters(flags, null, 2, null, 0, pcbNeeded, pcReturned);
      if (pcbNeeded.getValue() <= 0) {
         return new Winspool.PRINTER_INFO_2[0];
      } else {
         Winspool.PRINTER_INFO_2 pPrinterEnum = new Winspool.PRINTER_INFO_2(pcbNeeded.getValue());
         if (!Winspool.INSTANCE.EnumPrinters(flags, null, 2, pPrinterEnum.getPointer(), pcbNeeded.getValue(), pcbNeeded, pcReturned)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
         } else {
            pPrinterEnum.read();
            return (Winspool.PRINTER_INFO_2[])pPrinterEnum.toArray(pcReturned.getValue());
         }
      }
   }

   public static Winspool.PRINTER_INFO_2 getPrinterInfo2(String printerName) {
      IntByReference pcbNeeded = new IntByReference();
      IntByReference pcReturned = new IntByReference();
      WinNT.HANDLEByReference pHandle = new WinNT.HANDLEByReference();
      if (!Winspool.INSTANCE.OpenPrinter(printerName, pHandle, null)) {
         throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
      } else {
         Win32Exception we = null;
         Winspool.PRINTER_INFO_2 pinfo2 = null;

         try {
            Winspool.INSTANCE.GetPrinter(pHandle.getValue(), 2, null, 0, pcbNeeded);
            if (pcbNeeded.getValue() <= 0) {
               return new Winspool.PRINTER_INFO_2();
            }

            pinfo2 = new Winspool.PRINTER_INFO_2(pcbNeeded.getValue());
            if (!Winspool.INSTANCE.GetPrinter(pHandle.getValue(), 2, pinfo2.getPointer(), pcbNeeded.getValue(), pcReturned)) {
               throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }

            pinfo2.read();
         } catch (Win32Exception var12) {
            we = var12;
         } finally {
            if (!Winspool.INSTANCE.ClosePrinter(pHandle.getValue())) {
               Win32Exception ex = new Win32Exception(Kernel32.INSTANCE.GetLastError());
               if (we != null) {
                  ex.addSuppressedReflected(we);
               }
            }
         }

         if (we != null) {
            throw we;
         } else {
            return pinfo2;
         }
      }
   }

   public static Winspool.PRINTER_INFO_4[] getPrinterInfo4() {
      IntByReference pcbNeeded = new IntByReference();
      IntByReference pcReturned = new IntByReference();
      Winspool.INSTANCE.EnumPrinters(2, null, 4, null, 0, pcbNeeded, pcReturned);
      if (pcbNeeded.getValue() <= 0) {
         return new Winspool.PRINTER_INFO_4[0];
      } else {
         Winspool.PRINTER_INFO_4 pPrinterEnum = new Winspool.PRINTER_INFO_4(pcbNeeded.getValue());
         if (!Winspool.INSTANCE.EnumPrinters(2, null, 4, pPrinterEnum.getPointer(), pcbNeeded.getValue(), pcbNeeded, pcReturned)) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
         } else {
            pPrinterEnum.read();
            return (Winspool.PRINTER_INFO_4[])pPrinterEnum.toArray(pcReturned.getValue());
         }
      }
   }

   public static Winspool.JOB_INFO_1[] getJobInfo1(WinNT.HANDLEByReference phPrinter) {
      IntByReference pcbNeeded = new IntByReference();
      IntByReference pcReturned = new IntByReference();
      Winspool.INSTANCE.EnumJobs(phPrinter.getValue(), 0, 255, 1, null, 0, pcbNeeded, pcReturned);
      if (pcbNeeded.getValue() <= 0) {
         return new Winspool.JOB_INFO_1[0];
      } else {
         int lastError = 0;

         Winspool.JOB_INFO_1 pJobEnum;
         do {
            pJobEnum = new Winspool.JOB_INFO_1(pcbNeeded.getValue());
            if (!Winspool.INSTANCE.EnumJobs(phPrinter.getValue(), 0, 255, 1, pJobEnum.getPointer(), pcbNeeded.getValue(), pcbNeeded, pcReturned)) {
               lastError = Kernel32.INSTANCE.GetLastError();
            }
         } while (lastError == 122);

         if (lastError != 0) {
            throw new Win32Exception(lastError);
         } else if (pcReturned.getValue() <= 0) {
            return new Winspool.JOB_INFO_1[0];
         } else {
            pJobEnum.read();
            return (Winspool.JOB_INFO_1[])pJobEnum.toArray(pcReturned.getValue());
         }
      }
   }
}
