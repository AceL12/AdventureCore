package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

@Immutable
final class SolarisSoundCard extends AbstractSoundCard {
   private static final String LSHAL = "lshal";
   private static final String DEFAULT_AUDIO_DRIVER = "audio810";

   SolarisSoundCard(String kernelVersion, String name, String codec) {
      super(kernelVersion, name, codec);
   }

   public static List<SoundCard> getSoundCards() {
      Map<String, String> vendorMap = new HashMap<>();
      Map<String, String> productMap = new HashMap<>();
      List<String> sounds = new ArrayList<>();
      String key = "";

      for (String line : ExecutingCommand.runNative("lshal")) {
         line = line.trim();
         if (line.startsWith("udi =")) {
            key = ParseUtil.getSingleQuoteStringValue(line);
         } else if (!key.isEmpty() && !line.isEmpty()) {
            if (line.contains("info.solaris.driver =") && "audio810".equals(ParseUtil.getSingleQuoteStringValue(line))) {
               sounds.add(key);
            } else if (line.contains("info.product")) {
               productMap.put(key, ParseUtil.getStringBetween(line, '\''));
            } else if (line.contains("info.vendor")) {
               vendorMap.put(key, ParseUtil.getStringBetween(line, '\''));
            }
         }
      }

      List<SoundCard> soundCards = new ArrayList<>();

      for (String s : sounds) {
         soundCards.add(new SolarisSoundCard(productMap.get(s) + " " + "audio810", vendorMap.get(s) + " " + productMap.get(s), productMap.get(s)));
      }

      return soundCards;
   }
}
