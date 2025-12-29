package oshi;

public enum PlatformEnum {
   MACOS("macOS"),
   LINUX("Linux"),
   WINDOWS("Windows"),
   SOLARIS("Solaris"),
   FREEBSD("FreeBSD"),
   OPENBSD("OpenBSD"),
   WINDOWSCE("Windows CE"),
   AIX("AIX"),
   ANDROID("Android"),
   GNU("GNU"),
   KFREEBSD("kFreeBSD"),
   NETBSD("NetBSD"),
   UNKNOWN("Unknown");

   private final String name;

   private PlatformEnum(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public static String getName(int osType) {
      return getValue(osType).getName();
   }

   public static PlatformEnum getValue(int osType) {
      return osType >= 0 && osType < UNKNOWN.ordinal() ? values()[osType] : UNKNOWN;
   }
}
