package padej.soup.base.util.texture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import padej.protect.ProtIgnore;
import padej.soup.base.util.logger.LoggerUtil;

@ProtIgnore
public final class TextureObfuscator {
   private static final byte[] XOR_KEY = new byte[]{
      31, 31, 112, -1, -96, -24, 37, -39, 13, 67, 25, -46, -112, 10, 50, -80
   };

   public static byte[] obfuscate(byte[] data) {
      byte[] result = new byte[data.length];

      for (int i = 0; i < data.length; i++) {
         result[i] = (byte)(data[i] ^ XOR_KEY[i % XOR_KEY.length]);
      }

      return result;
   }

   public static byte[] deobfuscate(byte[] data) {
      return obfuscate(data);
   }

   public static InputStream deobfuscateStream(InputStream obfuscatedStream) throws IOException {
      try {
         byte[] obfuscatedData = obfuscatedStream.readAllBytes();
         byte[] deobfuscatedData = deobfuscate(obfuscatedData);
         return new ByteArrayInputStream(deobfuscatedData);
      } catch (IOException e) {
         LoggerUtil.error("Failed to deobfuscate texture stream: " + e.getMessage());
         throw e;
      }
   }

   public static boolean isObfuscatedHeader(byte[] header) {
      if (header.length < 8) {
         return false;
      }

      byte[] pngSignature = new byte[]{-119, 80, 78, 71, 13, 10, 26, 10};
      boolean hasPngSignature = true;

      for (int i = 0; i < 8; i++) {
         if (header[i] != pngSignature[i]) {
            hasPngSignature = false;
            break;
         }
      }

      if (hasPngSignature) {
         return false;
      }

      byte[] deobfuscatedHeader = new byte[8];

      for (int i = 0; i < 8; i++) {
         deobfuscatedHeader[i] = (byte)(header[i] ^ XOR_KEY[i % XOR_KEY.length]);
      }

      for (int i = 0; i < 8; i++) {
         if (deobfuscatedHeader[i] != pngSignature[i]) {
            return false;
         }
      }

      return true;
   }

   public static boolean isObfuscated(byte[] data) {
      if (data.length < 8) {
         return false;
      }

      byte[] header = new byte[8];
      System.arraycopy(data, 0, header, 0, 8);
      return isObfuscatedHeader(header);
   }

   public static byte[] imageToBytes(BufferedImage image) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      return baos.toByteArray();
   }

   public static BufferedImage bytesToImage(byte[] data) throws IOException {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      return ImageIO.read(bais);
   }

   public static String deobfuscateText(String text) {
      byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
      if (textBytes.length > 10) {
         String prefix = text.substring(0, Math.min(10, text.length()));
         if (!prefix.startsWith("#") && !prefix.startsWith("//") && !prefix.startsWith("/*")) {
            byte[] deobfuscated = deobfuscate(textBytes);
            String deobfuscatedText = new String(deobfuscated, StandardCharsets.UTF_8);
            if (deobfuscatedText.contains("#version") || deobfuscatedText.contains("uniform") || deobfuscatedText.contains("void main")) {
               return deobfuscatedText;
            }
         }
      }

      return text;
   }

   public static InputStream deobfuscateTextStream(InputStream stream) throws IOException {
      try {
         byte[] data = stream.readAllBytes();
         String text = new String(data, StandardCharsets.UTF_8);
         int nonAsciiCount = 0;

         for (int i = 0; i < Math.min(100, text.length()); i++) {
            char c = text.charAt(i);
            if ((c < ' ' || c > '~') && c != '\n' && c != '\r' && c != '\t') {
               nonAsciiCount++;
            }
         }

         if (nonAsciiCount > 30) {
            byte[] deobfuscated = deobfuscate(data);
            return new ByteArrayInputStream(deobfuscated);
         } else {
            return new ByteArrayInputStream(data);
         }
      } catch (Exception e) {
         LoggerUtil.error("Failed to deobfuscate text stream: " + e.getMessage());
         throw e;
      }
   }

   private TextureObfuscator() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
