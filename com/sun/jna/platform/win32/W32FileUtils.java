package com.sun.jna.platform.win32;

import com.sun.jna.platform.FileUtils;
import java.io.File;
import java.io.IOException;

public class W32FileUtils extends FileUtils {
   @Override
   public boolean hasTrash() {
      return true;
   }

   @Override
   public void moveToTrash(File... files) throws IOException {
      Shell32 shell = Shell32.INSTANCE;
      ShellAPI.SHFILEOPSTRUCT fileop = new ShellAPI.SHFILEOPSTRUCT();
      fileop.wFunc = 3;
      String[] paths = new String[files.length];

      for (int i = 0; i < paths.length; i++) {
         paths[i] = files[i].getAbsolutePath();
      }

      fileop.pFrom = fileop.encodePaths(paths);
      fileop.fFlags = 1620;
      int ret = shell.SHFileOperation(fileop);
      if (ret != 0) {
         throw new IOException("Move to trash failed: " + fileop.pFrom + ": " + Kernel32Util.formatMessageFromLastErrorCode(ret));
      } else if (fileop.fAnyOperationsAborted) {
         throw new IOException("Move to trash aborted");
      }
   }
}
