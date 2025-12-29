package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.ParseUtil;

@Immutable
final class AixSoundCard extends AbstractSoundCard {
   AixSoundCard(String kernelVersion, String name, String codec) {
      super(kernelVersion, name, codec);
   }

   public static List<SoundCard> getSoundCards(Supplier<List<String>> lscfg) {
      List<SoundCard> soundCards = new ArrayList<>();

      for (String line : lscfg.get()) {
         String s = line.trim();
         if (s.startsWith("paud")) {
            String[] split = ParseUtil.whitespaces.split(s, 3);
            if (split.length == 3) {
               soundCards.add(new AixSoundCard("unknown", split[2], "unknown"));
            }
         }
      }

      return soundCards;
   }
}
