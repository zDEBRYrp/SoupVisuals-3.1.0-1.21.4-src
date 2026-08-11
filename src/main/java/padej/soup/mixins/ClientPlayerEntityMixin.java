package padej.soup.mixins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.implement.events.container.CloseScreenEvent;
import padej.soup.implement.events.player.TickEvent;

@ProtIgnore
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayerEntity {
   @Final
   @Shadow
   protected MinecraftClient client;

   @Shadow
   @Override
   public abstract float getPitch(float tickDelta);

   @Shadow
   @Override
   public abstract float getYaw(float tickDelta);

   public ClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @Inject(method = "tick", at = @At("HEAD"))
   public void tick(CallbackInfo info) {
      if (this.client.player != null && this.client.world != null) {
         EventManager.callEvent(new TickEvent());
      }
   }

   @Inject(method = "closeHandledScreen", at = @At("HEAD"), cancellable = true)
   private void closeHandledScreenHook(CallbackInfo info) {
      CloseScreenEvent event = new CloseScreenEvent(this.client.currentScreen);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         info.cancel();
      }
   }
}
