package com.sun.jna.platform.unix;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface LibC extends LibCAPI, Library {
   String NAME = "c";
   LibC INSTANCE = Native.load("c", LibC.class);
}
