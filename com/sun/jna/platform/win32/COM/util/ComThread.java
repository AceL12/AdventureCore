package com.sun.jna.platform.win32.COM.util;

import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.COMUtils;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ComThread {
   private static ThreadLocal<Boolean> isCOMThread = new ThreadLocal<>();
   ExecutorService executor;
   Runnable firstTask;
   boolean requiresInitialisation = true;
   long timeoutMilliseconds;
   UncaughtExceptionHandler uncaughtExceptionHandler;

   public ComThread(String threadName, long timeoutMilliseconds, UncaughtExceptionHandler uncaughtExceptionHandler) {
      this(threadName, timeoutMilliseconds, uncaughtExceptionHandler, 0);
   }

   public ComThread(final String threadName, long timeoutMilliseconds, UncaughtExceptionHandler uncaughtExceptionHandler, final int coinitialiseExFlag) {
      this.timeoutMilliseconds = timeoutMilliseconds;
      this.uncaughtExceptionHandler = uncaughtExceptionHandler;
      this.firstTask = new Runnable() {
         @Override
         public void run() {
            try {
               WinNT.HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, coinitialiseExFlag);
               ComThread.isCOMThread.set(true);
               COMUtils.checkRC(hr);
               ComThread.this.requiresInitialisation = false;
            } catch (Throwable var2) {
               ComThread.this.uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), var2);
            }
         }
      };
      this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
         @Override
         public Thread newThread(Runnable r) {
            if (!ComThread.this.requiresInitialisation) {
               throw new RuntimeException("ComThread executor has a problem.");
            } else {
               Thread thread = new Thread(r, threadName);
               thread.setDaemon(true);
               thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                  @Override
                  public void uncaughtException(Thread t, Throwable e) {
                     ComThread.this.requiresInitialisation = true;
                     ComThread.this.uncaughtExceptionHandler.uncaughtException(t, e);
                  }
               });
               return thread;
            }
         }
      });
   }

   public void terminate(long timeoutMilliseconds) {
      try {
         this.executor.submit(new Runnable() {
            @Override
            public void run() {
               Ole32.INSTANCE.CoUninitialize();
            }
         }).get(timeoutMilliseconds, TimeUnit.MILLISECONDS);
         this.executor.shutdown();
      } catch (InterruptedException var4) {
         var4.printStackTrace();
      } catch (ExecutionException var5) {
         var5.printStackTrace();
      } catch (TimeoutException var6) {
         this.executor.shutdownNow();
      }
   }

   @Override
   protected void finalize() throws Throwable {
      if (!this.executor.isShutdown()) {
         this.terminate(100L);
      }
   }

   static void setComThread(boolean value) {
      isCOMThread.set(value);
   }

   public <T> T execute(Callable<T> task) throws TimeoutException, InterruptedException, ExecutionException {
      Boolean comThread = isCOMThread.get();
      if (comThread == null) {
         comThread = false;
      }

      if (comThread) {
         try {
            return task.call();
         } catch (Exception var4) {
            throw new ExecutionException(var4);
         }
      } else {
         if (this.requiresInitialisation) {
            this.executor.execute(this.firstTask);
         }

         return this.executor.submit(task).get(this.timeoutMilliseconds, TimeUnit.MILLISECONDS);
      }
   }
}
