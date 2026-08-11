package padej.soup.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.sound.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.core.Main;
import padej.soup.core.server.ServerConfigManager;
import padej.soup.implement.features.modules.particles.TotemParticles;

@ProtIgnore
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
   @Shadow
   private ClientWorld world;

   @Shadow
   private static ItemStack getActiveDeathProtector(PlayerEntity player) {
      return null;
   }

   @Inject(method = "onGameJoin", at = @At("RETURN"))
   private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
      MinecraftClient client = Main.mc;
      if (client.getCurrentServerEntry() != null) {
         String serverAddress = client.getCurrentServerEntry().address;
         ServerConfigManager.onServerJoin(serverAddress);
      }
   }

   @Inject(method = "onEntityStatus", at = @At("HEAD"), cancellable = true)
   private void onTotemPopCustomParticles(EntityStatusS2CPacket packet, CallbackInfo ci) {
      MinecraftClient client = Main.mc;
      TotemParticles module = TotemParticles.getInstance();
      if (module != null && module.isEnabled()) {
         if (packet.getStatus() == 35) {
            Entity entity = packet.getEntity(this.world);
            if (entity != null) {
               this.world.playSound(entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ITEM_TOTEM_USE, entity.getSoundCategory(), 1.0F, 1.0F, false);
               if (entity == client.player) {
                  client.gameRenderer.showFloatingItem(getActiveDeathProtector(client.player));
               }

               client.execute(() -> module.onTotemPop(entity));
               ci.cancel();
            }
         }
      }
   }
}
