package com.sun.jna.internal;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Cleaner {
   private static final Cleaner INSTANCE = new Cleaner();
   private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
   private final Thread cleanerThread = new Thread() {
      @Override
      public void run() {
         while (true) {
            try {
               Reference<? extends Object> ref = Cleaner.this.referenceQueue.remove();
               if (ref instanceof Cleaner.CleanerRef) {
                  ((Cleaner.CleanerRef)ref).clean();
               }
            } catch (InterruptedException var2) {
               Logger.getLogger(Cleaner.class.getName()).log(Level.SEVERE, null, var2);
               return;
            } catch (Exception var3) {
               Logger.getLogger(Cleaner.class.getName()).log(Level.SEVERE, null, var3);
            }
         }
      }
   };
   private Cleaner.CleanerRef firstCleanable;

   public static Cleaner getCleaner() {
      return INSTANCE;
   }

   private Cleaner() {
      this.cleanerThread.setName("JNA Cleaner");
      this.cleanerThread.setDaemon(true);
      this.cleanerThread.start();
   }

   public synchronized Cleaner.Cleanable register(Object obj, Runnable cleanupTask) {
      return this.add(new Cleaner.CleanerRef(this, obj, this.referenceQueue, cleanupTask));
   }

   private synchronized Cleaner.CleanerRef add(Cleaner.CleanerRef ref) {
      if (this.firstCleanable == null) {
         this.firstCleanable = ref;
      } else {
         ref.setNext(this.firstCleanable);
         this.firstCleanable.setPrevious(ref);
         this.firstCleanable = ref;
      }

      return ref;
   }

   private synchronized boolean remove(Cleaner.CleanerRef ref) {
      boolean inChain = false;
      if (ref == this.firstCleanable) {
         this.firstCleanable = ref.getNext();
         inChain = true;
      }

      if (ref.getPrevious() != null) {
         ref.getPrevious().setNext(ref.getNext());
      }

      if (ref.getNext() != null) {
         ref.getNext().setPrevious(ref.getPrevious());
      }

      if (ref.getPrevious() != null || ref.getNext() != null) {
         inChain = true;
      }

      ref.setNext(null);
      ref.setPrevious(null);
      return inChain;
   }

   public interface Cleanable {
      void clean();
   }

   private static class CleanerRef extends PhantomReference<Object> implements Cleaner.Cleanable {
      private final Cleaner cleaner;
      private final Runnable cleanupTask;
      private Cleaner.CleanerRef previous;
      private Cleaner.CleanerRef next;

      public CleanerRef(Cleaner cleaner, Object referent, ReferenceQueue<? super Object> q, Runnable cleanupTask) {
         super(referent, q);
         this.cleaner = cleaner;
         this.cleanupTask = cleanupTask;
      }

      @Override
      public void clean() {
         if (this.cleaner.remove(this)) {
            this.cleanupTask.run();
         }
      }

      Cleaner.CleanerRef getPrevious() {
         return this.previous;
      }

      void setPrevious(Cleaner.CleanerRef previous) {
         this.previous = previous;
      }

      Cleaner.CleanerRef getNext() {
         return this.next;
      }

      void setNext(Cleaner.CleanerRef next) {
         this.next = next;
      }
   }
}
