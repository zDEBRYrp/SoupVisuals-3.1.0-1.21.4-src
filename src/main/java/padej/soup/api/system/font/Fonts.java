package padej.soup.api.system.font;

import java.awt.Font;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import padej.soup.core.Main;

public class Fonts {
   private static final Map<Fonts.FontKey, FontRenderer> fontCache = new HashMap<>();

   public static FontRenderer create(float size, String name) {
      try {
         String path = "assets/minecraft/fonts/" + name + ".otf";

         try (InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(path)) {
            Font font = Font.createFont(0, Objects.requireNonNull(inputStream)).deriveFont(0, size / 2.0F);
            return new FontRenderer(font, size / 2.0F);
         }
      } catch (Exception e) {
         throw new IllegalStateException("Failed to load font: " + name, e);
      }
   }

   public static void init() {
      for (Fonts.Type type : Fonts.Type.values()) {
         for (int size = 4; size <= 32; size++) {
            fontCache.put(new Fonts.FontKey(size, type), create(size, type.getType()));
         }
      }
   }

   public static FontRenderer getSize(int size) {
      return getSize(size, Fonts.Type.INTER_BOLD);
   }

   public static FontRenderer getSize(int size, Fonts.Type type) {
      return fontCache.computeIfAbsent(new Fonts.FontKey(size, type), k -> create(size, type.getType()));
   }

   private record FontKey(int size, Fonts.Type type) {
   }

   public enum Type {
      SF_DEFAULT("sfpromedium"),
      SF_BOLD("sfprosemibold"),
      INTER_DEFAULT("inter"),
      INTER_BOLD("inter_bold"),
      ICO("ico"),
      JET_DEFAULT("jetbrains_mono"),
      JET_BOLD("jetbrains_mono_bold");

      private final String type;

      public String getType() {
         return this.type;
      }

      Type(final String type) {
         this.type = type;
      }
   }
}
