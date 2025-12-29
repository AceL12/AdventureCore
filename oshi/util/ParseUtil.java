package oshi.util;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

@ThreadSafe
public final class ParseUtil {
   private static final Logger LOG = LoggerFactory.getLogger(ParseUtil.class);
   private static final String DEFAULT_LOG_MSG = "{} didn't parse. Returning default. {}";
   private static final Pattern HERTZ_PATTERN = Pattern.compile("(\\d+(.\\d+)?) ?([kKMGT]?Hz).*");
   private static final Pattern BYTES_PATTERN = Pattern.compile("(\\d+) ?([kKMGT]?B?).*");
   private static final Pattern UNITS_PATTERN = Pattern.compile("(\\d+(.\\d+)?)[\\s]?([kKMGT])?");
   private static final Pattern VALID_HEX = Pattern.compile("[0-9a-fA-F]+");
   private static final Pattern DHMS = Pattern.compile("(?:(\\d+)-)?(?:(\\d+):)??(?:(\\d+):)?(\\d+)(?:\\.(\\d+))?");
   private static final Pattern UUID_PATTERN = Pattern.compile(".*([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}).*");
   private static final Pattern VENDOR_PRODUCT_ID_SERIAL = Pattern.compile(".*(?:VID|VEN)_(\\p{XDigit}{4})&(?:PID|DEV)_(\\p{XDigit}{4})(.*)\\\\(.*)");
   private static final Pattern LSPCI_MACHINE_READABLE = Pattern.compile("(.+)\\s\\[(.*?)\\]");
   private static final Pattern LSPCI_MEMORY_SIZE = Pattern.compile(".+\\s\\[size=(\\d+)([kKMGT])\\]");
   private static final String HZ = "Hz";
   private static final String KHZ = "kHz";
   private static final String MHZ = "MHz";
   private static final String GHZ = "GHz";
   private static final String THZ = "THz";
   private static final String PHZ = "PHz";
   private static final Map<String, Long> multipliers = new HashMap<>();
   private static final long EPOCH_DIFF = 11644473600000L;
   private static final int TZ_OFFSET = TimeZone.getDefault().getOffset(System.currentTimeMillis());
   public static final Pattern whitespacesColonWhitespace = Pattern.compile("\\s+:\\s");
   public static final Pattern whitespaces = Pattern.compile("\\s+");
   public static final Pattern notDigits = Pattern.compile("[^0-9]+");
   public static final Pattern startWithNotDigits = Pattern.compile("^[^0-9]*");
   public static final Pattern slash = Pattern.compile("\\/");
   private static final long[] POWERS_OF_TEN = new long[]{
      1L,
      10L,
      100L,
      1000L,
      10000L,
      100000L,
      1000000L,
      10000000L,
      100000000L,
      1000000000L,
      10000000000L,
      100000000000L,
      1000000000000L,
      10000000000000L,
      100000000000000L,
      1000000000000000L,
      10000000000000000L,
      100000000000000000L,
      1000000000000000000L
   };
   private static final DateTimeFormatter CIM_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSSSSZZZZZ", Locale.US);

   private ParseUtil() {
   }

   public static long parseHertz(String hertz) {
      Matcher matcher = HERTZ_PATTERN.matcher(hertz.trim());
      if (matcher.find() && matcher.groupCount() == 3) {
         double value = Double.valueOf(matcher.group(1)) * multipliers.getOrDefault(matcher.group(3), -1L).longValue();
         if (value >= 0.0) {
            return (long)value;
         }
      }

      return -1L;
   }

   public static int parseLastInt(String s, int i) {
      try {
         String ls = parseLastString(s);
         return ls.toLowerCase().startsWith("0x") ? Integer.decode(ls) : Integer.parseInt(ls);
      } catch (NumberFormatException var3) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var3);
         return i;
      }
   }

   public static long parseLastLong(String s, long li) {
      try {
         String ls = parseLastString(s);
         return ls.toLowerCase().startsWith("0x") ? Long.decode(ls) : Long.parseLong(ls);
      } catch (NumberFormatException var4) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var4);
         return li;
      }
   }

   public static double parseLastDouble(String s, double d) {
      try {
         return Double.parseDouble(parseLastString(s));
      } catch (NumberFormatException var4) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var4);
         return d;
      }
   }

   public static String parseLastString(String s) {
      String[] ss = whitespaces.split(s);
      return ss[ss.length - 1];
   }

   public static String byteArrayToHexString(byte[] bytes) {
      StringBuilder sb = new StringBuilder(bytes.length * 2);

      for (byte b : bytes) {
         sb.append(Character.forDigit((b & 240) >>> 4, 16));
         sb.append(Character.forDigit(b & 15, 16));
      }

      return sb.toString().toUpperCase();
   }

   public static byte[] hexStringToByteArray(String digits) {
      int len = digits.length();
      if (VALID_HEX.matcher(digits).matches() && (len & 1) == 0) {
         byte[] data = new byte[len / 2];

         for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte)(Character.digit(digits.charAt(i), 16) << 4 | Character.digit(digits.charAt(i + 1), 16));
         }

         return data;
      } else {
         LOG.warn("Invalid hexadecimal string: {}", digits);
         return new byte[0];
      }
   }

   public static byte[] asciiStringToByteArray(String text, int length) {
      return Arrays.copyOf(text.getBytes(StandardCharsets.US_ASCII), length);
   }

   public static byte[] longToByteArray(long value, int valueSize, int length) {
      long val = value;
      byte[] b = new byte[8];

      for (int i = 7; i >= 0 && val != 0L; i--) {
         b[i] = (byte)val;
         val >>>= 8;
      }

      return Arrays.copyOfRange(b, 8 - valueSize, 8 + length - valueSize);
   }

   public static long strToLong(String str, int size) {
      return byteArrayToLong(str.getBytes(StandardCharsets.US_ASCII), size);
   }

   public static long byteArrayToLong(byte[] bytes, int size) {
      return byteArrayToLong(bytes, size, true);
   }

   public static long byteArrayToLong(byte[] bytes, int size, boolean bigEndian) {
      if (size > 8) {
         throw new IllegalArgumentException("Can't convert more than 8 bytes.");
      } else if (size > bytes.length) {
         throw new IllegalArgumentException("Size can't be larger than array length.");
      } else {
         long total = 0L;

         for (int i = 0; i < size; i++) {
            if (bigEndian) {
               total = total << 8 | bytes[i] & 255;
            } else {
               total = total << 8 | bytes[size - i - 1] & 255;
            }
         }

         return total;
      }
   }

   public static float byteArrayToFloat(byte[] bytes, int size, int fpBits) {
      return (float)byteArrayToLong(bytes, size) / (1 << fpBits);
   }

   public static long unsignedIntToLong(int unsignedValue) {
      long longValue = unsignedValue;
      return longValue & 4294967295L;
   }

   public static long unsignedLongToSignedLong(long unsignedValue) {
      return unsignedValue & Long.MAX_VALUE;
   }

   public static String hexStringToString(String hexString) {
      if (hexString.length() % 2 > 0) {
         return hexString;
      } else {
         StringBuilder sb = new StringBuilder();

         try {
            for (int pos = 0; pos < hexString.length(); pos += 2) {
               int charAsInt = Integer.parseInt(hexString.substring(pos, pos + 2), 16);
               if (charAsInt < 32 || charAsInt > 127) {
                  return hexString;
               }

               sb.append((char)charAsInt);
            }
         } catch (NumberFormatException var4) {
            LOG.trace("{} didn't parse. Returning default. {}", hexString, var4);
            return hexString;
         }

         return sb.toString();
      }
   }

   public static int parseIntOrDefault(String s, int defaultInt) {
      try {
         return Integer.parseInt(s);
      } catch (NumberFormatException var3) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var3);
         return defaultInt;
      }
   }

   public static long parseLongOrDefault(String s, long defaultLong) {
      try {
         return Long.parseLong(s);
      } catch (NumberFormatException var4) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var4);
         return defaultLong;
      }
   }

   public static long parseUnsignedLongOrDefault(String s, long defaultLong) {
      try {
         return new BigInteger(s).longValue();
      } catch (NumberFormatException var4) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var4);
         return defaultLong;
      }
   }

   public static double parseDoubleOrDefault(String s, double defaultDouble) {
      try {
         return Double.parseDouble(s);
      } catch (NumberFormatException var4) {
         LOG.trace("{} didn't parse. Returning default. {}", s, var4);
         return defaultDouble;
      }
   }

   public static long parseDHMSOrDefault(String s, long defaultLong) {
      Matcher m = DHMS.matcher(s);
      if (m.matches()) {
         long milliseconds = 0L;
         if (m.group(1) != null) {
            milliseconds += parseLongOrDefault(m.group(1), 0L) * 86400000L;
         }

         if (m.group(2) != null) {
            milliseconds += parseLongOrDefault(m.group(2), 0L) * 3600000L;
         }

         if (m.group(3) != null) {
            milliseconds += parseLongOrDefault(m.group(3), 0L) * 60000L;
         }

         milliseconds += parseLongOrDefault(m.group(4), 0L) * 1000L;
         if (m.group(5) != null) {
            milliseconds += (long)(1000.0 * parseDoubleOrDefault("0." + m.group(5), 0.0));
         }

         return milliseconds;
      } else {
         return defaultLong;
      }
   }

   public static String parseUuidOrDefault(String s, String defaultStr) {
      Matcher m = UUID_PATTERN.matcher(s.toLowerCase());
      return m.matches() ? m.group(1) : defaultStr;
   }

   public static String getSingleQuoteStringValue(String line) {
      return getStringBetween(line, '\'');
   }

   public static String getDoubleQuoteStringValue(String line) {
      return getStringBetween(line, '"');
   }

   public static String getStringBetween(String line, char c) {
      int firstOcc = line.indexOf(c);
      return firstOcc < 0 ? "" : line.substring(firstOcc + 1, line.lastIndexOf(c)).trim();
   }

   public static int getFirstIntValue(String line) {
      return getNthIntValue(line, 1);
   }

   public static int getNthIntValue(String line, int n) {
      String[] split = notDigits.split(startWithNotDigits.matcher(line).replaceFirst(""));
      return split.length >= n ? parseIntOrDefault(split[n - 1], 0) : 0;
   }

   public static String removeMatchingString(String original, String toRemove) {
      if (original != null && !original.isEmpty() && toRemove != null && !toRemove.isEmpty()) {
         int matchIndex = original.indexOf(toRemove, 0);
         if (matchIndex == -1) {
            return original;
         } else {
            StringBuilder buffer = new StringBuilder(original.length() - toRemove.length());
            int currIndex = 0;

            do {
               buffer.append(original.substring(currIndex, matchIndex));
               currIndex = matchIndex + toRemove.length();
               matchIndex = original.indexOf(toRemove, currIndex);
            } while (matchIndex != -1);

            buffer.append(original.substring(currIndex));
            return buffer.toString();
         }
      } else {
         return original;
      }
   }

   public static long[] parseStringToLongArray(String s, int[] indices, int length, char delimiter) {
      long[] parsed = new long[indices.length];
      int charIndex = s.length();
      int parsedIndex = indices.length - 1;
      int stringIndex = length - 1;
      int power = 0;
      boolean delimCurrent = false;
      boolean numeric = true;
      boolean numberFound = false;
      boolean dashSeen = false;

      while (--charIndex > 0 && parsedIndex >= 0) {
         int c = s.charAt(charIndex);
         if (c == delimiter) {
            if (!numberFound && numeric) {
               numberFound = true;
            }

            if (!delimCurrent) {
               if (numberFound && indices[parsedIndex] == stringIndex--) {
                  parsedIndex--;
               }

               delimCurrent = true;
               power = 0;
               dashSeen = false;
               numeric = true;
            }
         } else if (indices[parsedIndex] != stringIndex || c == 43 || !numeric) {
            delimCurrent = false;
         } else if (c >= 48 && c <= 57 && !dashSeen) {
            if (power <= 18 && (power != 17 || c != 57 || parsed[parsedIndex] <= 223372036854775807L)) {
               parsed[parsedIndex] += (c - 48) * POWERS_OF_TEN[power++];
            } else {
               parsed[parsedIndex] = Long.MAX_VALUE;
            }

            delimCurrent = false;
         } else if (c == 45) {
            parsed[parsedIndex] *= -1L;
            delimCurrent = false;
            dashSeen = true;
         } else {
            if (numberFound) {
               if (!noLog(s)) {
                  LOG.error("Illegal character parsing string '{}' to long array: {}", s, s.charAt(charIndex));
               }

               return new long[indices.length];
            }

            parsed[parsedIndex] = 0L;
            numeric = false;
         }
      }

      if (parsedIndex > 0) {
         if (!noLog(s)) {
            LOG.error("Not enough fields in string '{}' parsing to long array: {}", s, indices.length - parsedIndex);
         }

         return new long[indices.length];
      } else {
         return parsed;
      }
   }

   private static boolean noLog(String s) {
      return s.startsWith("NOLOG: ");
   }

   public static int countStringToLongArray(String s, char delimiter) {
      int charIndex = s.length();
      int numbers = 0;
      boolean delimCurrent = false;
      boolean numeric = true;
      boolean dashSeen = false;

      while (--charIndex > 0) {
         int c = s.charAt(charIndex);
         if (c == delimiter) {
            if (!delimCurrent) {
               if (numeric) {
                  numbers++;
               }

               delimCurrent = true;
               dashSeen = false;
               numeric = true;
            }
         } else if (c == 43 || !numeric) {
            delimCurrent = false;
         } else if (c >= 48 && c <= 57 && !dashSeen) {
            delimCurrent = false;
         } else if (c == 45) {
            delimCurrent = false;
            dashSeen = true;
         } else {
            if (numbers > 0) {
               return numbers;
            }

            numeric = false;
         }
      }

      return numbers + 1;
   }

   public static String getTextBetweenStrings(String text, String before, String after) {
      String result = "";
      if (text.indexOf(before) >= 0 && text.indexOf(after) >= 0) {
         result = text.substring(text.indexOf(before) + before.length(), text.length());
         result = result.substring(0, result.indexOf(after));
      }

      return result;
   }

   public static long filetimeToUtcMs(long filetime, boolean local) {
      return filetime / 10000L - 11644473600000L - (local ? TZ_OFFSET : 0L);
   }

   public static String parseMmDdYyyyToYyyyMmDD(String dateString) {
      try {
         return String.format("%s-%s-%s", dateString.substring(6, 10), dateString.substring(0, 2), dateString.substring(3, 5));
      } catch (StringIndexOutOfBoundsException var2) {
         return dateString;
      }
   }

   public static OffsetDateTime parseCimDateTimeToOffset(String cimDateTime) {
      try {
         int tzInMinutes = Integer.parseInt(cimDateTime.substring(22));
         LocalTime offsetAsLocalTime = LocalTime.MIDNIGHT.plusMinutes(tzInMinutes);
         return OffsetDateTime.parse(cimDateTime.substring(0, 22) + offsetAsLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME), CIM_FORMAT);
      } catch (NumberFormatException | DateTimeParseException | IndexOutOfBoundsException var3) {
         LOG.trace("Unable to parse {} to CIM DateTime.", cimDateTime);
         return Constants.UNIX_EPOCH;
      }
   }

   public static boolean filePathStartsWith(List<String> prefixList, String path) {
      for (String match : prefixList) {
         if (path.equals(match) || path.startsWith(match + "/")) {
            return true;
         }
      }

      return false;
   }

   public static long parseMultipliedToLongs(String count) {
      Matcher matcher = UNITS_PATTERN.matcher(count.trim());
      String[] mem;
      if (matcher.find() && matcher.groupCount() == 3) {
         mem = new String[2];
         mem[0] = matcher.group(1);
         mem[1] = matcher.group(3);
      } else {
         mem = new String[]{count};
      }

      double number = parseDoubleOrDefault(mem[0], 0.0);
      if (mem.length == 2 && mem[1] != null && mem[1].length() >= 1) {
         switch (mem[1].charAt(0)) {
            case 'G':
               number *= 1.0E9;
               break;
            case 'K':
            case 'k':
               number *= 1000.0;
               break;
            case 'M':
               number *= 1000000.0;
               break;
            case 'T':
               number *= 1.0E12;
         }
      }

      return (long)number;
   }

   public static long parseDecimalMemorySizeToBinary(String size) {
      String[] mem = whitespaces.split(size);
      if (mem.length < 2) {
         Matcher matcher = BYTES_PATTERN.matcher(size.trim());
         if (matcher.find() && matcher.groupCount() == 2) {
            mem = new String[]{matcher.group(1), matcher.group(2)};
         }
      }

      long capacity = parseLongOrDefault(mem[0], 0L);
      if (mem.length == 2 && mem[1].length() > 1) {
         switch (mem[1].charAt(0)) {
            case 'G':
               capacity <<= 30;
               break;
            case 'K':
            case 'k':
               capacity <<= 10;
               break;
            case 'M':
               capacity <<= 20;
               break;
            case 'T':
               capacity <<= 40;
         }
      }

      return capacity;
   }

   public static Triplet<String, String, String> parseDeviceIdToVendorProductSerial(String deviceId) {
      Matcher m = VENDOR_PRODUCT_ID_SERIAL.matcher(deviceId);
      if (!m.matches()) {
         return null;
      } else {
         String vendorId = "0x" + m.group(1).toLowerCase();
         String productId = "0x" + m.group(2).toLowerCase();
         String serial = m.group(4);
         return new Triplet<>(vendorId, productId, m.group(3).isEmpty() && !serial.contains("&") ? serial : "");
      }
   }

   public static long parseLshwResourceString(String resources) {
      long bytes = 0L;
      String[] resourceArray = whitespaces.split(resources);

      for (String r : resourceArray) {
         if (r.startsWith("memory:")) {
            String[] mem = r.substring(7).split("-");
            if (mem.length == 2) {
               try {
                  bytes += Long.parseLong(mem[1], 16) - Long.parseLong(mem[0], 16) + 1L;
               } catch (NumberFormatException var10) {
                  LOG.trace("{} didn't parse. Returning default. {}", r, var10);
               }
            }
         }
      }

      return bytes;
   }

   public static Pair<String, String> parseLspciMachineReadable(String line) {
      Matcher matcher = LSPCI_MACHINE_READABLE.matcher(line);
      return matcher.matches() ? new Pair<>(matcher.group(1), matcher.group(2)) : null;
   }

   public static long parseLspciMemorySize(String line) {
      Matcher matcher = LSPCI_MEMORY_SIZE.matcher(line);
      return matcher.matches() ? parseDecimalMemorySizeToBinary(matcher.group(1) + " " + matcher.group(2) + "B") : 0L;
   }

   public static List<Integer> parseHyphenatedIntList(String str) {
      List<Integer> result = new ArrayList<>();

      for (String s : whitespaces.split(str)) {
         if (s.contains("-")) {
            int first = getFirstIntValue(s);
            int last = getNthIntValue(s, 2);

            for (int i = first; i <= last; i++) {
               result.add(i);
            }
         } else {
            int only = parseIntOrDefault(s, -1);
            if (only >= 0) {
               result.add(only);
            }
         }
      }

      return result;
   }

   public static byte[] parseIntToIP(int ip) {
      return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip).array();
   }

   public static byte[] parseIntArrayToIP(int[] ip6) {
      ByteBuffer bb = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);

      for (int i : ip6) {
         bb.putInt(i);
      }

      return bb.array();
   }

   public static int bigEndian16ToLittleEndian(int port) {
      return port >> 8 & 0xFF | port << 8 & 0xFF00;
   }

   public static String parseUtAddrV6toIP(int[] utAddrV6) {
      if (utAddrV6.length != 4) {
         throw new IllegalArgumentException("ut_addr_v6 must have exactly 4 elements");
      } else if (utAddrV6[1] != 0 || utAddrV6[2] != 0 || utAddrV6[3] != 0) {
         byte[] ipv6 = ByteBuffer.allocate(16).putInt(utAddrV6[0]).putInt(utAddrV6[1]).putInt(utAddrV6[2]).putInt(utAddrV6[3]).array();

         try {
            return InetAddress.getByAddress(ipv6).getHostAddress().replaceAll("((?:(?:^|:)0+\\b){2,8}):?(?!\\S*\\b\\1:0+\\b)(\\S*)", "::$2");
         } catch (UnknownHostException var4) {
            return "unknown";
         }
      } else if (utAddrV6[0] == 0) {
         return "::";
      } else {
         byte[] ipv4 = ByteBuffer.allocate(4).putInt(utAddrV6[0]).array();

         try {
            return InetAddress.getByAddress(ipv4).getHostAddress();
         } catch (UnknownHostException var3) {
            return "unknown";
         }
      }
   }

   public static int hexStringToInt(String hexString, int defaultValue) {
      if (hexString != null) {
         try {
            if (hexString.startsWith("0x")) {
               return new BigInteger(hexString.substring(2), 16).intValue();
            }

            return new BigInteger(hexString, 16).intValue();
         } catch (NumberFormatException var3) {
            LOG.trace("{} didn't parse. Returning default. {}", hexString, var3);
         }
      }

      return defaultValue;
   }

   public static long hexStringToLong(String hexString, long defaultValue) {
      if (hexString != null) {
         try {
            if (hexString.startsWith("0x")) {
               return new BigInteger(hexString.substring(2), 16).longValue();
            }

            return new BigInteger(hexString, 16).longValue();
         } catch (NumberFormatException var4) {
            LOG.trace("{} didn't parse. Returning default. {}", hexString, var4);
         }
      }

      return defaultValue;
   }

   public static String removeLeadingDots(String dotPrefixedStr) {
      int pos = 0;

      while (pos < dotPrefixedStr.length() && dotPrefixedStr.charAt(pos) == '.') {
         pos++;
      }

      return pos < dotPrefixedStr.length() ? dotPrefixedStr.substring(pos) : "";
   }

   public static List<String> parseByteArrayToStrings(byte[] bytes) {
      List<String> strList = new ArrayList<>();
      int start = 0;
      int end = 0;

      do {
         if (end == bytes.length || bytes[end] == 0) {
            if (start == end) {
               break;
            }

            strList.add(new String(bytes, start, end - start, StandardCharsets.UTF_8));
            start = end + 1;
         }
      } while (end++ < bytes.length);

      return strList;
   }

   public static Map<String, String> parseByteArrayToStringMap(byte[] bytes) {
      Map<String, String> strMap = new LinkedHashMap<>();
      int start = 0;
      int end = 0;
      String key = null;

      do {
         if (end != bytes.length && bytes[end] != 0) {
            if (bytes[end] == 61 && key == null) {
               key = new String(bytes, start, end - start, StandardCharsets.UTF_8);
               start = end + 1;
            }
         } else {
            if (start == end && key == null) {
               break;
            }

            strMap.put(key, new String(bytes, start, end - start, StandardCharsets.UTF_8));
            key = null;
            start = end + 1;
         }
      } while (end++ < bytes.length);

      return strMap;
   }

   public static Map<String, String> parseCharArrayToStringMap(char[] chars) {
      Map<String, String> strMap = new LinkedHashMap<>();
      int start = 0;
      int end = 0;
      String key = null;

      do {
         if (end != chars.length && chars[end] != 0) {
            if (chars[end] == '=' && key == null) {
               key = new String(chars, start, end - start);
               start = end + 1;
            }
         } else {
            if (start == end && key == null) {
               break;
            }

            strMap.put(key, new String(chars, start, end - start));
            key = null;
            start = end + 1;
         }
      } while (end++ < chars.length);

      return strMap;
   }

   public static <K extends Enum<K>> Map<K, String> stringToEnumMap(Class<K> clazz, String values, char delim) {
      EnumMap<K, String> map = new EnumMap<>(clazz);
      int start = 0;
      int len = values.length();
      EnumSet<K> keys = EnumSet.allOf(clazz);
      int keySize = keys.size();

      label29:
      for (K key : keys) {
         keySize--;
         int idx = keySize == 0 ? len : values.indexOf(delim, start);
         if (idx >= 0) {
            map.put(key, values.substring(start, idx));
            start = idx;

            while (true) {
               start++;
               if (start >= len || values.charAt(start) != delim) {
                  continue label29;
               }
            }
         }

         map.put(key, values.substring(start));
         break;
      }

      return map;
   }

   static {
      multipliers.put("Hz", 1L);
      multipliers.put("kHz", 1000L);
      multipliers.put("MHz", 1000000L);
      multipliers.put("GHz", 1000000000L);
      multipliers.put("THz", 1000000000000L);
      multipliers.put("PHz", 1000000000000000L);
   }
}
