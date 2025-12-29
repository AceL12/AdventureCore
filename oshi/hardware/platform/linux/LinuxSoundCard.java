package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.Immutable;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractSoundCard;
import oshi.util.FileUtil;
import oshi.util.platform.linux.ProcPath;

@Immutable
final class LinuxSoundCard extends AbstractSoundCard {
   private static final Logger LOG = LoggerFactory.getLogger(LinuxSoundCard.class);
   private static final String CARD_FOLDER = "card";
   private static final String CARDS_FILE = "cards";
   private static final String ID_FILE = "id";

   LinuxSoundCard(String kernelVersion, String name, String codec) {
      super(kernelVersion, name, codec);
   }

   private static List<File> getCardFolders() {
      File cardsDirectory = new File(ProcPath.ASOUND);
      List<File> cardFolders = new ArrayList<>();
      File[] allContents = cardsDirectory.listFiles();
      if (allContents != null) {
         for (File card : allContents) {
            if (card.getName().startsWith("card") && card.isDirectory()) {
               cardFolders.add(card);
            }
         }
      } else {
         LOG.warn("No Audio Cards Found");
      }

      return cardFolders;
   }

   private static String getSoundCardVersion() {
      String driverVersion = FileUtil.getStringFromFile(ProcPath.ASOUND + "version");
      return driverVersion.isEmpty() ? "not available" : driverVersion;
   }

   private static String getCardCodec(File cardDir) {
      String cardCodec = "";
      File[] cardFiles = cardDir.listFiles();
      if (cardFiles != null) {
         for (File file : cardFiles) {
            if (file.getName().startsWith("codec")) {
               if (!file.isDirectory()) {
                  cardCodec = FileUtil.getKeyValueMapFromFile(file.getPath(), ":").get("Codec");
               } else {
                  File[] codecs = file.listFiles();
                  if (codecs != null) {
                     for (File codec : codecs) {
                        if (!codec.isDirectory() && codec.getName().contains("#")) {
                           cardCodec = codec.getName().substring(0, codec.getName().indexOf(35));
                           break;
                        }
                     }
                  }
               }
            }
         }
      }

      return cardCodec;
   }

   private static String getCardName(File file) {
      String cardName = "Not Found..";
      Map<String, String> cardNamePairs = FileUtil.getKeyValueMapFromFile(ProcPath.ASOUND + "/" + "cards", ":");
      String cardId = FileUtil.getStringFromFile(file.getPath() + "/" + "id");

      for (Entry<String, String> entry : cardNamePairs.entrySet()) {
         if (entry.getKey().contains(cardId)) {
            return entry.getValue();
         }
      }

      return cardName;
   }

   public static List<SoundCard> getSoundCards() {
      List<SoundCard> soundCards = new ArrayList<>();

      for (File cardFile : getCardFolders()) {
         soundCards.add(new LinuxSoundCard(getSoundCardVersion(), getCardName(cardFile), getCardCodec(cardFile)));
      }

      return soundCards;
   }
}
