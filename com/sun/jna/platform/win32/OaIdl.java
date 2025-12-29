package com.sun.jna.platform.win32;

import com.sun.jna.IntegerType;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Dispatch;
import com.sun.jna.platform.win32.COM.TypeComp;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.ptr.PointerByReference;
import java.io.Closeable;
import java.util.Date;

public interface OaIdl {
   long DATE_OFFSET = new Date(-1, 11, 30, 0, 0, 0).getTime();
   OaIdl.DISPID DISPID_COLLECT = new OaIdl.DISPID(-8);
   OaIdl.DISPID DISPID_CONSTRUCTOR = new OaIdl.DISPID(-6);
   OaIdl.DISPID DISPID_DESTRUCTOR = new OaIdl.DISPID(-7);
   OaIdl.DISPID DISPID_EVALUATE = new OaIdl.DISPID(-5);
   OaIdl.DISPID DISPID_NEWENUM = new OaIdl.DISPID(-4);
   OaIdl.DISPID DISPID_PROPERTYPUT = new OaIdl.DISPID(-3);
   OaIdl.DISPID DISPID_UNKNOWN = new OaIdl.DISPID(-1);
   OaIdl.DISPID DISPID_VALUE = new OaIdl.DISPID(0);
   OaIdl.MEMBERID MEMBERID_NIL = new OaIdl.MEMBERID(DISPID_UNKNOWN.intValue());
   int FADF_AUTO = 1;
   int FADF_STATIC = 2;
   int FADF_EMBEDDED = 4;
   int FADF_FIXEDSIZE = 16;
   int FADF_RECORD = 32;
   int FADF_HAVEIID = 64;
   int FADF_HAVEVARTYPE = 128;
   int FADF_BSTR = 256;
   int FADF_UNKNOWN = 512;
   int FADF_DISPATCH = 1024;
   int FADF_VARIANT = 2048;
   int FADF_RESERVED = 61448;

   @Structure.FieldOrder({"tdescElem", "cDims", "rgbounds"})
   public static class ARRAYDESC extends Structure {
      public OaIdl.TYPEDESC tdescElem;
      public short cDims;
      public OaIdl.SAFEARRAYBOUND[] rgbounds = new OaIdl.SAFEARRAYBOUND[]{new OaIdl.SAFEARRAYBOUND()};

      public ARRAYDESC() {
      }

      public ARRAYDESC(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public ARRAYDESC(OaIdl.TYPEDESC tdescElem, short cDims, OaIdl.SAFEARRAYBOUND[] rgbounds) {
         this.tdescElem = tdescElem;
         this.cDims = cDims;
         if (rgbounds.length != this.rgbounds.length) {
            throw new IllegalArgumentException("Wrong array size !");
         } else {
            this.rgbounds = rgbounds;
         }
      }

      public static class ByReference extends OaIdl.ARRAYDESC implements Structure.ByReference {
      }
   }

   public static class BINDPTR extends Union {
      public OaIdl.FUNCDESC lpfuncdesc;
      public OaIdl.VARDESC lpvardesc;
      public TypeComp lptcomp;

      public BINDPTR() {
      }

      public BINDPTR(OaIdl.VARDESC lpvardesc) {
         this.lpvardesc = lpvardesc;
         this.setType(OaIdl.VARDESC.class);
      }

      public BINDPTR(TypeComp lptcomp) {
         this.lptcomp = lptcomp;
         this.setType(TypeComp.class);
      }

      public BINDPTR(OaIdl.FUNCDESC lpfuncdesc) {
         this.lpfuncdesc = lpfuncdesc;
         this.setType(OaIdl.FUNCDESC.class);
      }

      public static class ByReference extends OaIdl.BINDPTR implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder("value")
   public static class CALLCONV extends Structure {
      public static final int CC_FASTCALL = 0;
      public static final int CC_CDECL = 1;
      public static final int CC_MSCPASCAL = 2;
      public static final int CC_PASCAL = 2;
      public static final int CC_MACPASCAL = 3;
      public static final int CC_STDCALL = 4;
      public static final int CC_FPFASTCALL = 5;
      public static final int CC_SYSCALL = 6;
      public static final int CC_MPWCDECL = 7;
      public static final int CC_MPWPASCAL = 8;
      public static final int CC_MAX = 9;
      public int value;

      public CALLCONV() {
      }

      public CALLCONV(int value) {
         this.value = value;
      }

      public static class ByReference extends OaIdl.CALLCONV implements Structure.ByReference {
      }
   }

   public static class CURRENCY extends Union {
      public OaIdl.CURRENCY._CURRENCY currency;
      public WinDef.LONGLONG int64;

      public CURRENCY() {
      }

      public CURRENCY(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.CURRENCY implements Structure.ByReference {
      }

      @Structure.FieldOrder({"Lo", "Hi"})
      public static class _CURRENCY extends Structure {
         public WinDef.ULONG Lo;
         public WinDef.LONG Hi;

         public _CURRENCY() {
         }

         public _CURRENCY(Pointer pointer) {
            super(pointer);
            this.read();
         }
      }
   }

   @Structure.FieldOrder("date")
   public static class DATE extends Structure {
      private static final long MICRO_SECONDS_PER_DAY = 86400000L;
      public double date;

      public DATE() {
      }

      public DATE(double date) {
         this.date = date;
      }

      public DATE(Date javaDate) {
         this.setFromJavaDate(javaDate);
      }

      public Date getAsJavaDate() {
         long days = (long)this.date * 86400000L + OaIdl.DATE_OFFSET;
         double timePart = 24.0 * Math.abs(this.date - (long)this.date);
         int hours = (int)timePart;
         timePart = 60.0 * (timePart - (int)timePart);
         int minutes = (int)timePart;
         timePart = 60.0 * (timePart - (int)timePart);
         int seconds = (int)timePart;
         timePart = 1000.0 * (timePart - (int)timePart);
         int milliseconds = (int)timePart;
         Date baseDate = new Date(days);
         baseDate.setHours(hours);
         baseDate.setMinutes(minutes);
         baseDate.setSeconds(seconds);
         baseDate.setTime(baseDate.getTime() + milliseconds);
         return baseDate;
      }

      public void setFromJavaDate(Date javaDate) {
         double msSinceOrigin = javaDate.getTime() - OaIdl.DATE_OFFSET;
         double daysAsFract = msSinceOrigin / 8.64E7;
         Date dayDate = new Date(javaDate.getTime());
         dayDate.setHours(0);
         dayDate.setMinutes(0);
         dayDate.setSeconds(0);
         dayDate.setTime(dayDate.getTime() / 1000L * 1000L);
         double integralPart = Math.floor(daysAsFract);
         double fractionalPart = Math.signum(daysAsFract) * ((javaDate.getTime() - dayDate.getTime()) / 8.64E7);
         this.date = integralPart + fractionalPart;
      }

      public static class ByReference extends OaIdl.DATE implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"wReserved", "decimal1", "Hi32", "decimal2"})
   public static class DECIMAL extends Structure {
      public short wReserved;
      public OaIdl.DECIMAL._DECIMAL1 decimal1;
      public NativeLong Hi32;
      public OaIdl.DECIMAL._DECIMAL2 decimal2;

      public DECIMAL() {
      }

      public DECIMAL(Pointer pointer) {
         super(pointer);
      }

      public static class ByReference extends OaIdl.DECIMAL implements Structure.ByReference {
      }

      public static class _DECIMAL1 extends Union {
         public WinDef.USHORT signscale;
         public OaIdl.DECIMAL._DECIMAL1._DECIMAL1_DECIMAL decimal1_DECIMAL;

         public _DECIMAL1() {
            this.setType("signscale");
         }

         public _DECIMAL1(Pointer pointer) {
            super(pointer);
            this.setType("signscale");
            this.read();
         }

         @Structure.FieldOrder({"scale", "sign"})
         public static class _DECIMAL1_DECIMAL extends Structure {
            public WinDef.BYTE scale;
            public WinDef.BYTE sign;

            public _DECIMAL1_DECIMAL() {
            }

            public _DECIMAL1_DECIMAL(Pointer pointer) {
               super(pointer);
            }
         }
      }

      public static class _DECIMAL2 extends Union {
         public WinDef.ULONGLONG Lo64;
         public OaIdl.DECIMAL._DECIMAL2._DECIMAL2_DECIMAL decimal2_DECIMAL;

         public _DECIMAL2() {
            this.setType("Lo64");
         }

         public _DECIMAL2(Pointer pointer) {
            super(pointer);
            this.setType("Lo64");
            this.read();
         }

         @Structure.FieldOrder({"Lo32", "Mid32"})
         public static class _DECIMAL2_DECIMAL extends Structure {
            public WinDef.BYTE Lo32;
            public WinDef.BYTE Mid32;

            public _DECIMAL2_DECIMAL() {
            }

            public _DECIMAL2_DECIMAL(Pointer pointer) {
               super(pointer);
            }
         }
      }
   }

   @Structure.FieldOrder("value")
   public static class DESCKIND extends Structure {
      public int value;
      public static final int DESCKIND_NONE = 0;
      public static final int DESCKIND_FUNCDESC = 1;
      public static final int DESCKIND_VARDESC = 2;
      public static final int DESCKIND_TYPECOMP = 3;
      public static final int DESCKIND_IMPLICITAPPOBJ = 4;
      public static final int DESCKIND_MAX = 5;

      public DESCKIND() {
      }

      public DESCKIND(int value) {
         this.value = value;
      }

      public DESCKIND(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.DESCKIND implements Structure.ByReference {
      }
   }

   public static class DISPID extends WinDef.LONG {
      private static final long serialVersionUID = 1L;

      public DISPID() {
         this(0);
      }

      public DISPID(int value) {
         super(value);
      }
   }

   public static class DISPIDByReference extends com.sun.jna.ptr.ByReference {
      public DISPIDByReference() {
         this(new OaIdl.DISPID(0));
      }

      public DISPIDByReference(OaIdl.DISPID value) {
         super(OaIdl.DISPID.SIZE);
         this.setValue(value);
      }

      public void setValue(OaIdl.DISPID value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public OaIdl.DISPID getValue() {
         return new OaIdl.DISPID(this.getPointer().getInt(0L));
      }
   }

   @Structure.FieldOrder({"tdesc", "_elemdesc"})
   public static class ELEMDESC extends Structure {
      public OaIdl.TYPEDESC tdesc;
      public OaIdl.ELEMDESC._ELEMDESC _elemdesc;

      public ELEMDESC() {
      }

      public ELEMDESC(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.ELEMDESC implements Structure.ByReference {
      }

      public static class _ELEMDESC extends Union {
         public OaIdl.IDLDESC idldesc;
         public OaIdl.PARAMDESC paramdesc;

         public _ELEMDESC() {
         }

         public _ELEMDESC(Pointer pointer) {
            super(pointer);
            this.setType("paramdesc");
            this.read();
         }

         public _ELEMDESC(OaIdl.PARAMDESC paramdesc) {
            this.paramdesc = paramdesc;
            this.setType("paramdesc");
         }

         public _ELEMDESC(OaIdl.IDLDESC idldesc) {
            this.idldesc = idldesc;
            this.setType("idldesc");
         }

         public static class ByReference extends OaIdl.ELEMDESC._ELEMDESC implements Structure.ByReference {
         }
      }
   }

   @Structure.FieldOrder({"wCode", "wReserved", "bstrSource", "bstrDescription", "bstrHelpFile", "dwHelpContext", "pvReserved", "pfnDeferredFillIn", "scode"})
   public static class EXCEPINFO extends Structure {
      public WinDef.WORD wCode;
      public WinDef.WORD wReserved;
      public WTypes.BSTR bstrSource;
      public WTypes.BSTR bstrDescription;
      public WTypes.BSTR bstrHelpFile;
      public WinDef.DWORD dwHelpContext;
      public WinDef.PVOID pvReserved;
      public OaIdl.EXCEPINFO.ByReference pfnDeferredFillIn;
      public WinDef.SCODE scode;

      public EXCEPINFO() {
      }

      public EXCEPINFO(Pointer p) {
         super(p);
      }

      public static class ByReference extends OaIdl.EXCEPINFO implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder("elemDescArg")
   public static class ElemDescArg extends Structure {
      public OaIdl.ELEMDESC[] elemDescArg = new OaIdl.ELEMDESC[]{new OaIdl.ELEMDESC()};

      public ElemDescArg() {
      }

      public ElemDescArg(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.ElemDescArg implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder(
      {"memid", "lprgscode", "lprgelemdescParam", "funckind", "invkind", "callconv", "cParams", "cParamsOpt", "oVft", "cScodes", "elemdescFunc", "wFuncFlags"}
   )
   public static class FUNCDESC extends Structure {
      public OaIdl.MEMBERID memid;
      public OaIdl.ScodeArg.ByReference lprgscode;
      public OaIdl.ElemDescArg.ByReference lprgelemdescParam;
      public OaIdl.FUNCKIND funckind;
      public OaIdl.INVOKEKIND invkind;
      public OaIdl.CALLCONV callconv;
      public WinDef.SHORT cParams;
      public WinDef.SHORT cParamsOpt;
      public WinDef.SHORT oVft;
      public WinDef.SHORT cScodes;
      public OaIdl.ELEMDESC elemdescFunc;
      public WinDef.WORD wFuncFlags;

      public FUNCDESC() {
      }

      public FUNCDESC(Pointer pointer) {
         super(pointer);
         this.read();
         if (this.cParams.shortValue() > 1) {
            this.lprgelemdescParam.elemDescArg = new OaIdl.ELEMDESC[this.cParams.shortValue()];
            this.lprgelemdescParam.read();
         }
      }

      public static class ByReference extends OaIdl.FUNCDESC implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder("value")
   public static class FUNCKIND extends Structure {
      public static final int FUNC_VIRTUAL = 0;
      public static final int FUNC_PUREVIRTUAL = 1;
      public static final int FUNC_NONVIRTUAL = 2;
      public static final int FUNC_STATIC = 3;
      public static final int FUNC_DISPATCH = 4;
      public int value;

      public FUNCKIND() {
      }

      public FUNCKIND(int value) {
         this.value = value;
      }

      public static class ByReference extends OaIdl.FUNCKIND implements Structure.ByReference {
      }
   }

   public static class HREFTYPE extends WinDef.DWORD {
      private static final long serialVersionUID = 1L;

      public HREFTYPE() {
      }

      public HREFTYPE(long value) {
         super(value);
      }
   }

   public static class HREFTYPEByReference extends WinDef.DWORDByReference {
      public HREFTYPEByReference() {
         this(new OaIdl.HREFTYPE(0L));
      }

      public HREFTYPEByReference(WinDef.DWORD value) {
         super(value);
      }

      public void setValue(OaIdl.HREFTYPE value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public OaIdl.HREFTYPE getValue() {
         return new OaIdl.HREFTYPE(this.getPointer().getInt(0L));
      }
   }

   @Structure.FieldOrder({"dwReserved", "wIDLFlags"})
   public static class IDLDESC extends Structure {
      public BaseTSD.ULONG_PTR dwReserved;
      public WinDef.USHORT wIDLFlags;

      public IDLDESC() {
      }

      public IDLDESC(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public IDLDESC(BaseTSD.ULONG_PTR dwReserved, WinDef.USHORT wIDLFlags) {
         this.dwReserved = dwReserved;
         this.wIDLFlags = wIDLFlags;
      }

      public static class ByReference extends OaIdl.IDLDESC implements Structure.ByReference {
         public ByReference() {
         }

         public ByReference(OaIdl.IDLDESC idldesc) {
            super(idldesc.dwReserved, idldesc.wIDLFlags);
         }
      }
   }

   @Structure.FieldOrder("value")
   public static class INVOKEKIND extends Structure {
      public static final OaIdl.INVOKEKIND INVOKE_FUNC = new OaIdl.INVOKEKIND(1);
      public static final OaIdl.INVOKEKIND INVOKE_PROPERTYGET = new OaIdl.INVOKEKIND(2);
      public static final OaIdl.INVOKEKIND INVOKE_PROPERTYPUT = new OaIdl.INVOKEKIND(4);
      public static final OaIdl.INVOKEKIND INVOKE_PROPERTYPUTREF = new OaIdl.INVOKEKIND(8);
      public int value;

      public INVOKEKIND() {
      }

      public INVOKEKIND(int value) {
         this.value = value;
      }

      public static class ByReference extends OaIdl.INVOKEKIND implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder("value")
   public static class LIBFLAGS extends Structure {
      public int value;
      public static final int LIBFLAG_FRESTRICTED = 1;
      public static final int LIBFLAG_FCONTROL = 2;
      public static final int LIBFLAG_FHIDDEN = 4;
      public static final int LIBFLAG_FHASDISKIMAGE = 8;

      public LIBFLAGS() {
      }

      public LIBFLAGS(int value) {
         this.value = value;
      }

      public LIBFLAGS(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.LIBFLAGS implements Structure.ByReference {
      }
   }

   public static class MEMBERID extends OaIdl.DISPID {
      private static final long serialVersionUID = 1L;

      public MEMBERID() {
         this(0);
      }

      public MEMBERID(int value) {
         super(value);
      }
   }

   public static class MEMBERIDByReference extends com.sun.jna.ptr.ByReference {
      public MEMBERIDByReference() {
         this(new OaIdl.MEMBERID(0));
      }

      public MEMBERIDByReference(OaIdl.MEMBERID value) {
         super(OaIdl.MEMBERID.SIZE);
         this.setValue(value);
      }

      public void setValue(OaIdl.MEMBERID value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public OaIdl.MEMBERID getValue() {
         return new OaIdl.MEMBERID(this.getPointer().getInt(0L));
      }
   }

   @Structure.FieldOrder({"pparamdescex", "wParamFlags"})
   public static class PARAMDESC extends Structure {
      public Pointer pparamdescex;
      public WinDef.USHORT wParamFlags;

      public PARAMDESC() {
      }

      public PARAMDESC(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.PARAMDESC implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cBytes", "varDefaultValue"})
   public static class PARAMDESCEX extends Structure {
      public WinDef.ULONG cBytes;
      public Variant.VariantArg varDefaultValue;

      public PARAMDESCEX() {
      }

      public PARAMDESCEX(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.PARAMDESCEX implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cDims", "fFeatures", "cbElements", "cLocks", "pvData", "rgsabound"})
   public static class SAFEARRAY extends Structure implements Closeable {
      public WinDef.USHORT cDims;
      public WinDef.USHORT fFeatures;
      public WinDef.ULONG cbElements;
      public WinDef.ULONG cLocks;
      public WinDef.PVOID pvData;
      public OaIdl.SAFEARRAYBOUND[] rgsabound = new OaIdl.SAFEARRAYBOUND[]{new OaIdl.SAFEARRAYBOUND()};

      public SAFEARRAY() {
      }

      public SAFEARRAY(Pointer pointer) {
         super(pointer);
         this.read();
      }

      @Override
      public void read() {
         super.read();
         if (this.cDims.intValue() > 0) {
            this.rgsabound = (OaIdl.SAFEARRAYBOUND[])this.rgsabound[0].toArray(this.cDims.intValue());
         } else {
            this.rgsabound = new OaIdl.SAFEARRAYBOUND[]{new OaIdl.SAFEARRAYBOUND()};
         }
      }

      public static OaIdl.SAFEARRAY.ByReference createSafeArray(int... size) {
         return createSafeArray(new WTypes.VARTYPE(12), size);
      }

      public static OaIdl.SAFEARRAY.ByReference createSafeArray(WTypes.VARTYPE vartype, int... size) {
         OaIdl.SAFEARRAYBOUND[] rgsabound = (OaIdl.SAFEARRAYBOUND[])new OaIdl.SAFEARRAYBOUND().toArray(size.length);

         for (int i = 0; i < size.length; i++) {
            rgsabound[i].lLbound = new WinDef.LONG(0L);
            rgsabound[i].cElements = new WinDef.ULONG(size[size.length - i - 1]);
         }

         return OleAuto.INSTANCE.SafeArrayCreate(vartype, new WinDef.UINT(size.length), rgsabound);
      }

      public void putElement(Object arg, int... indices) {
         WinDef.LONG[] paramIndices = new WinDef.LONG[indices.length];

         for (int i = 0; i < indices.length; i++) {
            paramIndices[i] = new WinDef.LONG(indices[indices.length - i - 1]);
         }

         switch (this.getVarType().intValue()) {
            case 2:
            case 18: {
               Memory mem = new Memory(2L);
               mem.setShort(0L, ((Number)arg).shortValue());
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 3:
            case 19:
            case 22:
            case 23: {
               Memory mem = new Memory(4L);
               mem.setInt(0L, ((Number)arg).intValue());
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 4: {
               Memory mem = new Memory(4L);
               mem.setFloat(0L, ((Number)arg).floatValue());
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 5: {
               Memory mem = new Memory(8L);
               mem.setDouble(0L, ((Number)arg).doubleValue());
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 6: {
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((OaIdl.CURRENCY)arg).getPointer());
               COMUtils.checkRC(hr);
               break;
            }
            case 7: {
               Memory mem = new Memory(8L);
               mem.setDouble(0L, ((OaIdl.DATE)arg).date);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 8:
               if (arg instanceof String) {
                  WTypes.BSTR bstr = OleAuto.INSTANCE.SysAllocString((String)arg);
                  WinNT.HRESULT hrx = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, bstr.getPointer());
                  OleAuto.INSTANCE.SysFreeString(bstr);
                  COMUtils.checkRC(hrx);
               } else {
                  WinNT.HRESULT hrx = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((WTypes.BSTR)arg).getPointer());
                  COMUtils.checkRC(hrx);
               }
               break;
            case 9: {
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((Dispatch)arg).getPointer());
               COMUtils.checkRC(hr);
               break;
            }
            case 10: {
               Memory mem = new Memory(4L);
               mem.setInt(0L, ((Number)arg).intValue());
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 11: {
               Memory mem = new Memory(2L);
               if (arg instanceof Boolean) {
                  mem.setShort(0L, (short)((Boolean)arg ? '\uffff' : 0));
               } else {
                  mem.setShort(0L, (short)(((Number)arg).intValue() > 0 ? '\uffff' : 0));
               }

               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               break;
            }
            case 12: {
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((Variant.VARIANT)arg).getPointer());
               COMUtils.checkRC(hr);
               break;
            }
            case 13: {
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((Unknown)arg).getPointer());
               COMUtils.checkRC(hr);
               break;
            }
            case 14: {
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, ((OaIdl.DECIMAL)arg).getPointer());
               COMUtils.checkRC(hr);
               break;
            }
            case 15:
            case 20:
            case 21:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            default:
               throw new IllegalStateException("Can't parse array content - type not supported: " + this.getVarType().intValue());
            case 16:
            case 17: {
               Memory mem = new Memory(1L);
               mem.setByte(0L, ((Number)arg).byteValue());
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPutElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
            }
         }
      }

      public Object getElement(int... indices) {
         WinDef.LONG[] paramIndices = new WinDef.LONG[indices.length];

         for (int i = 0; i < indices.length; i++) {
            paramIndices[i] = new WinDef.LONG(indices[indices.length - i - 1]);
         }

         Object result;
         switch (this.getVarType().intValue()) {
            case 2:
            case 18: {
               Memory mem = new Memory(2L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = mem.getShort(0L);
               break;
            }
            case 3:
            case 19:
            case 22:
            case 23: {
               Memory mem = new Memory(4L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = mem.getInt(0L);
               break;
            }
            case 4: {
               Memory mem = new Memory(4L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = mem.getFloat(0L);
               break;
            }
            case 5: {
               Memory mem = new Memory(8L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = mem.getDouble(0L);
               break;
            }
            case 6: {
               OaIdl.CURRENCY currency = new OaIdl.CURRENCY();
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, currency.getPointer());
               COMUtils.checkRC(hr);
               result = currency;
               break;
            }
            case 7: {
               Memory mem = new Memory(8L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = new OaIdl.DATE(mem.getDouble(0L));
               break;
            }
            case 8: {
               PointerByReference pbr = new PointerByReference();
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, pbr.getPointer());
               COMUtils.checkRC(hr);
               WTypes.BSTR bstr = new WTypes.BSTR(pbr.getValue());
               result = bstr.getValue();
               OleAuto.INSTANCE.SysFreeString(bstr);
               break;
            }
            case 9: {
               PointerByReference pbr = new PointerByReference();
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, pbr.getPointer());
               COMUtils.checkRC(hr);
               result = new Dispatch(pbr.getValue());
               break;
            }
            case 10: {
               Memory mem = new Memory(4L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = new WinDef.SCODE(mem.getInt(0L));
               break;
            }
            case 11: {
               Memory mem = new Memory(2L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = mem.getShort(0L) != 0;
               break;
            }
            case 12: {
               Variant.VARIANT holder = new Variant.VARIANT();
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, holder.getPointer());
               COMUtils.checkRC(hr);
               result = holder;
               break;
            }
            case 13: {
               PointerByReference pbr = new PointerByReference();
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, pbr.getPointer());
               COMUtils.checkRC(hr);
               result = new Unknown(pbr.getValue());
               break;
            }
            case 14: {
               OaIdl.DECIMAL decimal = new OaIdl.DECIMAL();
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, decimal.getPointer());
               COMUtils.checkRC(hr);
               result = decimal;
               break;
            }
            case 15:
            case 20:
            case 21:
            case 24:
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            default:
               throw new IllegalStateException("Can't parse array content - type not supported: " + this.getVarType().intValue());
            case 16:
            case 17: {
               Memory mem = new Memory(1L);
               WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayGetElement(this, paramIndices, mem);
               COMUtils.checkRC(hr);
               result = mem.getByte(0L);
            }
         }

         return result;
      }

      public Pointer ptrOfIndex(int... indices) {
         WinDef.LONG[] paramIndices = new WinDef.LONG[indices.length];

         for (int i = 0; i < indices.length; i++) {
            paramIndices[i] = new WinDef.LONG(indices[indices.length - i - 1]);
         }

         PointerByReference pbr = new PointerByReference();
         WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayPtrOfIndex(this, paramIndices, pbr);
         COMUtils.checkRC(hr);
         return pbr.getValue();
      }

      public void destroy() {
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayDestroy(this);
         COMUtils.checkRC(res);
      }

      @Override
      public void close() {
         this.destroy();
      }

      public int getLBound(int dimension) {
         int targetDimension = this.getDimensionCount() - dimension;
         WinDef.LONGByReference bound = new WinDef.LONGByReference();
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayGetLBound(this, new WinDef.UINT(targetDimension), bound);
         COMUtils.checkRC(res);
         return bound.getValue().intValue();
      }

      public int getUBound(int dimension) {
         int targetDimension = this.getDimensionCount() - dimension;
         WinDef.LONGByReference bound = new WinDef.LONGByReference();
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayGetUBound(this, new WinDef.UINT(targetDimension), bound);
         COMUtils.checkRC(res);
         return bound.getValue().intValue();
      }

      public int getDimensionCount() {
         return OleAuto.INSTANCE.SafeArrayGetDim(this).intValue();
      }

      public Pointer accessData() {
         PointerByReference pbr = new PointerByReference();
         WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayAccessData(this, pbr);
         COMUtils.checkRC(hr);
         return pbr.getValue();
      }

      public void unaccessData() {
         WinNT.HRESULT hr = OleAuto.INSTANCE.SafeArrayUnaccessData(this);
         COMUtils.checkRC(hr);
      }

      public void lock() {
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayLock(this);
         COMUtils.checkRC(res);
      }

      public void unlock() {
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayUnlock(this);
         COMUtils.checkRC(res);
      }

      public void redim(int cElements, int lLbound) {
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayRedim(this, new OaIdl.SAFEARRAYBOUND(cElements, lLbound));
         COMUtils.checkRC(res);
      }

      public WTypes.VARTYPE getVarType() {
         WTypes.VARTYPEByReference resultHolder = new WTypes.VARTYPEByReference();
         WinNT.HRESULT res = OleAuto.INSTANCE.SafeArrayGetVartype(this, resultHolder);
         COMUtils.checkRC(res);
         return resultHolder.getValue();
      }

      public long getElemsize() {
         return OleAuto.INSTANCE.SafeArrayGetElemsize(this).longValue();
      }

      public static class ByReference extends OaIdl.SAFEARRAY implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"cElements", "lLbound"})
   public static class SAFEARRAYBOUND extends Structure {
      public WinDef.ULONG cElements;
      public WinDef.LONG lLbound;

      public SAFEARRAYBOUND() {
      }

      public SAFEARRAYBOUND(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public SAFEARRAYBOUND(int cElements, int lLbound) {
         this.cElements = new WinDef.ULONG(cElements);
         this.lLbound = new WinDef.LONG(lLbound);
         this.write();
      }

      public static class ByReference extends OaIdl.SAFEARRAYBOUND implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder("pSAFEARRAY")
   public static class SAFEARRAYByReference extends Structure implements Structure.ByReference {
      public OaIdl.SAFEARRAY.ByReference pSAFEARRAY;

      public SAFEARRAYByReference() {
      }

      public SAFEARRAYByReference(Pointer p) {
         super(p);
         this.read();
      }

      public SAFEARRAYByReference(OaIdl.SAFEARRAY.ByReference safeArray) {
         this.pSAFEARRAY = safeArray;
      }
   }

   @Structure.FieldOrder("value")
   public static class SYSKIND extends Structure {
      public int value;
      public static final int SYS_WIN16 = 0;
      public static final int SYS_WIN32 = 1;
      public static final int SYS_MAC = 2;
      public static final int SYS_WIN64 = 3;

      public SYSKIND() {
      }

      public SYSKIND(int value) {
         this.value = value;
      }

      public SYSKIND(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.SYSKIND implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder("scodeArg")
   public static class ScodeArg extends Structure {
      public WinDef.SCODE[] scodeArg = new WinDef.SCODE[]{new WinDef.SCODE()};

      public ScodeArg() {
      }

      public ScodeArg(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.ScodeArg implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"guid", "lcid", "syskind", "wMajorVerNum", "wMinorVerNum", "wLibFlags"})
   public static class TLIBATTR extends Structure {
      public Guid.GUID guid;
      public WinDef.LCID lcid;
      public OaIdl.SYSKIND syskind;
      public WinDef.WORD wMajorVerNum;
      public WinDef.WORD wMinorVerNum;
      public WinDef.WORD wLibFlags;

      public TLIBATTR() {
      }

      public TLIBATTR(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.TLIBATTR implements Structure.ByReference {
         public ByReference() {
         }

         public ByReference(Pointer pointer) {
            super(pointer);
            this.read();
         }
      }
   }

   @Structure.FieldOrder(
      {
            "guid",
            "lcid",
            "dwReserved",
            "memidConstructor",
            "memidDestructor",
            "lpstrSchema",
            "cbSizeInstance",
            "typekind",
            "cFuncs",
            "cVars",
            "cImplTypes",
            "cbSizeVft",
            "cbAlignment",
            "wTypeFlags",
            "wMajorVerNum",
            "wMinorVerNum",
            "tdescAlias",
            "idldescType"
      }
   )
   public static class TYPEATTR extends Structure {
      public Guid.GUID guid;
      public WinDef.LCID lcid;
      public WinDef.DWORD dwReserved;
      public OaIdl.MEMBERID memidConstructor;
      public OaIdl.MEMBERID memidDestructor;
      public WTypes.LPOLESTR lpstrSchema;
      public WinDef.ULONG cbSizeInstance;
      public OaIdl.TYPEKIND typekind;
      public WinDef.WORD cFuncs;
      public WinDef.WORD cVars;
      public WinDef.WORD cImplTypes;
      public WinDef.WORD cbSizeVft;
      public WinDef.WORD cbAlignment;
      public WinDef.WORD wTypeFlags;
      public WinDef.WORD wMajorVerNum;
      public WinDef.WORD wMinorVerNum;
      public OaIdl.TYPEDESC tdescAlias;
      public OaIdl.IDLDESC idldescType;
      public static final int TYPEFLAGS_FAPPOBJECT = 1;
      public static final int TYPEFLAGS_FCANCREATE = 2;
      public static final int TYPEFLAGS_FLICENSED = 4;
      public static final int TYPEFLAGS_FPREDECLID = 8;
      public static final int TYPEFLAGS_FHIDDEN = 16;
      public static final int TYPEFLAGS_FCONTROL = 32;
      public static final int TYPEFLAGS_FDUAL = 64;
      public static final int TYPEFLAGS_FNONEXTENSIBLE = 128;
      public static final int TYPEFLAGS_FOLEAUTOMATION = 256;
      public static final int TYPEFLAGS_FRESTRICTED = 512;
      public static final int TYPEFLAGS_FAGGREGATABLE = 1024;
      public static final int TYPEFLAGS_FREPLACEABLE = 2048;
      public static final int TYPEFLAGS_FDISPATCHABLE = 4096;
      public static final int TYPEFLAGS_FREVERSEBIND = 8192;
      public static final int TYPEFLAGS_FPROXY = 16384;

      public TYPEATTR() {
      }

      public TYPEATTR(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.TYPEATTR implements Structure.ByReference {
      }
   }

   @Structure.FieldOrder({"_typedesc", "vt"})
   public static class TYPEDESC extends Structure {
      public OaIdl.TYPEDESC._TYPEDESC _typedesc;
      public WTypes.VARTYPE vt;

      public TYPEDESC() {
         this.read();
      }

      public TYPEDESC(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public TYPEDESC(OaIdl.TYPEDESC._TYPEDESC _typedesc, WTypes.VARTYPE vt) {
         this._typedesc = _typedesc;
         this.vt = vt;
      }

      public static class ByReference extends OaIdl.TYPEDESC implements Structure.ByReference {
      }

      public static class _TYPEDESC extends Union {
         public OaIdl.TYPEDESC.ByReference lptdesc;
         public OaIdl.ARRAYDESC.ByReference lpadesc;
         public OaIdl.HREFTYPE hreftype;

         public _TYPEDESC() {
            this.setType("hreftype");
            this.read();
         }

         public _TYPEDESC(Pointer pointer) {
            super(pointer);
            this.setType("hreftype");
            this.read();
         }

         public OaIdl.TYPEDESC.ByReference getLptdesc() {
            this.setType("lptdesc");
            this.read();
            return this.lptdesc;
         }

         public OaIdl.ARRAYDESC.ByReference getLpadesc() {
            this.setType("lpadesc");
            this.read();
            return this.lpadesc;
         }

         public OaIdl.HREFTYPE getHreftype() {
            this.setType("hreftype");
            this.read();
            return this.hreftype;
         }
      }
   }

   @Structure.FieldOrder("value")
   public static class TYPEKIND extends Structure {
      public int value;
      public static final int TKIND_ENUM = 0;
      public static final int TKIND_RECORD = 1;
      public static final int TKIND_MODULE = 2;
      public static final int TKIND_INTERFACE = 3;
      public static final int TKIND_DISPATCH = 4;
      public static final int TKIND_COCLASS = 5;
      public static final int TKIND_ALIAS = 6;
      public static final int TKIND_UNION = 7;
      public static final int TKIND_MAX = 8;

      public TYPEKIND() {
      }

      public TYPEKIND(int value) {
         this.value = value;
      }

      public TYPEKIND(Pointer pointer) {
         super(pointer);
         this.read();
      }

      public static class ByReference extends OaIdl.TYPEKIND implements Structure.ByReference {
         public ByReference() {
         }

         public ByReference(int value) {
            super(value);
         }

         public ByReference(OaIdl.TYPEKIND typekind) {
            super(typekind.getPointer());
            this.value = typekind.value;
         }
      }
   }

   @Structure.FieldOrder({"memid", "lpstrSchema", "_vardesc", "elemdescVar", "wVarFlags", "varkind"})
   public static class VARDESC extends Structure {
      public OaIdl.MEMBERID memid;
      public WTypes.LPOLESTR lpstrSchema;
      public OaIdl.VARDESC._VARDESC _vardesc;
      public OaIdl.ELEMDESC elemdescVar;
      public WinDef.WORD wVarFlags;
      public OaIdl.VARKIND varkind;

      public VARDESC() {
      }

      public VARDESC(Pointer pointer) {
         super(pointer);
         this._vardesc.setType("lpvarValue");
         this.read();
      }

      public static class ByReference extends OaIdl.VARDESC implements Structure.ByReference {
      }

      public static class _VARDESC extends Union {
         public NativeLong oInst;
         public Variant.VARIANT.ByReference lpvarValue;

         public _VARDESC() {
            this.setType("lpvarValue");
            this.read();
         }

         public _VARDESC(Pointer pointer) {
            super(pointer);
            this.setType("lpvarValue");
            this.read();
         }

         public _VARDESC(Variant.VARIANT.ByReference lpvarValue) {
            this.lpvarValue = lpvarValue;
            this.setType("lpvarValue");
         }

         public _VARDESC(NativeLong oInst) {
            this.oInst = oInst;
            this.setType("oInst");
         }

         public static class ByReference extends OaIdl.VARDESC._VARDESC implements Structure.ByReference {
         }
      }
   }

   public static class VARIANT_BOOL extends IntegerType {
      private static final long serialVersionUID = 1L;
      public static final int SIZE = 2;

      public VARIANT_BOOL() {
         this(0L);
      }

      public VARIANT_BOOL(long value) {
         super(2, value);
      }

      public VARIANT_BOOL(boolean value) {
         this(value ? 65535L : 0L);
      }

      public boolean booleanValue() {
         return this.shortValue() != 0;
      }
   }

   public static class VARIANT_BOOLByReference extends com.sun.jna.ptr.ByReference {
      public VARIANT_BOOLByReference() {
         this(new OaIdl.VARIANT_BOOL(0L));
      }

      public VARIANT_BOOLByReference(OaIdl.VARIANT_BOOL value) {
         super(2);
         this.setValue(value);
      }

      public void setValue(OaIdl.VARIANT_BOOL value) {
         this.getPointer().setShort(0L, value.shortValue());
      }

      public OaIdl.VARIANT_BOOL getValue() {
         return new OaIdl.VARIANT_BOOL(this.getPointer().getShort(0L));
      }
   }

   @Structure.FieldOrder("value")
   public static class VARKIND extends Structure {
      public static final int VAR_PERINSTANCE = 0;
      public static final int VAR_STATIC = 1;
      public static final int VAR_CONST = 2;
      public static final int VAR_DISPATCH = 3;
      public int value;

      public VARKIND() {
      }

      public VARKIND(int value) {
         this.value = value;
      }

      public static class ByReference extends OaIdl.VARKIND implements Structure.ByReference {
      }
   }

   public static class _VARIANT_BOOL extends OaIdl.VARIANT_BOOL {
      private static final long serialVersionUID = 1L;

      public _VARIANT_BOOL() {
         this(0L);
      }

      public _VARIANT_BOOL(long value) {
         super(value);
      }
   }

   public static class _VARIANT_BOOLByReference extends com.sun.jna.ptr.ByReference {
      public _VARIANT_BOOLByReference() {
         this(new OaIdl.VARIANT_BOOL(0L));
      }

      public _VARIANT_BOOLByReference(OaIdl.VARIANT_BOOL value) {
         super(2);
         this.setValue(value);
      }

      public void setValue(OaIdl.VARIANT_BOOL value) {
         this.getPointer().setShort(0L, value.shortValue());
      }

      public OaIdl.VARIANT_BOOL getValue() {
         return new OaIdl.VARIANT_BOOL(this.getPointer().getShort(0L));
      }
   }
}
