package com.sun.jna.platform.win32;

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import java.awt.Rectangle;

public interface WinDef {
   int MAX_PATH = 260;

   public static class ATOM extends WinDef.WORD {
      public ATOM() {
         this(0L);
      }

      public ATOM(long value) {
         super(value);
      }
   }

   public static class BOOL extends IntegerType implements Comparable<WinDef.BOOL> {
      public static final int SIZE = 4;

      public BOOL() {
         this(0L);
      }

      public BOOL(boolean value) {
         this(value ? 1L : 0L);
      }

      public BOOL(long value) {
         super(4, value, false);

         assert value == 0L || value == 1L;
      }

      public boolean booleanValue() {
         return this.intValue() > 0;
      }

      @Override
      public String toString() {
         return Boolean.toString(this.booleanValue());
      }

      public int compareTo(WinDef.BOOL other) {
         return compare(this, other);
      }

      public static int compare(WinDef.BOOL v1, WinDef.BOOL v2) {
         if (v1 == v2) {
            return 0;
         } else if (v1 == null) {
            return 1;
         } else {
            return v2 == null ? -1 : compare(v1.booleanValue(), v2.booleanValue());
         }
      }

      public static int compare(WinDef.BOOL v1, boolean v2) {
         return v1 == null ? 1 : compare(v1.booleanValue(), v2);
      }

      public static int compare(boolean v1, boolean v2) {
         if (v1 == v2) {
            return 0;
         } else {
            return v1 ? 1 : -1;
         }
      }
   }

   public static class BOOLByReference extends com.sun.jna.ptr.ByReference {
      public BOOLByReference() {
         this(new WinDef.BOOL(0L));
      }

      public BOOLByReference(WinDef.BOOL value) {
         super(4);
         this.setValue(value);
      }

      public void setValue(WinDef.BOOL value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public WinDef.BOOL getValue() {
         return new WinDef.BOOL(this.getPointer().getInt(0L));
      }
   }

   public static class BYTE extends WinDef.UCHAR {
      public BYTE() {
         this(0L);
      }

      public BYTE(long value) {
         super(value);
      }
   }

   public static class CHAR extends IntegerType implements Comparable<WinDef.CHAR> {
      public static final int SIZE = 1;

      public CHAR() {
         this(0L);
      }

      public CHAR(byte ch) {
         this((long)(ch & 255));
      }

      public CHAR(long value) {
         super(1, value, false);
      }

      public int compareTo(WinDef.CHAR other) {
         return compare(this, other);
      }
   }

   public static class CHARByReference extends com.sun.jna.ptr.ByReference {
      public CHARByReference() {
         this(new WinDef.CHAR(0L));
      }

      public CHARByReference(WinDef.CHAR value) {
         super(1);
         this.setValue(value);
      }

      public void setValue(WinDef.CHAR value) {
         this.getPointer().setByte(0L, value.byteValue());
      }

      public WinDef.CHAR getValue() {
         return new WinDef.CHAR(this.getPointer().getByte(0L));
      }
   }

   public static class DWORD extends IntegerType implements Comparable<WinDef.DWORD> {
      public static final int SIZE = 4;

      public DWORD() {
         this(0L);
      }

      public DWORD(long value) {
         super(4, value, true);
      }

      public WinDef.WORD getLow() {
         return new WinDef.WORD(this.longValue() & 65535L);
      }

      public WinDef.WORD getHigh() {
         return new WinDef.WORD(this.longValue() >> 16 & 65535L);
      }

      public int compareTo(WinDef.DWORD other) {
         return compare(this, other);
      }
   }

   public static class DWORDByReference extends com.sun.jna.ptr.ByReference {
      public DWORDByReference() {
         this(new WinDef.DWORD(0L));
      }

      public DWORDByReference(WinDef.DWORD value) {
         super(4);
         this.setValue(value);
      }

      public void setValue(WinDef.DWORD value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public WinDef.DWORD getValue() {
         return new WinDef.DWORD(this.getPointer().getInt(0L));
      }
   }

   public static class DWORDLONG extends IntegerType implements Comparable<WinDef.DWORDLONG> {
      public static final int SIZE = 8;

      public DWORDLONG() {
         this(0L);
      }

      public DWORDLONG(long value) {
         super(8, value, true);
      }

      public int compareTo(WinDef.DWORDLONG other) {
         return compare(this, other);
      }
   }

   public static class HBITMAP extends WinNT.HANDLE {
      public HBITMAP() {
      }

      public HBITMAP(Pointer p) {
         super(p);
      }
   }

   public static class HBRUSH extends WinNT.HANDLE {
      public HBRUSH() {
      }

      public HBRUSH(Pointer p) {
         super(p);
      }
   }

   public static class HCURSOR extends WinDef.HICON {
      public HCURSOR() {
      }

      public HCURSOR(Pointer p) {
         super(p);
      }
   }

   public static class HDC extends WinNT.HANDLE {
      public HDC() {
      }

      public HDC(Pointer p) {
         super(p);
      }
   }

   public static class HFONT extends WinNT.HANDLE {
      public HFONT() {
      }

      public HFONT(Pointer p) {
         super(p);
      }
   }

   public static class HGLRC extends WinNT.HANDLE {
      public HGLRC() {
      }

      public HGLRC(Pointer p) {
         super(p);
      }
   }

   public static class HGLRCByReference extends WinNT.HANDLEByReference {
      public HGLRCByReference() {
      }

      public HGLRCByReference(WinDef.HGLRC h) {
         super(h);
      }
   }

   public static class HICON extends WinNT.HANDLE {
      public HICON() {
      }

      public HICON(WinNT.HANDLE handle) {
         this(handle.getPointer());
      }

      public HICON(Pointer p) {
         super(p);
      }
   }

   public static class HINSTANCE extends WinNT.HANDLE {
   }

   public static class HKL extends WinNT.HANDLE {
      public HKL() {
      }

      public HKL(Pointer p) {
         super(p);
      }

      public HKL(int i) {
         super(Pointer.createConstant(i));
      }

      public int getLanguageIdentifier() {
         return (int)(Pointer.nativeValue(this.getPointer()) & 65535L);
      }

      public int getDeviceHandle() {
         return (int)(Pointer.nativeValue(this.getPointer()) >> 16 & 65535L);
      }

      @Override
      public String toString() {
         return String.format("%08x", Pointer.nativeValue(this.getPointer()));
      }
   }

   public static class HMENU extends WinNT.HANDLE {
      public HMENU() {
      }

      public HMENU(Pointer p) {
         super(p);
      }
   }

   public static class HMODULE extends WinDef.HINSTANCE {
   }

   public static class HPALETTE extends WinNT.HANDLE {
      public HPALETTE() {
      }

      public HPALETTE(Pointer p) {
         super(p);
      }
   }

   public static class HPEN extends WinNT.HANDLE {
      public HPEN() {
      }

      public HPEN(Pointer p) {
         super(p);
      }
   }

   public static class HRGN extends WinNT.HANDLE {
      public HRGN() {
      }

      public HRGN(Pointer p) {
         super(p);
      }
   }

   public static class HRSRC extends WinNT.HANDLE {
      public HRSRC() {
      }

      public HRSRC(Pointer p) {
         super(p);
      }
   }

   public static class HWND extends WinNT.HANDLE {
      public HWND() {
      }

      public HWND(Pointer p) {
         super(p);
      }
   }

   public static class INT_PTR extends IntegerType {
      public INT_PTR() {
         super(Native.POINTER_SIZE);
      }

      public INT_PTR(long value) {
         super(Native.POINTER_SIZE, value);
      }

      public Pointer toPointer() {
         return Pointer.createConstant(this.longValue());
      }
   }

   public static class LCID extends WinDef.DWORD {
      public LCID() {
         super(0L);
      }

      public LCID(long value) {
         super(value);
      }
   }

   public static class LONG extends IntegerType implements Comparable<WinDef.LONG> {
      public static final int SIZE = Native.LONG_SIZE;

      public LONG() {
         this(0L);
      }

      public LONG(long value) {
         super(SIZE, value);
      }

      public int compareTo(WinDef.LONG other) {
         return compare(this, other);
      }
   }

   public static class LONGByReference extends com.sun.jna.ptr.ByReference {
      public LONGByReference() {
         this(new WinDef.LONG(0L));
      }

      public LONGByReference(WinDef.LONG value) {
         super(WinDef.LONG.SIZE);
         this.setValue(value);
      }

      public void setValue(WinDef.LONG value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public WinDef.LONG getValue() {
         return new WinDef.LONG(this.getPointer().getInt(0L));
      }
   }

   public static class LONGLONG extends IntegerType implements Comparable<WinDef.LONGLONG> {
      public static final int SIZE = Native.LONG_SIZE * 2;

      public LONGLONG() {
         this(0L);
      }

      public LONGLONG(long value) {
         super(8, value, false);
      }

      public int compareTo(WinDef.LONGLONG other) {
         return compare(this, other);
      }
   }

   public static class LONGLONGByReference extends com.sun.jna.ptr.ByReference {
      public LONGLONGByReference() {
         this(new WinDef.LONGLONG(0L));
      }

      public LONGLONGByReference(WinDef.LONGLONG value) {
         super(WinDef.LONGLONG.SIZE);
         this.setValue(value);
      }

      public void setValue(WinDef.LONGLONG value) {
         this.getPointer().setLong(0L, value.longValue());
      }

      public WinDef.LONGLONG getValue() {
         return new WinDef.LONGLONG(this.getPointer().getLong(0L));
      }
   }

   public static class LPARAM extends BaseTSD.LONG_PTR {
      public LPARAM() {
         this(0L);
      }

      public LPARAM(long value) {
         super(value);
      }
   }

   public static class LPVOID extends PointerType {
      public LPVOID() {
      }

      public LPVOID(Pointer p) {
         super(p);
      }
   }

   public static class LRESULT extends BaseTSD.LONG_PTR {
      public LRESULT() {
         this(0L);
      }

      public LRESULT(long value) {
         super(value);
      }
   }

   @Structure.FieldOrder({"x", "y"})
   public static class POINT extends Structure {
      public int x;
      public int y;

      public POINT() {
      }

      public POINT(Pointer memory) {
         super(memory);
         this.read();
      }

      public POINT(int x, int y) {
         this.x = x;
         this.y = y;
      }

      public static class ByReference extends WinDef.POINT implements Structure.ByReference {
         public ByReference() {
         }

         public ByReference(Pointer memory) {
            super(memory);
         }

         public ByReference(int x, int y) {
            super(x, y);
         }
      }

      public static class ByValue extends WinDef.POINT implements Structure.ByValue {
         public ByValue() {
         }

         public ByValue(Pointer memory) {
            super(memory);
         }

         public ByValue(int x, int y) {
            super(x, y);
         }
      }
   }

   public static class PVOID extends PointerType {
      public PVOID() {
      }

      public PVOID(Pointer pointer) {
         super(pointer);
      }
   }

   @Structure.FieldOrder({"left", "top", "right", "bottom"})
   public static class RECT extends Structure {
      public int left;
      public int top;
      public int right;
      public int bottom;

      public Rectangle toRectangle() {
         return new Rectangle(this.left, this.top, this.right - this.left, this.bottom - this.top);
      }

      @Override
      public String toString() {
         return "[(" + this.left + "," + this.top + ")(" + this.right + "," + this.bottom + ")]";
      }
   }

   public static class SCODE extends WinDef.ULONG {
      public SCODE() {
         this(0L);
      }

      public SCODE(long value) {
         super(value);
      }
   }

   public static class SCODEByReference extends com.sun.jna.ptr.ByReference {
      public SCODEByReference() {
         this(new WinDef.SCODE(0L));
      }

      public SCODEByReference(WinDef.SCODE value) {
         super(WinDef.SCODE.SIZE);
         this.setValue(value);
      }

      public void setValue(WinDef.SCODE value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public WinDef.SCODE getValue() {
         return new WinDef.SCODE(this.getPointer().getInt(0L));
      }
   }

   public static class SHORT extends IntegerType implements Comparable<WinDef.SHORT> {
      public static final int SIZE = 2;

      public SHORT() {
         this(0L);
      }

      public SHORT(long value) {
         super(2, value, false);
      }

      public int compareTo(WinDef.SHORT other) {
         return compare(this, other);
      }
   }

   public static class UCHAR extends IntegerType implements Comparable<WinDef.UCHAR> {
      public static final int SIZE = 1;

      public UCHAR() {
         this(0L);
      }

      public UCHAR(char ch) {
         this((long)(ch & 255));
      }

      public UCHAR(long value) {
         super(1, value, true);
      }

      public int compareTo(WinDef.UCHAR other) {
         return compare(this, other);
      }
   }

   public static class UINT extends IntegerType implements Comparable<WinDef.UINT> {
      public static final int SIZE = 4;

      public UINT() {
         this(0L);
      }

      public UINT(long value) {
         super(4, value, true);
      }

      public int compareTo(WinDef.UINT other) {
         return compare(this, other);
      }
   }

   public static class UINTByReference extends com.sun.jna.ptr.ByReference {
      public UINTByReference() {
         this(new WinDef.UINT(0L));
      }

      public UINTByReference(WinDef.UINT value) {
         super(4);
         this.setValue(value);
      }

      public void setValue(WinDef.UINT value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public WinDef.UINT getValue() {
         return new WinDef.UINT(this.getPointer().getInt(0L));
      }
   }

   public static class UINT_PTR extends IntegerType {
      public UINT_PTR() {
         super(Native.POINTER_SIZE);
      }

      public UINT_PTR(long value) {
         super(Native.POINTER_SIZE, value, true);
      }

      public Pointer toPointer() {
         return Pointer.createConstant(this.longValue());
      }
   }

   public static class ULONG extends IntegerType implements Comparable<WinDef.ULONG> {
      public static final int SIZE = Native.LONG_SIZE;

      public ULONG() {
         this(0L);
      }

      public ULONG(long value) {
         super(SIZE, value, true);
      }

      public int compareTo(WinDef.ULONG other) {
         return compare(this, other);
      }
   }

   public static class ULONGByReference extends com.sun.jna.ptr.ByReference {
      public ULONGByReference() {
         this(new WinDef.ULONG(0L));
      }

      public ULONGByReference(WinDef.ULONG value) {
         super(WinDef.ULONG.SIZE);
         this.setValue(value);
      }

      public void setValue(WinDef.ULONG value) {
         this.getPointer().setInt(0L, value.intValue());
      }

      public WinDef.ULONG getValue() {
         return new WinDef.ULONG(this.getPointer().getInt(0L));
      }
   }

   public static class ULONGLONG extends IntegerType implements Comparable<WinDef.ULONGLONG> {
      public static final int SIZE = 8;

      public ULONGLONG() {
         this(0L);
      }

      public ULONGLONG(long value) {
         super(8, value, true);
      }

      public int compareTo(WinDef.ULONGLONG other) {
         return compare(this, other);
      }
   }

   public static class ULONGLONGByReference extends com.sun.jna.ptr.ByReference {
      public ULONGLONGByReference() {
         this(new WinDef.ULONGLONG(0L));
      }

      public ULONGLONGByReference(WinDef.ULONGLONG value) {
         super(8);
         this.setValue(value);
      }

      public void setValue(WinDef.ULONGLONG value) {
         this.getPointer().setLong(0L, value.longValue());
      }

      public WinDef.ULONGLONG getValue() {
         return new WinDef.ULONGLONG(this.getPointer().getLong(0L));
      }
   }

   public static class USHORT extends IntegerType implements Comparable<WinDef.USHORT> {
      public static final int SIZE = 2;

      public USHORT() {
         this(0L);
      }

      public USHORT(long value) {
         super(2, value, true);
      }

      public int compareTo(WinDef.USHORT other) {
         return compare(this, other);
      }
   }

   public static class USHORTByReference extends com.sun.jna.ptr.ByReference {
      public USHORTByReference() {
         this(new WinDef.USHORT(0L));
      }

      public USHORTByReference(WinDef.USHORT value) {
         super(2);
         this.setValue(value);
      }

      public USHORTByReference(short value) {
         super(2);
         this.setValue(new WinDef.USHORT(value));
      }

      public void setValue(WinDef.USHORT value) {
         this.getPointer().setShort(0L, value.shortValue());
      }

      public WinDef.USHORT getValue() {
         return new WinDef.USHORT(this.getPointer().getShort(0L));
      }
   }

   public static class WORD extends IntegerType implements Comparable<WinDef.WORD> {
      public static final int SIZE = 2;

      public WORD() {
         this(0L);
      }

      public WORD(long value) {
         super(2, value, true);
      }

      public int compareTo(WinDef.WORD other) {
         return compare(this, other);
      }
   }

   public static class WORDByReference extends com.sun.jna.ptr.ByReference {
      public WORDByReference() {
         this(new WinDef.WORD(0L));
      }

      public WORDByReference(WinDef.WORD value) {
         super(2);
         this.setValue(value);
      }

      public void setValue(WinDef.WORD value) {
         this.getPointer().setShort(0L, value.shortValue());
      }

      public WinDef.WORD getValue() {
         return new WinDef.WORD(this.getPointer().getShort(0L));
      }
   }

   public static class WPARAM extends WinDef.UINT_PTR {
      public WPARAM() {
         this(0L);
      }

      public WPARAM(long value) {
         super(value);
      }
   }
}
