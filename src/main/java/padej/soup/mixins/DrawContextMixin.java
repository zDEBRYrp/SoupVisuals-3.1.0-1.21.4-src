package padej.soup.mixins;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.base.QuickImports;
import padej.soup.base.util.render.ItemOverlayRenderer;
import padej.soup.implement.features.modules.hud.ItemOverlay;

@ProtIgnore
@Mixin(DrawContext.class)
public abstract class DrawContextMixin implements QuickImports {
   @Unique
   private static final ThreadLocal<Boolean> IS_SCALING = ThreadLocal.withInitial(() -> false);

   @Shadow
   public abstract MatrixStack getMatrices();

   @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)V", at = @At("HEAD"))
   private void onDrawItemScaleStart(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
      if (!IS_SCALING.get()) {
         ItemOverlay itemOverlay = ItemOverlay.getInstance();
         if (itemOverlay.isEnabled() && itemOverlay.getEnableItemScale().isValue()) {
            float scale = itemOverlay.getItemScale().getValue();
            if (scale != 1.0F) {
               IS_SCALING.set(true);
               MatrixStack matrices = this.getMatrices();
               matrices.push();
               float centerX = x + 8;
               float centerY = y + 8;
               matrices.translate(centerX, centerY, 0.0F);
               matrices.scale(scale, scale, 1.0F);
               matrices.translate(-centerX, -centerY, 0.0F);
            }
         }
      }
   }

   @Inject(method = "drawItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;IIII)V", at = @At("RETURN"))
   private void onDrawItemScaleEnd(LivingEntity entity, World world, ItemStack stack, int x, int y, int seed, int z, CallbackInfo ci) {
      if (IS_SCALING.get()) {
         MatrixStack matrices = this.getMatrices();
         matrices.pop();
         IS_SCALING.set(false);
      }
   }

   @Inject(method = "drawItemBar", at = @At("HEAD"), cancellable = true)
   private void onDrawItemBar(ItemStack stack, int x, int y, CallbackInfo ci) {
      if (ItemOverlayRenderer.renderItemBar((DrawContext)(Object)this, stack, x, y)) {
         ci.cancel();
      }
   }

   @Inject(method = "drawStackCount", at = @At("HEAD"), cancellable = true)
   private void onDrawStackCount(TextRenderer textRenderer, ItemStack stack, int x, int y, @Nullable String stackCountText, CallbackInfo ci) {
      if (ItemOverlayRenderer.renderStackCount((DrawContext)(Object)this, textRenderer, stack, x, y, stackCountText)) {
         ci.cancel();
      }
   }

   @Inject(method = "drawCooldownProgress", at = @At("HEAD"), cancellable = true)
   private void onDrawCooldownProgress(ItemStack stack, int x, int y, CallbackInfo ci) {
      float cooldownProgress = mc.player == null
         ? 0.0F
         : mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickDelta(true));
      if (ItemOverlayRenderer.renderCooldownProgress((DrawContext)(Object)this, x, y, cooldownProgress)) {
         ci.cancel();
      }
   }
}
