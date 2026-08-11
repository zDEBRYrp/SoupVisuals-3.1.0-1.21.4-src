package padej.soup.mixins;

import net.minecraft.client.render.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.implement.events.render.CameraRollEvent;

@ProtIgnore
@Mixin(Camera.class)
public class CameraMixin {
   @Shadow
   private Quaternionf rotation;
   @Shadow
   private Vector3f horizontalPlane;
   @Shadow
   private Vector3f verticalPlane;
   @Shadow
   private Vector3f diagonalPlane;

   @Inject(method = "setRotation", at = @At("TAIL"))
   private void onSetRotation(float yaw, float pitch, CallbackInfo ci) {
      CameraRollEvent event = new CameraRollEvent(yaw, pitch, 0.0F);
      EventManager.callEvent(event);
      float roll = event.getRoll();
      if (roll != 0.0F) {
         float yawRad = (180.0F - yaw) * (float) (Math.PI / 180.0);
         float pitchRad = -pitch * (float) (Math.PI / 180.0);
         float rollRad = roll * (float) (Math.PI / 180.0);
         this.rotation.identity();
         this.rotation.rotateY(yawRad);
         this.rotation.rotateX(pitchRad);
         this.rotation.rotateZ(rollRad);
         this.horizontalPlane.set(0.0F, 0.0F, 1.0F).rotate(this.rotation);
         this.verticalPlane.set(0.0F, 1.0F, 0.0F).rotate(this.rotation);
         this.diagonalPlane.set(1.0F, 0.0F, 0.0F).rotate(this.rotation);
      }
   }
}
