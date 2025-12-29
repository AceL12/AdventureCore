package com.sun.jna;

import com.sun.jna.internal.Cleaner;
import java.io.Closeable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Memory extends Pointer implements Closeable {
   private static final Map<Long, Reference<Memory>> allocatedMemory = new ConcurrentHashMap<>();
   private static final WeakMemoryHolder buffers = new WeakMemoryHolder();
   private final Cleaner.Cleanable cleanable;
   protected long size;

   public static void purge() {
      buffers.clean();
   }

   public static void disposeAll() {
      for (Reference<Memory> r : new ArrayList<>(allocatedMemory.values())) {
         Memory m = r.get();
         if (m != null) {
            m.close();
         }
      }
   }

   public Memory(long size) {
      this.size = size;
      if (size <= 0L) {
         throw new IllegalArgumentException("Allocation size must be greater than zero");
      } else {
         this.peer = malloc(size);
         if (this.peer == 0L) {
            throw new OutOfMemoryError("Cannot allocate " + size + " bytes");
         } else {
            allocatedMemory.put(this.peer, new WeakReference<>(this));
            this.cleanable = Cleaner.getCleaner().register(this, new Memory.MemoryDisposer(this.peer));
         }
      }
   }

   protected Memory() {
      this.cleanable = null;
   }

   @Override
   public Pointer share(long offset) {
      return this.share(offset, this.size() - offset);
   }

   @Override
   public Pointer share(long offset, long sz) {
      this.boundsCheck(offset, sz);
      return new Memory.SharedMemory(offset, sz);
   }

   public Memory align(int byteBoundary) {
      if (byteBoundary <= 0) {
         throw new IllegalArgumentException("Byte boundary must be positive: " + byteBoundary);
      } else {
         for (int i = 0; i < 32; i++) {
            if (byteBoundary == 1 << i) {
               long mask = ~(byteBoundary - 1L);
               if ((this.peer & mask) != this.peer) {
                  long newPeer = this.peer + byteBoundary - 1L & mask;
                  long newSize = this.peer + this.size - newPeer;
                  if (newSize <= 0L) {
                     throw new IllegalArgumentException("Insufficient memory to align to the requested boundary");
                  }

                  return (Memory)this.share(newPeer - this.peer, newSize);
               }

               return this;
            }
         }

         throw new IllegalArgumentException("Byte boundary must be a power of two");
      }
   }

   @Override
   public void close() {
      this.peer = 0L;
      if (this.cleanable != null) {
         this.cleanable.clean();
      }
   }

   @Deprecated
   protected void dispose() {
      this.close();
   }

   public void clear() {
      this.clear(this.size);
   }

   public boolean valid() {
      return this.peer != 0L;
   }

   public long size() {
      return this.size;
   }

   protected void boundsCheck(long off, long sz) {
      if (off < 0L) {
         throw new IndexOutOfBoundsException("Invalid offset: " + off);
      } else if (off + sz > this.size) {
         String msg = "Bounds exceeds available space : size=" + this.size + ", offset=" + (off + sz);
         throw new IndexOutOfBoundsException(msg);
      }
   }

   @Override
   public void read(long bOff, byte[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 1L);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, short[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 2L);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, char[] buf, int index, int length) {
      this.boundsCheck(bOff, length * Native.WCHAR_SIZE);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, int[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 4L);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, long[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 8L);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, float[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 4L);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, double[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 8L);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void read(long bOff, Pointer[] buf, int index, int length) {
      this.boundsCheck(bOff, length * Native.POINTER_SIZE);
      super.read(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, byte[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 1L);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, short[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 2L);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, char[] buf, int index, int length) {
      this.boundsCheck(bOff, length * Native.WCHAR_SIZE);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, int[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 4L);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, long[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 8L);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, float[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 4L);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, double[] buf, int index, int length) {
      this.boundsCheck(bOff, length * 8L);
      super.write(bOff, buf, index, length);
   }

   @Override
   public void write(long bOff, Pointer[] buf, int index, int length) {
      this.boundsCheck(bOff, length * Native.POINTER_SIZE);
      super.write(bOff, buf, index, length);
   }

   @Override
   public byte getByte(long offset) {
      this.boundsCheck(offset, 1L);
      return super.getByte(offset);
   }

   @Override
   public char getChar(long offset) {
      this.boundsCheck(offset, Native.WCHAR_SIZE);
      return super.getChar(offset);
   }

   @Override
   public short getShort(long offset) {
      this.boundsCheck(offset, 2L);
      return super.getShort(offset);
   }

   @Override
   public int getInt(long offset) {
      this.boundsCheck(offset, 4L);
      return super.getInt(offset);
   }

   @Override
   public long getLong(long offset) {
      this.boundsCheck(offset, 8L);
      return super.getLong(offset);
   }

   @Override
   public float getFloat(long offset) {
      this.boundsCheck(offset, 4L);
      return super.getFloat(offset);
   }

   @Override
   public double getDouble(long offset) {
      this.boundsCheck(offset, 8L);
      return super.getDouble(offset);
   }

   @Override
   public Pointer getPointer(long offset) {
      this.boundsCheck(offset, Native.POINTER_SIZE);
      return this.shareReferenceIfInBounds(super.getPointer(offset));
   }

   @Override
   public ByteBuffer getByteBuffer(long offset, long length) {
      this.boundsCheck(offset, length);
      ByteBuffer b = super.getByteBuffer(offset, length);
      buffers.put(b, this);
      return b;
   }

   @Override
   public String getString(long offset, String encoding) {
      this.boundsCheck(offset, 0L);
      return super.getString(offset, encoding);
   }

   @Override
   public String getWideString(long offset) {
      this.boundsCheck(offset, 0L);
      return super.getWideString(offset);
   }

   @Override
   public void setByte(long offset, byte value) {
      this.boundsCheck(offset, 1L);
      super.setByte(offset, value);
   }

   @Override
   public void setChar(long offset, char value) {
      this.boundsCheck(offset, Native.WCHAR_SIZE);
      super.setChar(offset, value);
   }

   @Override
   public void setShort(long offset, short value) {
      this.boundsCheck(offset, 2L);
      super.setShort(offset, value);
   }

   @Override
   public void setInt(long offset, int value) {
      this.boundsCheck(offset, 4L);
      super.setInt(offset, value);
   }

   @Override
   public void setLong(long offset, long value) {
      this.boundsCheck(offset, 8L);
      super.setLong(offset, value);
   }

   @Override
   public void setFloat(long offset, float value) {
      this.boundsCheck(offset, 4L);
      super.setFloat(offset, value);
   }

   @Override
   public void setDouble(long offset, double value) {
      this.boundsCheck(offset, 8L);
      super.setDouble(offset, value);
   }

   @Override
   public void setPointer(long offset, Pointer value) {
      this.boundsCheck(offset, Native.POINTER_SIZE);
      super.setPointer(offset, value);
   }

   @Override
   public void setString(long offset, String value, String encoding) {
      this.boundsCheck(offset, Native.getBytes(value, encoding).length + 1L);
      super.setString(offset, value, encoding);
   }

   @Override
   public void setWideString(long offset, String value) {
      this.boundsCheck(offset, (value.length() + 1L) * Native.WCHAR_SIZE);
      super.setWideString(offset, value);
   }

   @Override
   public String toString() {
      return "allocated@0x" + Long.toHexString(this.peer) + " (" + this.size + " bytes)";
   }

   protected static void free(long p) {
      if (p != 0L) {
         Native.free(p);
      }
   }

   protected static long malloc(long size) {
      return Native.malloc(size);
   }

   public String dump() {
      return this.dump(0L, (int)this.size());
   }

   private Pointer shareReferenceIfInBounds(Pointer target) {
      if (target == null) {
         return null;
      } else {
         long offset = target.peer - this.peer;
         return offset >= 0L && offset < this.size ? this.share(offset) : target;
      }
   }

   private static final class MemoryDisposer implements Runnable {
      private long peer;

      public MemoryDisposer(long peer) {
         this.peer = peer;
      }

      @Override
      public synchronized void run() {
         try {
            Memory.free(this.peer);
         } finally {
            Memory.allocatedMemory.remove(this.peer);
            this.peer = 0L;
         }
      }
   }

   private class SharedMemory extends Memory {
      public SharedMemory(long offset, long size) {
         this.size = size;
         this.peer = Memory.this.peer + offset;
      }

      @Override
      protected synchronized void dispose() {
         this.peer = 0L;
      }

      @Override
      protected void boundsCheck(long off, long sz) {
         Memory.this.boundsCheck(this.peer - Memory.this.peer + off, sz);
      }

      @Override
      public String toString() {
         return super.toString() + " (shared from " + Memory.this.toString() + ")";
      }
   }
}
