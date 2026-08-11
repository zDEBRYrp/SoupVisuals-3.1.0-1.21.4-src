package padej.soup.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import padej.soup.api.accessor.ICapabilityTracker;

@Mixin(targets = "com.mojang.blaze3d.platform.GlStateManager$CapabilityTracker")
public abstract class CapabilityTrackerMixin implements ICapabilityTracker {
   @Shadow
   private boolean state;

   @Shadow
   public abstract void setState(boolean var1);

   @Override
   public boolean soup$get() {
      return this.state;
   }

   @Override
   public void soup$set(boolean state) {
      this.setState(state);
   }
}
