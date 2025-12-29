package com.sun.jna.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Native;

public abstract class WinCryptUtil {
   public static class MANAGED_CRYPT_SIGN_MESSAGE_PARA extends WinCrypt.CRYPT_SIGN_MESSAGE_PARA {
      private WinCrypt.CERT_CONTEXT[] rgpMsgCerts;
      private WinCrypt.CRL_CONTEXT[] rgpMsgCrls;
      private WinCrypt.CRYPT_ATTRIBUTE[] rgAuthAttrs;
      private WinCrypt.CRYPT_ATTRIBUTE[] rgUnauthAttrs;

      public void setRgpMsgCert(WinCrypt.CERT_CONTEXT[] rgpMsgCerts) {
         this.rgpMsgCerts = rgpMsgCerts;
         if (rgpMsgCerts != null && rgpMsgCerts.length != 0) {
            this.cMsgCert = rgpMsgCerts.length;
            Memory mem = new Memory(Native.POINTER_SIZE * rgpMsgCerts.length);

            for (int i = 0; i < rgpMsgCerts.length; i++) {
               mem.setPointer(i * Native.POINTER_SIZE, rgpMsgCerts[i].getPointer());
            }

            this.rgpMsgCert = mem;
         } else {
            this.rgpMsgCert = null;
            this.cMsgCert = 0;
         }
      }

      @Override
      public WinCrypt.CERT_CONTEXT[] getRgpMsgCert() {
         return this.rgpMsgCerts;
      }

      public void setRgpMsgCrl(WinCrypt.CRL_CONTEXT[] rgpMsgCrls) {
         this.rgpMsgCrls = rgpMsgCrls;
         if (rgpMsgCrls != null && rgpMsgCrls.length != 0) {
            this.cMsgCert = rgpMsgCrls.length;
            Memory mem = new Memory(Native.POINTER_SIZE * rgpMsgCrls.length);

            for (int i = 0; i < rgpMsgCrls.length; i++) {
               mem.setPointer(i * Native.POINTER_SIZE, rgpMsgCrls[i].getPointer());
            }

            this.rgpMsgCert = mem;
         } else {
            this.rgpMsgCert = null;
            this.cMsgCert = 0;
         }
      }

      @Override
      public WinCrypt.CRL_CONTEXT[] getRgpMsgCrl() {
         return this.rgpMsgCrls;
      }

      public void setRgAuthAttr(WinCrypt.CRYPT_ATTRIBUTE[] rgAuthAttrs) {
         this.rgAuthAttrs = rgAuthAttrs;
         if (rgAuthAttrs != null && rgAuthAttrs.length != 0) {
            this.cMsgCert = this.rgpMsgCerts.length;
            this.rgAuthAttr = rgAuthAttrs[0].getPointer();
         } else {
            this.rgAuthAttr = null;
            this.cMsgCert = 0;
         }
      }

      @Override
      public WinCrypt.CRYPT_ATTRIBUTE[] getRgAuthAttr() {
         return this.rgAuthAttrs;
      }

      public void setRgUnauthAttr(WinCrypt.CRYPT_ATTRIBUTE[] rgUnauthAttrs) {
         this.rgUnauthAttrs = rgUnauthAttrs;
         if (rgUnauthAttrs != null && rgUnauthAttrs.length != 0) {
            this.cMsgCert = this.rgpMsgCerts.length;
            this.rgUnauthAttr = rgUnauthAttrs[0].getPointer();
         } else {
            this.rgUnauthAttr = null;
            this.cMsgCert = 0;
         }
      }

      @Override
      public WinCrypt.CRYPT_ATTRIBUTE[] getRgUnauthAttr() {
         return this.rgUnauthAttrs;
      }

      @Override
      public void write() {
         if (this.rgpMsgCerts != null) {
            for (WinCrypt.CERT_CONTEXT cc : this.rgpMsgCerts) {
               cc.write();
            }
         }

         if (this.rgpMsgCrls != null) {
            for (WinCrypt.CRL_CONTEXT cc : this.rgpMsgCrls) {
               cc.write();
            }
         }

         if (this.rgAuthAttrs != null) {
            for (WinCrypt.CRYPT_ATTRIBUTE cc : this.rgAuthAttrs) {
               cc.write();
            }
         }

         if (this.rgUnauthAttrs != null) {
            for (WinCrypt.CRYPT_ATTRIBUTE cc : this.rgUnauthAttrs) {
               cc.write();
            }
         }

         this.cbSize = this.size();
         super.write();
      }

      @Override
      public void read() {
         if (this.rgpMsgCerts != null) {
            for (WinCrypt.CERT_CONTEXT cc : this.rgpMsgCerts) {
               cc.read();
            }
         }

         if (this.rgpMsgCrls != null) {
            for (WinCrypt.CRL_CONTEXT cc : this.rgpMsgCrls) {
               cc.read();
            }
         }

         if (this.rgAuthAttrs != null) {
            for (WinCrypt.CRYPT_ATTRIBUTE cc : this.rgAuthAttrs) {
               cc.read();
            }
         }

         if (this.rgUnauthAttrs != null) {
            for (WinCrypt.CRYPT_ATTRIBUTE cc : this.rgUnauthAttrs) {
               cc.read();
            }
         }

         super.read();
      }
   }
}
