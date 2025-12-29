package com.sun.jna;

public abstract class PointerType implements NativeMapped {
   private Pointer pointer;

   protected PointerType() {
      this.pointer = Pointer.NULL;
   }

   protected PointerType(Pointer p) {
      this.pointer = p;
   }

   @Override
   public Class<?> nativeType() {
      return Pointer.class;
   }

   @Override
   public Object toNative() {
      return this.getPointer();
   }

   public Pointer getPointer() {
      return this.pointer;
   }

   public void setPointer(Pointer p) {
      this.pointer = p;
   }

   @Override
   public Object fromNative(Object nativeValue, FromNativeContext context) {
      if (nativeValue == null) {
         return null;
      } else {
         PointerType pt = Klass.newInstance((Class<PointerType>)this.getClass());
         pt.pointer = (Pointer)nativeValue;
         return pt;
      }
   }

   @Override
   public int hashCode() {
      return this.pointer != null ? this.pointer.hashCode() : 0;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (o instanceof PointerType) {
         Pointer p = ((PointerType)o).getPointer();
         return this.pointer == null ? p == null : this.pointer.equals(p);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return this.pointer == null ? "NULL" : this.pointer.toString() + " (" + super.toString() + ")";
   }
}
