package padej.soup.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.implement.events.player.EventAttack;

@ProtIgnore
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
   @Shadow
   @Final
   private MinecraftClient client;

   @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
   private void onAttackEntityPre(PlayerEntity player, Entity target, CallbackInfo ci) {
      Vec3d hitPos = this.getHitPosition(target);
      EventAttack event = new EventAttack(target, true, hitPos);
      EventManager.callEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "attackEntity", at = @At("RETURN"))
   private void onAttackEntityPost(PlayerEntity player, Entity target, CallbackInfo ci) {
      Vec3d hitPos = this.getHitPosition(target);
      EventAttack event = new EventAttack(target, false, hitPos);
      EventManager.callEvent(event);
   }

   @Unique
   private Vec3d getHitPosition(Entity target) {
      HitResult hitResult = this.client.crosshairTarget;
      if (hitResult != null && hitResult.getType() == Type.ENTITY) {
         EntityHitResult entityHit = (EntityHitResult)hitResult;
         if (entityHit.getEntity() == target) {
            return entityHit.getPos();
         }
      }

      return target.getPos().add(0.0, target.getHeight() / 2.0, 0.0);
   }
}
