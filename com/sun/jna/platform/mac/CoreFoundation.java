package com.sun.jna.platform.mac;

import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

public interface CoreFoundation extends Library {
   CoreFoundation INSTANCE = Native.load("CoreFoundation", CoreFoundation.class);
   int kCFNotFound = -1;
   int kCFStringEncodingASCII = 1536;
   int kCFStringEncodingUTF8 = 134217984;
   CoreFoundation.CFTypeID ARRAY_TYPE_ID = INSTANCE.CFArrayGetTypeID();
   CoreFoundation.CFTypeID BOOLEAN_TYPE_ID = INSTANCE.CFBooleanGetTypeID();
   CoreFoundation.CFTypeID DATA_TYPE_ID = INSTANCE.CFDataGetTypeID();
   CoreFoundation.CFTypeID DATE_TYPE_ID = INSTANCE.CFDateGetTypeID();
   CoreFoundation.CFTypeID DICTIONARY_TYPE_ID = INSTANCE.CFDictionaryGetTypeID();
   CoreFoundation.CFTypeID NUMBER_TYPE_ID = INSTANCE.CFNumberGetTypeID();
   CoreFoundation.CFTypeID STRING_TYPE_ID = INSTANCE.CFStringGetTypeID();

   CoreFoundation.CFStringRef CFStringCreateWithCharacters(CoreFoundation.CFAllocatorRef var1, char[] var2, CoreFoundation.CFIndex var3);

   CoreFoundation.CFNumberRef CFNumberCreate(CoreFoundation.CFAllocatorRef var1, CoreFoundation.CFIndex var2, com.sun.jna.ptr.ByReference var3);

   CoreFoundation.CFArrayRef CFArrayCreate(CoreFoundation.CFAllocatorRef var1, Pointer var2, CoreFoundation.CFIndex var3, Pointer var4);

   CoreFoundation.CFDataRef CFDataCreate(CoreFoundation.CFAllocatorRef var1, Pointer var2, CoreFoundation.CFIndex var3);

   CoreFoundation.CFMutableDictionaryRef CFDictionaryCreateMutable(CoreFoundation.CFAllocatorRef var1, CoreFoundation.CFIndex var2, Pointer var3, Pointer var4);

   CoreFoundation.CFStringRef CFCopyDescription(CoreFoundation.CFTypeRef var1);

   void CFRelease(CoreFoundation.CFTypeRef var1);

   CoreFoundation.CFTypeRef CFRetain(CoreFoundation.CFTypeRef var1);

   CoreFoundation.CFIndex CFGetRetainCount(CoreFoundation.CFTypeRef var1);

   CoreFoundation.CFIndex CFDictionaryGetCount(CoreFoundation.CFDictionaryRef var1);

   Pointer CFDictionaryGetValue(CoreFoundation.CFDictionaryRef var1, PointerType var2);

   byte CFDictionaryGetValueIfPresent(CoreFoundation.CFDictionaryRef var1, PointerType var2, PointerByReference var3);

   void CFDictionarySetValue(CoreFoundation.CFMutableDictionaryRef var1, PointerType var2, PointerType var3);

   byte CFStringGetCString(CoreFoundation.CFStringRef var1, Pointer var2, CoreFoundation.CFIndex var3, int var4);

   byte CFBooleanGetValue(CoreFoundation.CFBooleanRef var1);

   CoreFoundation.CFIndex CFArrayGetCount(CoreFoundation.CFArrayRef var1);

   Pointer CFArrayGetValueAtIndex(CoreFoundation.CFArrayRef var1, CoreFoundation.CFIndex var2);

   CoreFoundation.CFIndex CFNumberGetType(CoreFoundation.CFNumberRef var1);

   byte CFNumberGetValue(CoreFoundation.CFNumberRef var1, CoreFoundation.CFIndex var2, com.sun.jna.ptr.ByReference var3);

   CoreFoundation.CFIndex CFStringGetLength(CoreFoundation.CFStringRef var1);

   CoreFoundation.CFIndex CFStringGetMaximumSizeForEncoding(CoreFoundation.CFIndex var1, int var2);

   boolean CFEqual(CoreFoundation.CFTypeRef var1, CoreFoundation.CFTypeRef var2);

   CoreFoundation.CFAllocatorRef CFAllocatorGetDefault();

   CoreFoundation.CFIndex CFDataGetLength(CoreFoundation.CFDataRef var1);

   Pointer CFDataGetBytePtr(CoreFoundation.CFDataRef var1);

   CoreFoundation.CFTypeID CFGetTypeID(CoreFoundation.CFTypeRef var1);

   CoreFoundation.CFTypeID CFGetTypeID(Pointer var1);

   CoreFoundation.CFTypeID CFArrayGetTypeID();

   CoreFoundation.CFTypeID CFBooleanGetTypeID();

   CoreFoundation.CFTypeID CFDateGetTypeID();

   CoreFoundation.CFTypeID CFDataGetTypeID();

   CoreFoundation.CFTypeID CFDictionaryGetTypeID();

   CoreFoundation.CFTypeID CFNumberGetTypeID();

   CoreFoundation.CFTypeID CFStringGetTypeID();

   public static class CFAllocatorRef extends CoreFoundation.CFTypeRef {
   }

   public static class CFArrayRef extends CoreFoundation.CFTypeRef {
      public CFArrayRef() {
      }

      public CFArrayRef(Pointer p) {
         super(p);
         if (!this.isTypeID(CoreFoundation.ARRAY_TYPE_ID)) {
            throw new ClassCastException("Unable to cast to CFArray. Type ID: " + this.getTypeID());
         }
      }

      public int getCount() {
         return CoreFoundation.INSTANCE.CFArrayGetCount(this).intValue();
      }

      public Pointer getValueAtIndex(int idx) {
         return CoreFoundation.INSTANCE.CFArrayGetValueAtIndex(this, new CoreFoundation.CFIndex(idx));
      }
   }

   public static class CFBooleanRef extends CoreFoundation.CFTypeRef {
      public CFBooleanRef() {
      }

      public CFBooleanRef(Pointer p) {
         super(p);
         if (!this.isTypeID(CoreFoundation.BOOLEAN_TYPE_ID)) {
            throw new ClassCastException("Unable to cast to CFBoolean. Type ID: " + this.getTypeID());
         }
      }

      public boolean booleanValue() {
         return 0 != CoreFoundation.INSTANCE.CFBooleanGetValue(this);
      }
   }

   public static class CFDataRef extends CoreFoundation.CFTypeRef {
      public CFDataRef() {
      }

      public CFDataRef(Pointer p) {
         super(p);
         if (!this.isTypeID(CoreFoundation.DATA_TYPE_ID)) {
            throw new ClassCastException("Unable to cast to CFData. Type ID: " + this.getTypeID());
         }
      }

      public int getLength() {
         return CoreFoundation.INSTANCE.CFDataGetLength(this).intValue();
      }

      public Pointer getBytePtr() {
         return CoreFoundation.INSTANCE.CFDataGetBytePtr(this);
      }
   }

   public static class CFDictionaryRef extends CoreFoundation.CFTypeRef {
      public CFDictionaryRef() {
      }

      public CFDictionaryRef(Pointer p) {
         super(p);
         if (!this.isTypeID(CoreFoundation.DICTIONARY_TYPE_ID)) {
            throw new ClassCastException("Unable to cast to CFDictionary. Type ID: " + this.getTypeID());
         }
      }

      public Pointer getValue(PointerType key) {
         return CoreFoundation.INSTANCE.CFDictionaryGetValue(this, key);
      }

      public long getCount() {
         return CoreFoundation.INSTANCE.CFDictionaryGetCount(this).longValue();
      }

      public boolean getValueIfPresent(PointerType key, PointerByReference value) {
         return CoreFoundation.INSTANCE.CFDictionaryGetValueIfPresent(this, key, value) > 0;
      }

      public static class ByReference extends PointerByReference {
         public ByReference() {
            this(null);
         }

         public ByReference(CoreFoundation.CFDictionaryRef value) {
            super(value != null ? value.getPointer() : null);
         }

         @Override
         public void setValue(Pointer value) {
            if (value != null) {
               CoreFoundation.CFTypeID typeId = CoreFoundation.INSTANCE.CFGetTypeID(value);
               if (!CoreFoundation.DICTIONARY_TYPE_ID.equals(typeId)) {
                  throw new ClassCastException("Unable to cast to CFDictionary. Type ID: " + typeId);
               }
            }

            super.setValue(value);
         }

         public CoreFoundation.CFDictionaryRef getDictionaryRefValue() {
            Pointer value = super.getValue();
            return value == null ? null : new CoreFoundation.CFDictionaryRef(value);
         }
      }
   }

   public static class CFIndex extends NativeLong {
      private static final long serialVersionUID = 1L;

      public CFIndex() {
      }

      public CFIndex(long value) {
         super(value);
      }
   }

   public static class CFMutableDictionaryRef extends CoreFoundation.CFDictionaryRef {
      public CFMutableDictionaryRef() {
      }

      public CFMutableDictionaryRef(Pointer p) {
         super(p);
      }

      public void setValue(PointerType key, PointerType value) {
         CoreFoundation.INSTANCE.CFDictionarySetValue(this, key, value);
      }
   }

   public static class CFNumberRef extends CoreFoundation.CFTypeRef {
      public CFNumberRef() {
      }

      public CFNumberRef(Pointer p) {
         super(p);
         if (!this.isTypeID(CoreFoundation.NUMBER_TYPE_ID)) {
            throw new ClassCastException("Unable to cast to CFNumber. Type ID: " + this.getTypeID());
         }
      }

      public long longValue() {
         LongByReference lbr = new LongByReference();
         CoreFoundation.INSTANCE.CFNumberGetValue(this, CoreFoundation.CFNumberType.kCFNumberLongLongType.typeIndex(), lbr);
         return lbr.getValue();
      }

      public int intValue() {
         IntByReference ibr = new IntByReference();
         CoreFoundation.INSTANCE.CFNumberGetValue(this, CoreFoundation.CFNumberType.kCFNumberIntType.typeIndex(), ibr);
         return ibr.getValue();
      }

      public short shortValue() {
         ShortByReference sbr = new ShortByReference();
         CoreFoundation.INSTANCE.CFNumberGetValue(this, CoreFoundation.CFNumberType.kCFNumberShortType.typeIndex(), sbr);
         return sbr.getValue();
      }

      public byte byteValue() {
         ByteByReference bbr = new ByteByReference();
         CoreFoundation.INSTANCE.CFNumberGetValue(this, CoreFoundation.CFNumberType.kCFNumberCharType.typeIndex(), bbr);
         return bbr.getValue();
      }

      public double doubleValue() {
         DoubleByReference dbr = new DoubleByReference();
         CoreFoundation.INSTANCE.CFNumberGetValue(this, CoreFoundation.CFNumberType.kCFNumberDoubleType.typeIndex(), dbr);
         return dbr.getValue();
      }

      public float floatValue() {
         FloatByReference fbr = new FloatByReference();
         CoreFoundation.INSTANCE.CFNumberGetValue(this, CoreFoundation.CFNumberType.kCFNumberFloatType.typeIndex(), fbr);
         return fbr.getValue();
      }
   }

   public static enum CFNumberType {
      unusedZero,
      kCFNumberSInt8Type,
      kCFNumberSInt16Type,
      kCFNumberSInt32Type,
      kCFNumberSInt64Type,
      kCFNumberFloat32Type,
      kCFNumberFloat64Type,
      kCFNumberCharType,
      kCFNumberShortType,
      kCFNumberIntType,
      kCFNumberLongType,
      kCFNumberLongLongType,
      kCFNumberFloatType,
      kCFNumberDoubleType,
      kCFNumberCFIndexType,
      kCFNumberNSIntegerType,
      kCFNumberCGFloatType,
      kCFNumberMaxType;

      public CoreFoundation.CFIndex typeIndex() {
         return new CoreFoundation.CFIndex(this.ordinal());
      }
   }

   public static class CFStringRef extends CoreFoundation.CFTypeRef {
      public CFStringRef() {
      }

      public CFStringRef(Pointer p) {
         super(p);
         if (!this.isTypeID(CoreFoundation.STRING_TYPE_ID)) {
            throw new ClassCastException("Unable to cast to CFString. Type ID: " + this.getTypeID());
         }
      }

      public static CoreFoundation.CFStringRef createCFString(String s) {
         char[] chars = s.toCharArray();
         return CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null, chars, new CoreFoundation.CFIndex(chars.length));
      }

      public String stringValue() {
         CoreFoundation.CFIndex length = CoreFoundation.INSTANCE.CFStringGetLength(this);
         if (length.longValue() == 0L) {
            return "";
         } else {
            CoreFoundation.CFIndex maxSize = CoreFoundation.INSTANCE.CFStringGetMaximumSizeForEncoding(length, 134217984);
            if (maxSize.intValue() == -1) {
               throw new StringIndexOutOfBoundsException("CFString maximum number of bytes exceeds LONG_MAX.");
            } else {
               maxSize.setValue(maxSize.longValue() + 1L);
               Memory buf = new Memory(maxSize.longValue());
               if (0 != CoreFoundation.INSTANCE.CFStringGetCString(this, buf, maxSize, 134217984)) {
                  return buf.getString(0L, "UTF8");
               } else {
                  throw new IllegalArgumentException("CFString conversion fails or the provided buffer is too small.");
               }
            }
         }
      }

      public static class ByReference extends PointerByReference {
         public ByReference() {
            this(null);
         }

         public ByReference(CoreFoundation.CFStringRef value) {
            super(value != null ? value.getPointer() : null);
         }

         @Override
         public void setValue(Pointer value) {
            if (value != null) {
               CoreFoundation.CFTypeID typeId = CoreFoundation.INSTANCE.CFGetTypeID(value);
               if (!CoreFoundation.STRING_TYPE_ID.equals(typeId)) {
                  throw new ClassCastException("Unable to cast to CFString. Type ID: " + typeId);
               }
            }

            super.setValue(value);
         }

         public CoreFoundation.CFStringRef getStringRefValue() {
            Pointer value = super.getValue();
            return value == null ? null : new CoreFoundation.CFStringRef(value);
         }
      }
   }

   public static class CFTypeID extends NativeLong {
      private static final long serialVersionUID = 1L;

      public CFTypeID() {
      }

      public CFTypeID(long value) {
         super(value);
      }

      @Override
      public String toString() {
         if (this.equals(CoreFoundation.ARRAY_TYPE_ID)) {
            return "CFArray";
         } else if (this.equals(CoreFoundation.BOOLEAN_TYPE_ID)) {
            return "CFBoolean";
         } else if (this.equals(CoreFoundation.DATA_TYPE_ID)) {
            return "CFData";
         } else if (this.equals(CoreFoundation.DATE_TYPE_ID)) {
            return "CFDate";
         } else if (this.equals(CoreFoundation.DICTIONARY_TYPE_ID)) {
            return "CFDictionary";
         } else if (this.equals(CoreFoundation.NUMBER_TYPE_ID)) {
            return "CFNumber";
         } else {
            return this.equals(CoreFoundation.STRING_TYPE_ID) ? "CFString" : super.toString();
         }
      }
   }

   public static class CFTypeRef extends PointerType {
      public CFTypeRef() {
      }

      public CFTypeRef(Pointer p) {
         super(p);
      }

      public CoreFoundation.CFTypeID getTypeID() {
         return this.getPointer() == null ? new CoreFoundation.CFTypeID(0L) : CoreFoundation.INSTANCE.CFGetTypeID(this);
      }

      public boolean isTypeID(CoreFoundation.CFTypeID typeID) {
         return this.getTypeID().equals(typeID);
      }

      public void retain() {
         CoreFoundation.INSTANCE.CFRetain(this);
      }

      public void release() {
         CoreFoundation.INSTANCE.CFRelease(this);
      }
   }
}
