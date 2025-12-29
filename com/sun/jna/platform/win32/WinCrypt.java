package com.sun.jna.platform.win32;

import com.sun.jna.FromNativeContext;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.Pointer;
import com.sun.jna.StringArray;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APITypeMapper;

public interface WinCrypt {
   int CRYPTPROTECT_PROMPT_ON_UNPROTECT = 1;
   int CRYPTPROTECT_PROMPT_ON_PROTECT = 2;
   int CRYPTPROTECT_PROMPT_RESERVED = 4;
   int CRYPTPROTECT_PROMPT_STRONG = 8;
   int CRYPTPROTECT_PROMPT_REQUIRE_STRONG = 16;
   int CRYPTPROTECT_UI_FORBIDDEN = 1;
   int CRYPTPROTECT_LOCAL_MACHINE = 4;
   int CRYPTPROTECT_CRED_SYNC = 8;
   int CRYPTPROTECT_AUDIT = 16;
   int CRYPTPROTECT_NO_RECOVERY = 32;
   int CRYPTPROTECT_VERIFY_PROTECTION = 64;
   int CRYPTPROTECT_CRED_REGENERATE = 128;
   int CRYPT_E_ASN1_ERROR = -2146881280;
   int CRYPT_E_ASN1_INTERNAL = -2146881279;
   int CRYPT_E_ASN1_EOD = -2146881278;
   int CRYPT_E_ASN1_CORRUPT = -2146881277;
   int CRYPT_E_ASN1_LARGE = -2146881276;
   int CRYPT_E_ASN1_CONSTRAINT = -2146881275;
   int CRYPT_E_ASN1_MEMORY = -2146881274;
   int CRYPT_E_ASN1_OVERFLOW = -2146881273;
   int CRYPT_E_ASN1_BADPDU = -2146881272;
   int CRYPT_E_ASN1_BADARGS = -2146881271;
   int CRYPT_E_ASN1_BADREAL = -2146881270;
   int CRYPT_E_ASN1_BADTAG = -2146881269;
   int CRYPT_E_ASN1_CHOICE = -2146881268;
   int CRYPT_E_ASN1_RULE = -2146881267;
   int CRYPT_E_ASN1_UTF8 = -2146881266;
   int CRYPT_E_ASN1_PDU_TYPE = -2146881229;
   int CRYPT_E_ASN1_NYI = -2146881228;
   int CRYPT_E_ASN1_EXTENDED = -2146881023;
   int CRYPT_E_ASN1_NOEOD = -2146881022;
   int CRYPT_ASN_ENCODING = 1;
   int CRYPT_NDR_ENCODING = 2;
   int X509_ASN_ENCODING = 1;
   int X509_NDR_ENCODING = 2;
   int PKCS_7_ASN_ENCODING = 65536;
   int PKCS_7_NDR_ENCODING = 131072;
   int USAGE_MATCH_TYPE_AND = 0;
   int USAGE_MATCH_TYPE_OR = 1;
   int PP_CLIENT_HWND = 1;
   int CERT_SIMPLE_NAME_STR = 1;
   int CERT_OID_NAME_STR = 2;
   int CERT_X500_NAME_STR = 3;
   int CERT_XML_NAME_STR = 4;
   int CERT_CHAIN_POLICY_BASE = 1;
   String szOID_RSA_SHA1RSA = "1.2.840.113549.1.1.5";
   WinCrypt.HCERTCHAINENGINE HCCE_CURRENT_USER = new WinCrypt.HCERTCHAINENGINE(Pointer.createConstant(0));
   WinCrypt.HCERTCHAINENGINE HCCE_LOCAL_MACHINE = new WinCrypt.HCERTCHAINENGINE(Pointer.createConstant(1));
   WinCrypt.HCERTCHAINENGINE HCCE_SERIAL_LOCAL_MACHINE = new WinCrypt.HCERTCHAINENGINE(Pointer.createConstant(2));
   int CERT_COMPARE_SHIFT = 16;
   int CERT_COMPARE_NAME_STR_W = 8;
   int CERT_INFO_SUBJECT_FLAG = 7;
   int CERT_FIND_SUBJECT_STR_W = 524295;
   int CERT_FIND_SUBJECT_STR = 524295;
   int CRYPT_EXPORTABLE = 1;
   int CRYPT_USER_PROTECTED = 2;
   int CRYPT_MACHINE_KEYSET = 32;
   int CRYPT_USER_KEYSET = 4096;
   int PKCS12_PREFER_CNG_KSP = 256;
   int PKCS12_ALWAYS_CNG_KSP = 512;
   int PKCS12_ALLOW_OVERWRITE_KEY = 16384;
   int PKCS12_NO_PERSIST_KEY = 32768;
   int PKCS12_INCLUDE_EXTENDED_PROPERTIES = 16;
   int CERT_STORE_PROV_MSG = 1;
   int CERT_STORE_PROV_MEMORY = 2;
   int CERT_STORE_PROV_FILE = 3;
   int CERT_STORE_PROV_REG = 4;
   int CERT_STORE_PROV_PKCS7 = 5;
   int CERT_STORE_PROV_SERIALIZED = 6;
   int CERT_STORE_PROV_FILENAME_A = 7;
   int CERT_STORE_PROV_FILENAME_W = 8;
   int CERT_STORE_PROV_FILENAME = 8;
   int CERT_STORE_PROV_SYSTEM_A = 9;
   int CERT_STORE_PROV_SYSTEM_W = 10;
   int CERT_STORE_PROV_SYSTEM = 10;
   int CERT_STORE_PROV_COLLECTION = 11;
   int CERT_STORE_PROV_SYSTEM_REGISTRY_A = 12;
   int CERT_STORE_PROV_SYSTEM_REGISTRY_W = 13;
   int CERT_STORE_PROV_SYSTEM_REGISTRY = 13;
   int CERT_STORE_PROV_PHYSICAL_W = 14;
   int CERT_STORE_PROV_PHYSICAL = 14;
   int CERT_STORE_PROV_SMART_CARD_W = 15;
   int CERT_STORE_PROV_SMART_CARD = 15;
   int CERT_STORE_PROV_LDAP_W = 16;
   int CERT_STORE_PROV_LDAP = 16;
   int CERT_STORE_NO_CRYPT_RELEASE_FLAG = 1;
   int CERT_STORE_SET_LOCALIZED_NAME_FLAG = 2;
   int CERT_STORE_DEFER_CLOSE_UNTIL_LAST_FREE_FLAG = 4;
   int CERT_STORE_DELETE_FLAG = 16;
   int CERT_STORE_UNSAFE_PHYSICAL_FLAG = 32;
   int CERT_STORE_SHARE_STORE_FLAG = 64;
   int CERT_STORE_SHARE_CONTEXT_FLAG = 128;
   int CERT_STORE_MANIFOLD_FLAG = 256;
   int CERT_STORE_ENUM_ARCHIVED_FLAG = 512;
   int CERT_STORE_UPDATE_KEYID_FLAG = 1024;
   int CERT_STORE_BACKUP_RESTORE_FLAG = 2048;
   int CERT_STORE_MAXIMUM_ALLOWED_FLAG = 4096;
   int CERT_STORE_CREATE_NEW_FLAG = 8192;
   int CERT_STORE_OPEN_EXISTING_FLAG = 16384;
   int CERT_STORE_READONLY_FLAG = 32768;
   int CERT_SYSTEM_STORE_CURRENT_USER = 65536;
   int CERT_SYSTEM_STORE_LOCAL_MACHINE = 131072;
   int CERT_SYSTEM_STORE_CURRENT_SERVICE = 262144;
   int CERT_SYSTEM_STORE_SERVICES = 327680;
   int CERT_SYSTEM_STORE_USERS = 393216;
   int CERT_SYSTEM_STORE_CURRENT_USER_GROUP_POLICY = 458752;
   int CERT_SYSTEM_STORE_LOCAL_MACHINE_GROUP_POLICY = 524288;
   int CERT_SYSTEM_STORE_LOCAL_MACHINE_ENTERPRISE = 589824;
   int CERT_SYSTEM_STORE_UNPROTECTED_FLAG = 1073741824;
   int CERT_SYSTEM_STORE_RELOCATE_FLAG = Integer.MIN_VALUE;
   int CERT_CLOSE_STORE_FORCE_FLAG = 1;
   int CERT_CLOSE_STORE_CHECK_FLAG = 2;
   int CERT_QUERY_CONTENT_CERT = 1;
   int CERT_QUERY_CONTENT_CTL = 2;
   int CERT_QUERY_CONTENT_CRL = 3;
   int CERT_QUERY_CONTENT_SERIALIZED_STORE = 4;
   int CERT_QUERY_CONTENT_SERIALIZED_CERT = 5;
   int CERT_QUERY_CONTENT_SERIALIZED_CTL = 6;
   int CERT_QUERY_CONTENT_SERIALIZED_CRL = 7;
   int CERT_QUERY_CONTENT_PKCS7_SIGNED = 8;
   int CERT_QUERY_CONTENT_PKCS7_UNSIGNED = 9;
   int CERT_QUERY_CONTENT_PKCS7_SIGNED_EMBED = 10;
   int CERT_QUERY_CONTENT_PKCS10 = 11;
   int CERT_QUERY_CONTENT_PFX = 12;
   int CERT_QUERY_CONTENT_CERT_PAIR = 13;
   int CERT_QUERY_CONTENT_PFX_AND_LOAD = 14;
   int CERT_QUERY_CONTENT_FLAG_CERT = 2;
   int CERT_QUERY_CONTENT_FLAG_CTL = 4;
   int CERT_QUERY_CONTENT_FLAG_CRL = 8;
   int CERT_QUERY_CONTENT_FLAG_SERIALIZED_STORE = 16;
   int CERT_QUERY_CONTENT_FLAG_SERIALIZED_CERT = 32;
   int CERT_QUERY_CONTENT_FLAG_SERIALIZED_CTL = 64;
   int CERT_QUERY_CONTENT_FLAG_SERIALIZED_CRL = 128;
   int CERT_QUERY_CONTENT_FLAG_PKCS7_SIGNED = 256;
   int CERT_QUERY_CONTENT_FLAG_PKCS7_UNSIGNED = 512;
   int CERT_QUERY_CONTENT_FLAG_PKCS7_SIGNED_EMBED = 1024;
   int CERT_QUERY_CONTENT_FLAG_PKCS10 = 2048;
   int CERT_QUERY_CONTENT_FLAG_PFX = 4096;
   int CERT_QUERY_CONTENT_FLAG_CERT_PAIR = 8192;
   int CERT_QUERY_CONTENT_FLAG_PFX_AND_LOAD = 16384;
   int CERT_QUERY_CONTENT_FLAG_ALL = 16382;
   int CERT_QUERY_FORMAT_BINARY = 1;
   int CERT_QUERY_FORMAT_BASE64_ENCODED = 2;
   int CERT_QUERY_FORMAT_ASN_ASCII_HEX_ENCODED = 3;
   int CERT_QUERY_FORMAT_FLAG_BINARY = 2;
   int CERT_QUERY_FORMAT_FLAG_BASE64_ENCODED = 4;
   int CERT_QUERY_FORMAT_FLAG_ASN_ASCII_HEX_ENCODED = 8;
   int CERT_QUERY_FORMAT_FLAG_ALL = 14;
   int CERT_QUERY_OBJECT_FILE = 1;
   int CERT_QUERY_OBJECT_BLOB = 2;

   @Structure.FieldOrder(
      {
            "cbSize",
            "TrustStatus",
            "cChain",
            "rgpChain",
            "cLowerQualityChainContext",
            "rgpLowerQualityChainContext",
            "fHasRevocationFreshnessTime",
            "dwRevocationFreshnessTime",
            "dwCreateFlags",
            "ChainId"
      }
   )
   public static class CERT_CHAIN_CONTEXT extends Structure {
      public int cbSize;
      public WinCrypt.CERT_TRUST_STATUS TrustStatus;
      public int cChain;
      public Pointer rgpChain;
      public int cLowerQualityChainContext;
      public Pointer rgpLowerQualityChainContext;
      public boolean fHasRevocationFreshnessTime;
      public int dwRevocationFreshnessTime;
      public int dwCreateFlags;
      public Guid.GUID ChainId;

      public WinCrypt.CERT_SIMPLE_CHAIN[] getRgpChain() {
         WinCrypt.CERT_SIMPLE_CHAIN[] elements = new WinCrypt.CERT_SIMPLE_CHAIN[this.cChain];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = Structure.newInstance(WinCrypt.CERT_SIMPLE_CHAIN.class, this.rgpChain.getPointer(i * Native.POINTER_SIZE));
            elements[i].read();
         }

         return elements;
      }

      public WinCrypt.CERT_CHAIN_CONTEXT[] getRgpLowerQualityChainContext() {
         WinCrypt.CERT_CHAIN_CONTEXT[] elements = new WinCrypt.CERT_CHAIN_CONTEXT[this.cLowerQualityChainContext];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = Structure.newInstance(WinCrypt.CERT_CHAIN_CONTEXT.class, this.rgpLowerQualityChainContext.getPointer(i * Native.POINTER_SIZE));
            elements[i].read();
         }

         return elements;
      }

      public CERT_CHAIN_CONTEXT() {
         super(W32APITypeMapper.DEFAULT);
      }

      public static class ByReference extends WinCrypt.CERT_CHAIN_CONTEXT implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "pCertContext", "TrustStatus", "pRevocationInfo", "pIssuanceUsage", "pApplicationUsage", "pwszExtendedErrorInfo"})
   public static class CERT_CHAIN_ELEMENT extends Structure {
      public int cbSize;
      public WinCrypt.CERT_CONTEXT.ByReference pCertContext;
      public WinCrypt.CERT_TRUST_STATUS TrustStatus;
      public WinCrypt.CERT_REVOCATION_INFO.ByReference pRevocationInfo;
      public WinCrypt.CTL_USAGE.ByReference pIssuanceUsage;
      public WinCrypt.CTL_USAGE.ByReference pApplicationUsage;
      public String pwszExtendedErrorInfo;

      public CERT_CHAIN_ELEMENT() {
         super(W32APITypeMapper.UNICODE);
      }

      public CERT_CHAIN_ELEMENT(Pointer p) {
         super(p, 0, W32APITypeMapper.UNICODE);
      }

      public static class ByReference extends WinCrypt.CERT_CHAIN_ELEMENT implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder(
      {
            "cbSize",
            "RequestedUsage",
            "RequestedIssuancePolicy",
            "dwUrlRetrievalTimeout",
            "fCheckRevocationFreshnessTime",
            "dwRevocationFreshnessTime",
            "pftCacheResync",
            "pStrongSignPara",
            "dwStrongSignFlags"
      }
   )
   public static class CERT_CHAIN_PARA extends Structure {
      public int cbSize;
      public WinCrypt.CERT_USAGE_MATCH RequestedUsage;
      public WinCrypt.CERT_USAGE_MATCH RequestedIssuancePolicy;
      public int dwUrlRetrievalTimeout;
      public boolean fCheckRevocationFreshnessTime;
      public int dwRevocationFreshnessTime;
      public WinBase.FILETIME.ByReference pftCacheResync;
      public WinCrypt.CERT_STRONG_SIGN_PARA.ByReference pStrongSignPara;
      public int dwStrongSignFlags;

      public CERT_CHAIN_PARA() {
         super(W32APITypeMapper.DEFAULT);
      }

      public static class ByReference extends WinCrypt.CERT_CHAIN_PARA implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "dwFlags", "pvExtraPolicyPara"})
   public static class CERT_CHAIN_POLICY_PARA extends Structure {
      public int cbSize;
      public int dwFlags;
      public Pointer pvExtraPolicyPara;

      public static class ByReference extends WinCrypt.CERT_CHAIN_POLICY_PARA implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "dwError", "lChainIndex", "lElementIndex", "pvExtraPolicyStatus"})
   public static class CERT_CHAIN_POLICY_STATUS extends Structure {
      public int cbSize;
      public int dwError;
      public int lChainIndex;
      public int lElementIndex;
      public Pointer pvExtraPolicyStatus;

      public static class ByReference extends WinCrypt.CERT_CHAIN_POLICY_STATUS implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwCertEncodingType", "pbCertEncoded", "cbCertEncoded", "pCertInfo", "hCertStore"})
   public static class CERT_CONTEXT extends Structure {
      public int dwCertEncodingType;
      public Pointer pbCertEncoded;
      public int cbCertEncoded;
      public WinCrypt.CERT_INFO.ByReference pCertInfo;
      public WinCrypt.HCERTSTORE hCertStore;

      public static class ByReference extends WinCrypt.CERT_CONTEXT implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"pszObjId", "fCritical", "Value"})
   public static class CERT_EXTENSION extends Structure {
      public String pszObjId;
      public boolean fCritical;
      public WinCrypt.DATA_BLOB Value;

      public CERT_EXTENSION() {
         super(W32APITypeMapper.ASCII);
      }

      public static class ByReference extends WinCrypt.CERT_EXTENSION implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cExtension", "rgExtension"})
   public static class CERT_EXTENSIONS extends Structure {
      public int cExtension;
      public Pointer rgExtension;

      public WinCrypt.CERT_EXTENSION[] getRgExtension() {
         if (this.cExtension == 0) {
            return new WinCrypt.CERT_EXTENSION[0];
         } else {
            WinCrypt.CERT_EXTENSION[] ces = (WinCrypt.CERT_EXTENSION[])Structure.newInstance(WinCrypt.CERT_EXTENSION.class, this.rgExtension)
               .toArray(this.cExtension);
            ces[0].read();
            return ces;
         }
      }

      public static class ByReference extends WinCrypt.CERT_EXTENSIONS implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder(
      {
            "dwVersion",
            "SerialNumber",
            "SignatureAlgorithm",
            "Issuer",
            "NotBefore",
            "NotAfter",
            "Subject",
            "SubjectPublicKeyInfo",
            "IssuerUniqueId",
            "SubjectUniqueId",
            "cExtension",
            "rgExtension"
      }
   )
   public static class CERT_INFO extends Structure {
      public int dwVersion;
      public WinCrypt.DATA_BLOB SerialNumber;
      public WinCrypt.CRYPT_ALGORITHM_IDENTIFIER SignatureAlgorithm;
      public WinCrypt.DATA_BLOB Issuer;
      public WinBase.FILETIME NotBefore;
      public WinBase.FILETIME NotAfter;
      public WinCrypt.DATA_BLOB Subject;
      public WinCrypt.CERT_PUBLIC_KEY_INFO SubjectPublicKeyInfo;
      public WinCrypt.CRYPT_BIT_BLOB IssuerUniqueId;
      public WinCrypt.CRYPT_BIT_BLOB SubjectUniqueId;
      public int cExtension;
      public Pointer rgExtension;

      public WinCrypt.CERT_EXTENSION[] getRgExtension() {
         if (this.cExtension == 0) {
            return new WinCrypt.CERT_EXTENSION[0];
         } else {
            WinCrypt.CERT_EXTENSION[] ces = (WinCrypt.CERT_EXTENSION[])Structure.newInstance(WinCrypt.CERT_EXTENSION.class, this.rgExtension)
               .toArray(this.cExtension);
            ces[0].read();
            return ces;
         }
      }

      public static class ByReference extends WinCrypt.CERT_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"Algorithm", "PublicKey"})
   public static class CERT_PUBLIC_KEY_INFO extends Structure {
      public WinCrypt.CRYPT_ALGORITHM_IDENTIFIER Algorithm;
      public WinCrypt.CRYPT_BIT_BLOB PublicKey;

      public static class ByReference extends WinCrypt.CERT_PUBLIC_KEY_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "pBaseCRLContext", "pDeltaCRLContext", "pCrlEntry", "fDeltaCrlEntry"})
   public static class CERT_REVOCATION_CRL_INFO extends Structure {
      public int cbSize;
      public WinCrypt.CRL_CONTEXT.ByReference pBaseCRLContext;
      public WinCrypt.CRL_CONTEXT.ByReference pDeltaCRLContext;
      public WinCrypt.CRL_ENTRY.ByReference pCrlEntry;
      public boolean fDeltaCrlEntry;

      public CERT_REVOCATION_CRL_INFO() {
         super(W32APITypeMapper.DEFAULT);
      }

      public static class ByReference extends WinCrypt.CERT_REVOCATION_CRL_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "dwRevocationResult", "pszRevocationOid", "pvOidSpecificInfo", "fHasFreshnessTime", "dwFreshnessTime", "pCrlInfo"})
   public static class CERT_REVOCATION_INFO extends Structure {
      public int cbSize;
      public int dwRevocationResult;
      public String pszRevocationOid;
      public Pointer pvOidSpecificInfo;
      public boolean fHasFreshnessTime;
      public int dwFreshnessTime;
      public WinCrypt.CERT_REVOCATION_CRL_INFO.ByReference pCrlInfo;

      public CERT_REVOCATION_INFO() {
         super(W32APITypeMapper.ASCII);
      }

      public static class ByReference extends WinCrypt.CERT_REVOCATION_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "TrustStatus", "cElement", "rgpElement", "pTrustListInfo", "fHasRevocationFreshnessTime", "dwRevocationFreshnessTime"})
   public static class CERT_SIMPLE_CHAIN extends Structure {
      public int cbSize;
      public WinCrypt.CERT_TRUST_STATUS TrustStatus;
      public int cElement;
      public Pointer rgpElement;
      public WinCrypt.CERT_TRUST_LIST_INFO.ByReference pTrustListInfo;
      public boolean fHasRevocationFreshnessTime;
      public int dwRevocationFreshnessTime;

      public CERT_SIMPLE_CHAIN() {
         super(W32APITypeMapper.DEFAULT);
      }

      public WinCrypt.CERT_CHAIN_ELEMENT[] getRgpElement() {
         WinCrypt.CERT_CHAIN_ELEMENT[] elements = new WinCrypt.CERT_CHAIN_ELEMENT[this.cElement];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = Structure.newInstance(WinCrypt.CERT_CHAIN_ELEMENT.class, this.rgpElement.getPointer(i * Native.POINTER_SIZE));
            elements[i].read();
         }

         return elements;
      }

      public static class ByReference extends WinCrypt.CERT_SIMPLE_CHAIN implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "dwInfoChoice", "DUMMYUNIONNAME"})
   public static class CERT_STRONG_SIGN_PARA extends Structure {
      public int cbSize;
      public int dwInfoChoice;
      public WinCrypt.CERT_STRONG_SIGN_PARA.DUMMYUNION DUMMYUNIONNAME;

      public static class ByReference extends WinCrypt.CERT_CHAIN_PARA implements Structure.ByReference {
      }

      public class DUMMYUNION extends Union {
         Pointer pvInfo;
         WinCrypt.CERT_STRONG_SIGN_SERIALIZED_INFO.ByReference pSerializedInfo;
         WTypes.LPSTR pszOID;
      }
   }

   @Structure.FieldOrder({"dwFlags", "pwszCNGSignHashAlgids", "pwszCNGPubKeyMinBitLengths"})
   public static class CERT_STRONG_SIGN_SERIALIZED_INFO extends Structure {
      public int dwFlags;
      public String pwszCNGSignHashAlgids;
      public String pwszCNGPubKeyMinBitLengths;

      public CERT_STRONG_SIGN_SERIALIZED_INFO() {
         super(W32APITypeMapper.UNICODE);
      }

      public static class ByReference extends WinCrypt.CERT_CHAIN_PARA implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "pCtlEntry", "pCtlContext"})
   public static class CERT_TRUST_LIST_INFO extends Structure {
      public int cbSize;
      public WinCrypt.CTL_ENTRY.ByReference pCtlEntry;
      public WinCrypt.CTL_CONTEXT.ByReference pCtlContext;

      public static class ByReference extends WinCrypt.CERT_TRUST_LIST_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwErrorStatus", "dwInfoStatus"})
   public static class CERT_TRUST_STATUS extends Structure {
      public int dwErrorStatus;
      public int dwInfoStatus;

      public static class ByReference extends WinCrypt.CERT_TRUST_STATUS implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwType", "Usage"})
   public static class CERT_USAGE_MATCH extends Structure {
      public int dwType;
      public WinCrypt.CTL_USAGE Usage;

      public static class ByReference extends WinCrypt.CERT_USAGE_MATCH implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwCertEncodingType", "pbCrlEncoded", "cbCrlEncoded", "pCrlInfo", "hCertStore"})
   public static class CRL_CONTEXT extends Structure {
      public int dwCertEncodingType;
      public Pointer pbCrlEncoded;
      public int cbCrlEncoded;
      public WinCrypt.CRL_INFO.ByReference pCrlInfo;
      public WinCrypt.HCERTSTORE hCertStore;

      public static class ByReference extends WinCrypt.CRL_CONTEXT implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"SerialNumber", "RevocationDate", "cExtension", "rgExtension"})
   public static class CRL_ENTRY extends Structure {
      public WinCrypt.DATA_BLOB SerialNumber;
      public WinBase.FILETIME RevocationDate;
      public int cExtension;
      public Pointer rgExtension;

      public WinCrypt.CERT_EXTENSION[] getRgExtension() {
         if (this.cExtension == 0) {
            return new WinCrypt.CERT_EXTENSION[0];
         } else {
            WinCrypt.CERT_EXTENSION[] result = (WinCrypt.CERT_EXTENSION[])Structure.newInstance(WinCrypt.CERT_EXTENSION.class, this.rgExtension)
               .toArray(this.cExtension);
            result[0].read();
            return result;
         }
      }

      public static class ByReference extends WinCrypt.CRL_ENTRY implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwVersion", "SignatureAlgorithm", "Issuer", "ThisUpdate", "NextUpdate", "cCRLEntry", "rgCRLEntry", "cExtension", "rgExtension"})
   public static class CRL_INFO extends Structure {
      public int dwVersion;
      public WinCrypt.CRYPT_ALGORITHM_IDENTIFIER SignatureAlgorithm;
      public WinCrypt.DATA_BLOB Issuer;
      public WinBase.FILETIME ThisUpdate;
      public WinBase.FILETIME NextUpdate;
      public int cCRLEntry;
      public Pointer rgCRLEntry;
      public int cExtension;
      public Pointer rgExtension;

      public WinCrypt.CRL_ENTRY[] getRgCRLEntry() {
         if (this.cCRLEntry == 0) {
            return new WinCrypt.CRL_ENTRY[0];
         } else {
            WinCrypt.CRL_ENTRY[] result = (WinCrypt.CRL_ENTRY[])Structure.newInstance(WinCrypt.CRL_ENTRY.class, this.rgCRLEntry).toArray(this.cCRLEntry);
            result[0].read();
            return result;
         }
      }

      public WinCrypt.CERT_EXTENSION[] getRgExtension() {
         if (this.cExtension == 0) {
            return new WinCrypt.CERT_EXTENSION[0];
         } else {
            WinCrypt.CERT_EXTENSION[] result = (WinCrypt.CERT_EXTENSION[])Structure.newInstance(WinCrypt.CERT_EXTENSION.class, this.rgExtension)
               .toArray(this.cExtension);
            result[0].read();
            return result;
         }
      }

      public static class ByReference extends WinCrypt.CRL_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "dwPromptFlags", "hwndApp", "szPrompt"})
   public static class CRYPTPROTECT_PROMPTSTRUCT extends Structure {
      public int cbSize;
      public int dwPromptFlags;
      public WinDef.HWND hwndApp;
      public String szPrompt;

      public CRYPTPROTECT_PROMPTSTRUCT() {
         super(W32APITypeMapper.DEFAULT);
      }

      public CRYPTPROTECT_PROMPTSTRUCT(Pointer memory) {
         super(memory, 0, W32APITypeMapper.DEFAULT);
         this.read();
      }
   }

   @Structure.FieldOrder({"pszObjId", "Parameters"})
   public static class CRYPT_ALGORITHM_IDENTIFIER extends Structure {
      public String pszObjId;
      public WinCrypt.DATA_BLOB Parameters;

      public CRYPT_ALGORITHM_IDENTIFIER() {
         super(W32APITypeMapper.ASCII);
      }

      public static class ByReference extends WinCrypt.CRYPT_ALGORITHM_IDENTIFIER implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"pszObjId", "cValue", "rgValue"})
   public static class CRYPT_ATTRIBUTE extends Structure {
      public String pszObjId;
      public int cValue;
      public WinCrypt.DATA_BLOB.ByReference rgValue;

      public WinCrypt.DATA_BLOB[] getRgValue() {
         return (WinCrypt.DATA_BLOB[])this.rgValue.toArray(this.cValue);
      }

      public CRYPT_ATTRIBUTE() {
         super(W32APITypeMapper.ASCII);
      }

      public static class ByReference extends WinCrypt.CRYPT_ATTRIBUTE implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbData", "pbData", "cUnusedBits"})
   public static class CRYPT_BIT_BLOB extends Structure {
      public int cbData;
      public Pointer pbData;
      public int cUnusedBits;

      public static class ByReference extends WinCrypt.CRYPT_BIT_BLOB implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"pwszContainerName", "pwszProvName", "dwProvType", "dwFlags", "cProvParam", "rgProvParam", "dwKeySpec"})
   public static class CRYPT_KEY_PROV_INFO extends Structure {
      public String pwszContainerName;
      public String pwszProvName;
      public int dwProvType;
      public int dwFlags;
      public int cProvParam;
      public Pointer rgProvParam;
      public int dwKeySpec;

      public CRYPT_KEY_PROV_INFO() {
         super(W32APITypeMapper.UNICODE);
      }

      public WinCrypt.CRYPT_KEY_PROV_PARAM[] getRgProvParam() {
         WinCrypt.CRYPT_KEY_PROV_PARAM[] elements = new WinCrypt.CRYPT_KEY_PROV_PARAM[this.cProvParam];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = Structure.newInstance(WinCrypt.CRYPT_KEY_PROV_PARAM.class, this.rgProvParam.getPointer(i * Native.POINTER_SIZE));
            elements[i].read();
         }

         return elements;
      }

      public static class ByReference extends WinCrypt.CRYPT_KEY_PROV_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwParam", "pbData", "cbData", "dwFlags"})
   public static class CRYPT_KEY_PROV_PARAM extends Structure {
      public int dwParam;
      public Pointer pbData;
      public int cbData;
      public int dwFlags;

      public static class ByReference extends WinCrypt.CRYPT_KEY_PROV_PARAM implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder(
      {
            "cbSize",
            "dwMsgEncodingType",
            "pSigningCert",
            "HashAlgorithm",
            "pvHashAuxInfo",
            "cMsgCert",
            "rgpMsgCert",
            "cMsgCrl",
            "rgpMsgCrl",
            "cAuthAttr",
            "rgAuthAttr",
            "cUnauthAttr",
            "rgUnauthAttr",
            "dwFlags",
            "dwInnerContentType",
            "HashEncryptionAlgorithm",
            "pvHashEncryptionAuxInfo"
      }
   )
   public static class CRYPT_SIGN_MESSAGE_PARA extends Structure {
      public int cbSize;
      public int dwMsgEncodingType;
      public WinCrypt.CERT_CONTEXT.ByReference pSigningCert;
      public WinCrypt.CRYPT_ALGORITHM_IDENTIFIER HashAlgorithm;
      public Pointer pvHashAuxInfo;
      public int cMsgCert;
      public Pointer rgpMsgCert = null;
      public int cMsgCrl;
      public Pointer rgpMsgCrl = null;
      public int cAuthAttr;
      public Pointer rgAuthAttr = null;
      public int cUnauthAttr;
      public Pointer rgUnauthAttr = null;
      public int dwFlags;
      public int dwInnerContentType;
      public WinCrypt.CRYPT_ALGORITHM_IDENTIFIER HashEncryptionAlgorithm;
      public Pointer pvHashEncryptionAuxInfo;

      public WinCrypt.CERT_CONTEXT[] getRgpMsgCert() {
         WinCrypt.CERT_CONTEXT[] elements = new WinCrypt.CERT_CONTEXT[this.cMsgCrl];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = Structure.newInstance(WinCrypt.CERT_CONTEXT.class, this.rgpMsgCert.getPointer(i * Native.POINTER_SIZE));
            elements[i].read();
         }

         return elements;
      }

      public WinCrypt.CRL_CONTEXT[] getRgpMsgCrl() {
         WinCrypt.CRL_CONTEXT[] elements = new WinCrypt.CRL_CONTEXT[this.cMsgCrl];

         for (int i = 0; i < elements.length; i++) {
            elements[i] = Structure.newInstance(WinCrypt.CRL_CONTEXT.class, this.rgpMsgCrl.getPointer(i * Native.POINTER_SIZE));
            elements[i].read();
         }

         return elements;
      }

      public WinCrypt.CRYPT_ATTRIBUTE[] getRgAuthAttr() {
         return this.cAuthAttr == 0
            ? new WinCrypt.CRYPT_ATTRIBUTE[0]
            : (WinCrypt.CRYPT_ATTRIBUTE[])Structure.newInstance(WinCrypt.CRYPT_ATTRIBUTE.class, this.rgAuthAttr).toArray(this.cAuthAttr);
      }

      public WinCrypt.CRYPT_ATTRIBUTE[] getRgUnauthAttr() {
         return this.cUnauthAttr == 0
            ? new WinCrypt.CRYPT_ATTRIBUTE[0]
            : (WinCrypt.CRYPT_ATTRIBUTE[])Structure.newInstance(WinCrypt.CRYPT_ATTRIBUTE.class, this.rgUnauthAttr).toArray(this.cUnauthAttr);
      }

      public static class ByReference extends WinCrypt.CRYPT_SIGN_MESSAGE_PARA implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cbSize", "dwMsgAndCertEncodingType", "hCryptProv", "pfnGetSignerCertificate", "pvGetArg", "pStrongSignPara"})
   public static class CRYPT_VERIFY_MESSAGE_PARA extends Structure {
      public int cbSize;
      public int dwMsgAndCertEncodingType;
      public WinCrypt.HCRYPTPROV_LEGACY hCryptProv;
      public WinCrypt.CryptGetSignerCertificateCallback pfnGetSignerCertificate;
      public Pointer pvGetArg;
      public WinCrypt.CERT_STRONG_SIGN_PARA.ByReference pStrongSignPara;

      @Override
      public void write() {
         this.cbSize = this.size();
         super.write();
      }

      public static class ByReference extends WinCrypt.CRYPT_SIGN_MESSAGE_PARA implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"dwMsgAndCertEncodingType", "pbCtlEncoded", "cbCtlEncoded", "pCtlInfo", "hCertStore", "hCryptMsg", "pbCtlContent", "cbCtlContent"})
   public static class CTL_CONTEXT extends Structure {
      public int dwMsgAndCertEncodingType;
      public Pointer pbCtlEncoded;
      public int cbCtlEncoded;
      public WinCrypt.CTL_INFO.ByReference pCtlInfo;
      public WinCrypt.HCERTSTORE hCertStore;
      public WinCrypt.HCRYPTMSG hCryptMsg;
      public Pointer pbCtlContent;
      public int cbCtlContent;

      public static class ByReference extends WinCrypt.CTL_CONTEXT implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"SubjectIdentifier", "cAttribute", "rgAttribute"})
   public static class CTL_ENTRY extends Structure {
      public WinCrypt.DATA_BLOB SubjectIdentifier;
      public int cAttribute;
      public Pointer rgAttribute;

      public WinCrypt.CRYPT_ATTRIBUTE[] getRgAttribute() {
         if (this.cAttribute == 0) {
            return new WinCrypt.CRYPT_ATTRIBUTE[0];
         } else {
            WinCrypt.CRYPT_ATTRIBUTE[] result = (WinCrypt.CRYPT_ATTRIBUTE[])Structure.newInstance(WinCrypt.CRYPT_ATTRIBUTE.class, this.rgAttribute)
               .toArray(this.cAttribute);
            result[0].read();
            return result;
         }
      }

      public static class ByReference extends WinCrypt.CTL_ENTRY implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder(
      {
            "dwVersion",
            "SubjectUsage",
            "ListIdentifier",
            "SequenceNumber",
            "ThisUpdate",
            "NextUpdate",
            "SubjectAlgorithm",
            "cCTLEntry",
            "rgCTLEntry",
            "cExtension",
            "rgExtension"
      }
   )
   public static class CTL_INFO extends Structure {
      public int dwVersion;
      public WinCrypt.CTL_USAGE SubjectUsage;
      public WinCrypt.DATA_BLOB ListIdentifier;
      public WinCrypt.DATA_BLOB SequenceNumber;
      public WinBase.FILETIME ThisUpdate;
      public WinBase.FILETIME NextUpdate;
      public WinCrypt.CRYPT_ALGORITHM_IDENTIFIER SubjectAlgorithm;
      public int cCTLEntry;
      public Pointer rgCTLEntry;
      public int cExtension;
      public Pointer rgExtension;

      public WinCrypt.CTL_ENTRY[] getRgCTLEntry() {
         if (this.cCTLEntry == 0) {
            return new WinCrypt.CTL_ENTRY[0];
         } else {
            WinCrypt.CTL_ENTRY[] result = (WinCrypt.CTL_ENTRY[])Structure.newInstance(WinCrypt.CTL_ENTRY.class, this.rgCTLEntry).toArray(this.cCTLEntry);
            result[0].read();
            return result;
         }
      }

      public WinCrypt.CERT_EXTENSION[] getRgExtension() {
         if (this.cExtension == 0) {
            return new WinCrypt.CERT_EXTENSION[0];
         } else {
            WinCrypt.CERT_EXTENSION[] result = (WinCrypt.CERT_EXTENSION[])Structure.newInstance(WinCrypt.CERT_EXTENSION.class, this.rgExtension)
               .toArray(this.cExtension);
            result[0].read();
            return result;
         }
      }

      public static class ByReference extends WinCrypt.CTL_INFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cUsageIdentifier", "rgpszUsageIdentifier"})
   public static class CTL_USAGE extends Structure {
      public int cUsageIdentifier;
      public Pointer rgpszUsageIdentifier;

      public String[] getRgpszUsageIdentier() {
         return this.cUsageIdentifier == 0 ? new String[0] : this.rgpszUsageIdentifier.getStringArray(0L, this.cUsageIdentifier);
      }

      public void setRgpszUsageIdentier(String[] array) {
         if (array != null && array.length != 0) {
            this.cUsageIdentifier = array.length;
            this.rgpszUsageIdentifier = new StringArray(array);
         } else {
            this.cUsageIdentifier = 0;
            this.rgpszUsageIdentifier = null;
         }
      }

      public static class ByReference extends WinCrypt.CTL_USAGE implements Structure.ByReference {
      }
   }

   public static class CertStoreProviderName implements NativeMapped {
      private final Pointer pointer;

      private CertStoreProviderName(Pointer fixedValue) {
         this.pointer = fixedValue;
      }

      public CertStoreProviderName() {
         this.pointer = Pointer.NULL;
      }

      public CertStoreProviderName(int fixedValue) {
         this.pointer = Pointer.createConstant(fixedValue);
      }

      public CertStoreProviderName(String name) {
         byte[] nameBytes = Native.toByteArray(name);
         this.pointer = new Memory(nameBytes.length);
         this.pointer.write(0L, nameBytes, 0, nameBytes.length);
      }

      @Override
      public Object fromNative(Object nativeValue, FromNativeContext fnc) {
         return nativeValue == null ? null : new WinCrypt.CertStoreProviderName((Pointer)nativeValue);
      }

      @Override
      public Object toNative() {
         return this.pointer;
      }

      @Override
      public Class<?> nativeType() {
         return Pointer.class;
      }
   }

   public interface CryptGetSignerCertificateCallback extends StdCallLibrary.StdCallCallback {
      WinCrypt.CERT_CONTEXT.ByReference callback(Pointer var1, int var2, WinCrypt.CERT_INFO var3, WinCrypt.HCERTSTORE var4);
   }

   @Structure.FieldOrder({"cbData", "pbData"})
   public static class DATA_BLOB extends Structure {
      public int cbData;
      public Pointer pbData;

      public DATA_BLOB() {
      }

      public DATA_BLOB(Pointer memory) {
         super(memory);
         this.read();
      }

      public DATA_BLOB(byte[] data) {
         if (data.length > 0) {
            this.pbData = new Memory(data.length);
            this.pbData.write(0L, data, 0, data.length);
            this.cbData = data.length;
         } else {
            this.pbData = new Memory(1L);
            this.cbData = 0;
         }
      }

      public DATA_BLOB(String s) {
         this(Native.toByteArray(s));
      }

      public byte[] getData() {
         return this.pbData == null ? null : this.pbData.getByteArray(0L, this.cbData);
      }

      public static class ByReference extends WinCrypt.DATA_BLOB implements Structure.ByReference {
      }
   }

   public static class HCERTCHAINENGINE extends WinNT.HANDLE {
      public HCERTCHAINENGINE() {
      }

      public HCERTCHAINENGINE(Pointer p) {
         super(p);
      }
   }

   public static class HCERTSTORE extends WinNT.HANDLE {
      public HCERTSTORE() {
      }

      public HCERTSTORE(Pointer p) {
         super(p);
      }
   }

   public static class HCRYPTMSG extends WinNT.HANDLE {
      public HCRYPTMSG() {
      }

      public HCRYPTMSG(Pointer p) {
         super(p);
      }
   }

   public static class HCRYPTPROV_LEGACY extends BaseTSD.ULONG_PTR {
      public HCRYPTPROV_LEGACY() {
      }

      public HCRYPTPROV_LEGACY(long value) {
         super(value);
      }
   }
}
