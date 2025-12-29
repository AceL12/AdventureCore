package oshi.driver.unix.aix;

import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.platform.unix.LibCAPI;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.unix.AixLibc;
import oshi.util.FileUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class PsInfo {
   private static final Logger LOG = LoggerFactory.getLogger(PsInfo.class);
   private static final AixLibc LIBC = AixLibc.INSTANCE;
   private static final long PAGE_SIZE = 4096L;

   private PsInfo() {
   }

   public static AixLibc.AixPsInfo queryPsInfo(int pid) {
      return new AixLibc.AixPsInfo(FileUtil.readAllBytesAsBuffer(String.format("/proc/%d/psinfo", pid)));
   }

   public static AixLibc.AixLwpsInfo queryLwpsInfo(int pid, int tid) {
      return new AixLibc.AixLwpsInfo(FileUtil.readAllBytesAsBuffer(String.format("/proc/%d/lwp/%d/lwpsinfo", pid, tid)));
   }

   public static Triplet<Integer, Long, Long> queryArgsEnvAddrs(int pid, AixLibc.AixPsInfo psinfo) {
      if (psinfo != null) {
         int argc = psinfo.pr_argc;
         if (argc > 0) {
            long argv = psinfo.pr_argv;
            long envp = psinfo.pr_envp;
            return new Triplet<>(argc, argv, envp);
         } else {
            LOG.trace("Failed argc sanity check: argc={}", argc);
            return null;
         }
      } else {
         LOG.trace("Failed to read psinfo file for pid: {} ", pid);
         return null;
      }
   }

   public static Pair<List<String>, Map<String, String>> queryArgsEnv(int pid, AixLibc.AixPsInfo psinfo) {
      List<String> args = new ArrayList<>();
      Map<String, String> env = new LinkedHashMap<>();
      Triplet<Integer, Long, Long> addrs = queryArgsEnvAddrs(pid, psinfo);
      if (addrs != null) {
         String procas = "/proc/" + pid + "/as";
         int fd = LIBC.open(procas, 0);
         if (fd < 0) {
            LOG.trace("No permission to read file: {} ", procas);
            return new Pair<>(args, env);
         } else {
            try {
               int argc = addrs.getA();
               long argv = addrs.getB();
               long envp = addrs.getC();
               Path p = Paths.get("/proc/" + pid + "/status");

               long increment;
               try {
                  byte[] status = Files.readAllBytes(p);
                  if (status[17] == 1) {
                     increment = 8L;
                  } else {
                     increment = 4L;
                  }
               } catch (IOException var39) {
                  return new Pair<>(args, env);
               }

               Memory buffer = new Memory(8192L);

               try {
                  LibCAPI.size_t bufSize = new LibCAPI.size_t(buffer.size());
                  long bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, 0L, argv);
                  long[] argPtr = new long[argc];
                  long argp = bufStart == 0L ? 0L : getOffsetFromBuffer(buffer, argv - bufStart, increment);
                  if (argp > 0L) {
                     for (int i = 0; i < argc; i++) {
                        long offset = argp + i * increment;
                        bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, offset);
                        argPtr[i] = bufStart == 0L ? 0L : getOffsetFromBuffer(buffer, offset - bufStart, increment);
                     }
                  }

                  bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, envp);
                  List<Long> envPtrList = new ArrayList<>();
                  long addr = bufStart == 0L ? 0L : getOffsetFromBuffer(buffer, envp - bufStart, increment);
                  int limit = 500;

                  for (long offset = addr; addr != 0L; offset += increment) {
                     if (--limit <= 0) {
                        break;
                     }

                     bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, offset);
                     long envPtr = bufStart == 0L ? 0L : getOffsetFromBuffer(buffer, offset - bufStart, increment);
                     if (envPtr != 0L) {
                        envPtrList.add(envPtr);
                     }
                  }

                  for (int i = 0; i < argPtr.length && argPtr[i] != 0L; i++) {
                     bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, argPtr[i]);
                     if (bufStart != 0L) {
                        String argStr = buffer.getString(argPtr[i] - bufStart);
                        if (!argStr.isEmpty()) {
                           args.add(argStr);
                        }
                     }
                  }

                  for (Long envPtr : envPtrList) {
                     bufStart = conditionallyReadBufferFromStartOfPage(fd, buffer, bufSize, bufStart, envPtr);
                     if (bufStart != 0L) {
                        String envStr = buffer.getString(envPtr - bufStart);
                        int idx = envStr.indexOf(61);
                        if (idx > 0) {
                           env.put(envStr.substring(0, idx), envStr.substring(idx + 1));
                        }
                     }
                  }
               } catch (Throwable var38) {
                  try {
                     buffer.close();
                  } catch (Throwable var37) {
                     var38.addSuppressed(var37);
                  }

                  throw var38;
               }

               buffer.close();
               return new Pair<>(args, env);
            } finally {
               LIBC.close(fd);
            }
         }
      } else {
         return new Pair<>(args, env);
      }
   }

   private static long conditionallyReadBufferFromStartOfPage(int fd, Memory buffer, LibCAPI.size_t bufSize, long bufStart, long addr) {
      if (addr >= bufStart && addr - bufStart <= 4096L) {
         return bufStart;
      } else {
         long newStart = Math.floorDiv(addr, 4096L) * 4096L;
         LibCAPI.ssize_t result = LIBC.pread(fd, buffer, bufSize, new NativeLong(newStart));
         if (result.longValue() < 4096L) {
            LOG.debug("Failed to read page from address space: {} bytes read", result.longValue());
            return 0L;
         } else {
            return newStart;
         }
      }
   }

   private static long getOffsetFromBuffer(Memory buffer, long offset, long increment) {
      return increment == 8L ? buffer.getLong(offset) : buffer.getInt(offset);
   }
}
