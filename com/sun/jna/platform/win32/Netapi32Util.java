package com.sun.jna.platform.win32;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.ArrayList;

public abstract class Netapi32Util {
   public static String getDCName() {
      return getDCName(null, null);
   }

   public static String getDCName(String serverName, String domainName) {
      PointerByReference bufptr = new PointerByReference();

      String var4;
      try {
         int rc = Netapi32.INSTANCE.NetGetDCName(domainName, serverName, bufptr);
         if (0 != rc) {
            throw new Win32Exception(rc);
         }

         var4 = bufptr.getValue().getWideString(0L);
      } finally {
         if (0 != Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue())) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
         }
      }

      return var4;
   }

   public static int getJoinStatus() {
      return getJoinStatus(null);
   }

   public static int getJoinStatus(String computerName) {
      PointerByReference lpNameBuffer = new PointerByReference();
      IntByReference bufferType = new IntByReference();

      int var4;
      try {
         int rc = Netapi32.INSTANCE.NetGetJoinInformation(computerName, lpNameBuffer, bufferType);
         if (0 != rc) {
            throw new Win32Exception(rc);
         }

         var4 = bufferType.getValue();
      } finally {
         if (lpNameBuffer.getPointer() != null) {
            int rc = Netapi32.INSTANCE.NetApiBufferFree(lpNameBuffer.getValue());
            if (0 != rc) {
               throw new Win32Exception(rc);
            }
         }
      }

      return var4;
   }

   public static String getDomainName(String computerName) {
      PointerByReference lpNameBuffer = new PointerByReference();
      IntByReference bufferType = new IntByReference();

      String var4;
      try {
         int rc = Netapi32.INSTANCE.NetGetJoinInformation(computerName, lpNameBuffer, bufferType);
         if (0 != rc) {
            throw new Win32Exception(rc);
         }

         var4 = lpNameBuffer.getValue().getWideString(0L);
      } finally {
         if (lpNameBuffer.getPointer() != null) {
            int rc = Netapi32.INSTANCE.NetApiBufferFree(lpNameBuffer.getValue());
            if (0 != rc) {
               throw new Win32Exception(rc);
            }
         }
      }

      return var4;
   }

   public static Netapi32Util.LocalGroup[] getLocalGroups() {
      return getLocalGroups(null);
   }

   public static Netapi32Util.LocalGroup[] getLocalGroups(String serverName) {
      PointerByReference bufptr = new PointerByReference();
      IntByReference entriesRead = new IntByReference();
      IntByReference totalEntries = new IntByReference();

      Netapi32Util.LocalGroup[] var17;
      try {
         int rc = Netapi32.INSTANCE.NetLocalGroupEnum(serverName, 1, bufptr, -1, entriesRead, totalEntries, null);
         if (0 != rc || bufptr.getValue() == Pointer.NULL) {
            throw new Win32Exception(rc);
         }

         ArrayList<Netapi32Util.LocalGroup> result = new ArrayList<>();
         if (entriesRead.getValue() > 0) {
            LMAccess.LOCALGROUP_INFO_1 group = new LMAccess.LOCALGROUP_INFO_1(bufptr.getValue());
            LMAccess.LOCALGROUP_INFO_1[] groups = (LMAccess.LOCALGROUP_INFO_1[])group.toArray(entriesRead.getValue());

            for (LMAccess.LOCALGROUP_INFO_1 lgpi : groups) {
               Netapi32Util.LocalGroup lgp = new Netapi32Util.LocalGroup();
               lgp.name = lgpi.lgrui1_name;
               lgp.comment = lgpi.lgrui1_comment;
               result.add(lgp);
            }
         }

         var17 = result.toArray(new Netapi32Util.LocalGroup[0]);
      } finally {
         if (bufptr.getValue() != Pointer.NULL) {
            int rcx = Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue());
            if (0 != rcx) {
               throw new Win32Exception(rcx);
            }
         }
      }

      return var17;
   }

   public static Netapi32Util.Group[] getGlobalGroups() {
      return getGlobalGroups(null);
   }

   public static Netapi32Util.Group[] getGlobalGroups(String serverName) {
      PointerByReference bufptr = new PointerByReference();
      IntByReference entriesRead = new IntByReference();
      IntByReference totalEntries = new IntByReference();

      Netapi32Util.Group[] var17;
      try {
         int rc = Netapi32.INSTANCE.NetGroupEnum(serverName, 1, bufptr, -1, entriesRead, totalEntries, null);
         if (0 != rc || bufptr.getValue() == Pointer.NULL) {
            throw new Win32Exception(rc);
         }

         ArrayList<Netapi32Util.LocalGroup> result = new ArrayList<>();
         if (entriesRead.getValue() > 0) {
            LMAccess.GROUP_INFO_1 group = new LMAccess.GROUP_INFO_1(bufptr.getValue());
            LMAccess.GROUP_INFO_1[] groups = (LMAccess.GROUP_INFO_1[])group.toArray(entriesRead.getValue());

            for (LMAccess.GROUP_INFO_1 lgpi : groups) {
               Netapi32Util.LocalGroup lgp = new Netapi32Util.LocalGroup();
               lgp.name = lgpi.grpi1_name;
               lgp.comment = lgpi.grpi1_comment;
               result.add(lgp);
            }
         }

         var17 = result.toArray(new Netapi32Util.LocalGroup[0]);
      } finally {
         if (bufptr.getValue() != Pointer.NULL) {
            int rcx = Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue());
            if (0 != rcx) {
               throw new Win32Exception(rcx);
            }
         }
      }

      return var17;
   }

   public static Netapi32Util.User[] getUsers() {
      return getUsers(null);
   }

   public static Netapi32Util.User[] getUsers(String serverName) {
      PointerByReference bufptr = new PointerByReference();
      IntByReference entriesRead = new IntByReference();
      IntByReference totalEntries = new IntByReference();

      Netapi32Util.User[] var17;
      try {
         int rc = Netapi32.INSTANCE.NetUserEnum(serverName, 1, 0, bufptr, -1, entriesRead, totalEntries, null);
         if (0 != rc || bufptr.getValue() == Pointer.NULL) {
            throw new Win32Exception(rc);
         }

         ArrayList<Netapi32Util.User> result = new ArrayList<>();
         if (entriesRead.getValue() > 0) {
            LMAccess.USER_INFO_1 user = new LMAccess.USER_INFO_1(bufptr.getValue());
            LMAccess.USER_INFO_1[] users = (LMAccess.USER_INFO_1[])user.toArray(entriesRead.getValue());

            for (LMAccess.USER_INFO_1 lu : users) {
               Netapi32Util.User auser = new Netapi32Util.User();
               if (lu.usri1_name != null) {
                  auser.name = lu.usri1_name;
               }

               result.add(auser);
            }
         }

         var17 = result.toArray(new Netapi32Util.User[0]);
      } finally {
         if (bufptr.getValue() != Pointer.NULL) {
            int rcx = Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue());
            if (0 != rcx) {
               throw new Win32Exception(rcx);
            }
         }
      }

      return var17;
   }

   public static Netapi32Util.Group[] getCurrentUserLocalGroups() {
      return getUserLocalGroups(Secur32Util.getUserNameEx(2));
   }

   public static Netapi32Util.Group[] getUserLocalGroups(String userName) {
      return getUserLocalGroups(userName, null);
   }

   public static Netapi32Util.Group[] getUserLocalGroups(String userName, String serverName) {
      PointerByReference bufptr = new PointerByReference();
      IntByReference entriesread = new IntByReference();
      IntByReference totalentries = new IntByReference();

      Netapi32Util.Group[] var18;
      try {
         int rc = Netapi32.INSTANCE.NetUserGetLocalGroups(serverName, userName, 0, 0, bufptr, -1, entriesread, totalentries);
         if (rc != 0) {
            throw new Win32Exception(rc);
         }

         ArrayList<Netapi32Util.Group> result = new ArrayList<>();
         if (entriesread.getValue() > 0) {
            LMAccess.LOCALGROUP_USERS_INFO_0 lgroup = new LMAccess.LOCALGROUP_USERS_INFO_0(bufptr.getValue());
            LMAccess.LOCALGROUP_USERS_INFO_0[] lgroups = (LMAccess.LOCALGROUP_USERS_INFO_0[])lgroup.toArray(entriesread.getValue());

            for (LMAccess.LOCALGROUP_USERS_INFO_0 lgpi : lgroups) {
               Netapi32Util.LocalGroup lgp = new Netapi32Util.LocalGroup();
               if (lgpi.lgrui0_name != null) {
                  lgp.name = lgpi.lgrui0_name;
               }

               result.add(lgp);
            }
         }

         var18 = result.toArray(new Netapi32Util.Group[0]);
      } finally {
         if (bufptr.getValue() != Pointer.NULL) {
            int rcx = Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue());
            if (0 != rcx) {
               throw new Win32Exception(rcx);
            }
         }
      }

      return var18;
   }

   public static Netapi32Util.Group[] getUserGroups(String userName) {
      return getUserGroups(userName, null);
   }

   public static Netapi32Util.Group[] getUserGroups(String userName, String serverName) {
      PointerByReference bufptr = new PointerByReference();
      IntByReference entriesread = new IntByReference();
      IntByReference totalentries = new IntByReference();

      Netapi32Util.Group[] var18;
      try {
         int rc = Netapi32.INSTANCE.NetUserGetGroups(serverName, userName, 0, bufptr, -1, entriesread, totalentries);
         if (rc != 0) {
            throw new Win32Exception(rc);
         }

         ArrayList<Netapi32Util.Group> result = new ArrayList<>();
         if (entriesread.getValue() > 0) {
            LMAccess.GROUP_USERS_INFO_0 lgroup = new LMAccess.GROUP_USERS_INFO_0(bufptr.getValue());
            LMAccess.GROUP_USERS_INFO_0[] lgroups = (LMAccess.GROUP_USERS_INFO_0[])lgroup.toArray(entriesread.getValue());

            for (LMAccess.GROUP_USERS_INFO_0 lgpi : lgroups) {
               Netapi32Util.Group lgp = new Netapi32Util.Group();
               if (lgpi.grui0_name != null) {
                  lgp.name = lgpi.grui0_name;
               }

               result.add(lgp);
            }
         }

         var18 = result.toArray(new Netapi32Util.Group[0]);
      } finally {
         if (bufptr.getValue() != Pointer.NULL) {
            int rcx = Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue());
            if (0 != rcx) {
               throw new Win32Exception(rcx);
            }
         }
      }

      return var18;
   }

   public static Netapi32Util.DomainController getDC() {
      DsGetDC.PDOMAIN_CONTROLLER_INFO pdci = new DsGetDC.PDOMAIN_CONTROLLER_INFO();
      int rc = Netapi32.INSTANCE.DsGetDcName(null, null, null, null, 0, pdci);
      if (0 != rc) {
         throw new Win32Exception(rc);
      } else {
         Netapi32Util.DomainController dc = new Netapi32Util.DomainController();
         dc.address = pdci.dci.DomainControllerAddress;
         dc.addressType = pdci.dci.DomainControllerAddressType;
         dc.clientSiteName = pdci.dci.ClientSiteName;
         dc.dnsForestName = pdci.dci.DnsForestName;
         dc.domainGuid = pdci.dci.DomainGuid;
         dc.domainName = pdci.dci.DomainName;
         dc.flags = pdci.dci.Flags;
         dc.name = pdci.dci.DomainControllerName;
         rc = Netapi32.INSTANCE.NetApiBufferFree(pdci.dci.getPointer());
         if (0 != rc) {
            throw new Win32Exception(rc);
         } else {
            return dc;
         }
      }
   }

   public static Netapi32Util.DomainTrust[] getDomainTrusts() {
      return getDomainTrusts(null);
   }

   public static Netapi32Util.DomainTrust[] getDomainTrusts(String serverName) {
      IntByReference domainTrustCount = new IntByReference();
      PointerByReference domainsPointerRef = new PointerByReference();
      int rc = Netapi32.INSTANCE.DsEnumerateDomainTrusts(serverName, 63, domainsPointerRef, domainTrustCount);
      if (0 != rc) {
         throw new Win32Exception(rc);
      } else {
         Netapi32Util.DomainTrust[] var16;
         try {
            ArrayList<Netapi32Util.DomainTrust> trusts = new ArrayList<>(domainTrustCount.getValue());
            if (domainTrustCount.getValue() > 0) {
               DsGetDC.DS_DOMAIN_TRUSTS domainTrustRefs = new DsGetDC.DS_DOMAIN_TRUSTS(domainsPointerRef.getValue());
               DsGetDC.DS_DOMAIN_TRUSTS[] domainTrusts = (DsGetDC.DS_DOMAIN_TRUSTS[])domainTrustRefs.toArray(
                  new DsGetDC.DS_DOMAIN_TRUSTS[domainTrustCount.getValue()]
               );

               for (DsGetDC.DS_DOMAIN_TRUSTS domainTrust : domainTrusts) {
                  Netapi32Util.DomainTrust t = new Netapi32Util.DomainTrust();
                  if (domainTrust.DnsDomainName != null) {
                     t.DnsDomainName = domainTrust.DnsDomainName;
                  }

                  if (domainTrust.NetbiosDomainName != null) {
                     t.NetbiosDomainName = domainTrust.NetbiosDomainName;
                  }

                  t.DomainSid = domainTrust.DomainSid;
                  if (domainTrust.DomainSid != null) {
                     t.DomainSidString = Advapi32Util.convertSidToStringSid(domainTrust.DomainSid);
                  }

                  t.DomainGuid = domainTrust.DomainGuid;
                  if (domainTrust.DomainGuid != null) {
                     t.DomainGuidString = Ole32Util.getStringFromGUID(domainTrust.DomainGuid);
                  }

                  t.flags = domainTrust.Flags;
                  trusts.add(t);
               }
            }

            var16 = trusts.toArray(new Netapi32Util.DomainTrust[0]);
         } finally {
            rc = Netapi32.INSTANCE.NetApiBufferFree(domainsPointerRef.getValue());
            if (0 != rc) {
               throw new Win32Exception(rc);
            }
         }

         return var16;
      }
   }

   public static Netapi32Util.UserInfo getUserInfo(String accountName) {
      return getUserInfo(accountName, getDCName());
   }

   public static Netapi32Util.UserInfo getUserInfo(String accountName, String domainName) {
      PointerByReference bufptr = new PointerByReference();

      Netapi32Util.UserInfo var6;
      try {
         int rc = Netapi32.INSTANCE.NetUserGetInfo(domainName, accountName, 23, bufptr);
         if (rc != 0) {
            throw new Win32Exception(rc);
         }

         LMAccess.USER_INFO_23 info_23 = new LMAccess.USER_INFO_23(bufptr.getValue());
         Netapi32Util.UserInfo userInfo = new Netapi32Util.UserInfo();
         userInfo.comment = info_23.usri23_comment;
         userInfo.flags = info_23.usri23_flags;
         userInfo.fullName = info_23.usri23_full_name;
         userInfo.name = info_23.usri23_name;
         if (info_23.usri23_user_sid != null) {
            userInfo.sidString = Advapi32Util.convertSidToStringSid(info_23.usri23_user_sid);
         }

         userInfo.sid = info_23.usri23_user_sid;
         var6 = userInfo;
      } finally {
         if (bufptr.getValue() != Pointer.NULL) {
            Netapi32.INSTANCE.NetApiBufferFree(bufptr.getValue());
         }
      }

      return var6;
   }

   public static class DomainController {
      public String name;
      public String address;
      public int addressType;
      public Guid.GUID domainGuid;
      public String domainName;
      public String dnsForestName;
      public int flags;
      public String clientSiteName;
   }

   public static class DomainTrust {
      public String NetbiosDomainName;
      public String DnsDomainName;
      public WinNT.PSID DomainSid;
      public String DomainSidString;
      public Guid.GUID DomainGuid;
      public String DomainGuidString;
      private int flags;

      public boolean isInForest() {
         return (this.flags & 1) != 0;
      }

      public boolean isOutbound() {
         return (this.flags & 2) != 0;
      }

      public boolean isRoot() {
         return (this.flags & 4) != 0;
      }

      public boolean isPrimary() {
         return (this.flags & 8) != 0;
      }

      public boolean isNativeMode() {
         return (this.flags & 16) != 0;
      }

      public boolean isInbound() {
         return (this.flags & 32) != 0;
      }
   }

   public static class Group {
      public String name;
   }

   public static class LocalGroup extends Netapi32Util.Group {
      public String comment;
   }

   public static class User {
      public String name;
      public String comment;
   }

   public static class UserInfo extends Netapi32Util.User {
      public String fullName;
      public String sidString;
      public WinNT.PSID sid;
      public int flags;
   }
}
