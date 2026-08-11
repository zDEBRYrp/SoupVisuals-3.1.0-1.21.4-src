package padej.soup.mixins;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import padej.protect.ProtIgnore;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.texture.TextureObfuscator;

@ProtIgnore
@Mixin(NativeImage.class)
public class NativeImageMixin {
   @ModifyVariable(
      method = "read(Lnet/minecraft/client/texture/NativeImage$Format;Ljava/io/InputStream;)Lnet/minecraft/client/texture/NativeImage;",
      at = @At("HEAD"),
      argsOnly = true,
      ordinal = 0
   )
   private static InputStream deobfuscateTexture(InputStream stream) {
      try {
         if (!stream.markSupported()) {
            byte[] allData = stream.readAllBytes();
            byte[] header = new byte[8];
            System.arraycopy(allData, 0, header, 0, Math.min(8, allData.length));
            if (allData.length >= 8 && TextureObfuscator.isObfuscatedHeader(header)) {
               byte[] deobfuscated = TextureObfuscator.deobfuscate(allData);
               return new ByteArrayInputStream(deobfuscated);
            } else {
               return new ByteArrayInputStream(allData);
            }
         } else {
            byte[] header = new byte[8];
            stream.mark(8);
            int read = stream.read(header);
            stream.reset();
            if (read == 8 && TextureObfuscator.isObfuscatedHeader(header)) {
               byte[] allData = stream.readAllBytes();
               byte[] deobfuscated = TextureObfuscator.deobfuscate(allData);
               return new ByteArrayInputStream(deobfuscated);
            } else {
               return stream;
            }
         }
      } catch (Exception e) {
         LoggerUtil.error("Failed to deobfuscate texture: " + e.getMessage());
         return stream;
      }
   }
}
