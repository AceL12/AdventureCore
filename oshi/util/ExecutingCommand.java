package oshi.util;

import com.sun.jna.Platform;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class ExecutingCommand {
   private static final Logger LOG = LoggerFactory.getLogger(ExecutingCommand.class);
   private static final String[] DEFAULT_ENV = getDefaultEnv();

   private ExecutingCommand() {
   }

   private static String[] getDefaultEnv() {
      return Platform.isWindows() ? new String[]{"LANGUAGE=C"} : new String[]{"LC_ALL=C"};
   }

   public static List<String> runNative(String cmdToRun) {
      String[] cmd = cmdToRun.split(" ");
      return runNative(cmd);
   }

   public static List<String> runNative(String[] cmdToRunWithArgs) {
      return runNative(cmdToRunWithArgs, DEFAULT_ENV);
   }

   public static List<String> runNative(String[] cmdToRunWithArgs, String[] envp) {
      Process p = null;

      try {
         p = Runtime.getRuntime().exec(cmdToRunWithArgs, envp);
         return getProcessOutput(p, cmdToRunWithArgs);
      } catch (IOException | SecurityException var21) {
         LOG.trace("Couldn't run command {}: {}", Arrays.toString((Object[])cmdToRunWithArgs), var21.getMessage());
      } finally {
         if (p != null) {
            if (Platform.isWindows() || Platform.isSolaris()) {
               try {
                  p.getOutputStream().close();
               } catch (IOException var20) {
               }

               try {
                  p.getInputStream().close();
               } catch (IOException var19) {
               }

               try {
                  p.getErrorStream().close();
               } catch (IOException var18) {
               }
            }

            p.destroy();
         }
      }

      return Collections.emptyList();
   }

   private static List<String> getProcessOutput(Process p, String[] cmd) {
      ArrayList<String> sa = new ArrayList<>();

      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()));

         try {
            String line;
            while ((line = reader.readLine()) != null) {
               sa.add(line);
            }

            p.waitFor();
         } catch (Throwable var7) {
            try {
               reader.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         reader.close();
      } catch (IOException var8) {
         LOG.trace("Problem reading output from {}: {}", Arrays.toString((Object[])cmd), var8.getMessage());
      } catch (InterruptedException var9) {
         LOG.trace("Interrupted while reading output from {}: {}", Arrays.toString((Object[])cmd), var9.getMessage());
         Thread.currentThread().interrupt();
      }

      return sa;
   }

   public static String getFirstAnswer(String cmd2launch) {
      return getAnswerAt(cmd2launch, 0);
   }

   public static String getAnswerAt(String cmd2launch, int answerIdx) {
      List<String> sa = runNative(cmd2launch);
      return answerIdx >= 0 && answerIdx < sa.size() ? sa.get(answerIdx) : "";
   }
}
