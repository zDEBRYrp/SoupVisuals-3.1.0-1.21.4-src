package padej.soup.mixins;

import java.util.ConcurrentModificationException;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.base.QuickImports;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.render.HotBarStatusRenderer;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.core.Main;
import padej.soup.implement.events.render.DrawEvent;
import padej.soup.implement.features.draggables.TargetHud;
import padej.soup.implement.features.modules.hud.CrossHair;
import padej.soup.implement.features.modules.hud.HotBar;
import padej.soup.implement.features.modules.hud.Potions;
import padej.soup.implement.features.modules.hud.ScoreBoard;

@ProtIgnore
@Mixin(InGameHud.class)
public abstract class InGameHudMixin implements QuickImports {
   @Final
   @Shadow
   private MinecraftClient client;
   @Unique
   private float displayedHealth = 20.0F;
   @Unique
   private float displayedArmor = 0.0F;
   @Unique
   private float displayedFood = 20.0F;
   @Unique
   private float displayedAir = 300.0F;
   @Unique
   private float displayedAbsorption = 0.0F;
   @Unique
   private TargetHud cachedTargetHudDraggable;

   @Shadow
   protected abstract void renderStatusBars(DrawContext var1);

   @Shadow
   protected abstract void renderMountHealth(DrawContext var1);

   @Inject(method = "render", at = @At("RETURN"))
   public void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      blur.setup();
      DrawEvent event = new DrawEvent(context, drawEngine, tickCounter.getTickDelta(false));
      EventManager.callEvent(event);
      Render2DUtil.onRender(context);
      if (!this.client.options.hudHidden) {
         List<AbstractDraggable> draggables = Main.getInstance().getDraggableRepository().draggable();

         for (AbstractDraggable draggable : draggables) {
            if (draggable.canDraw(draggable)) {
               draggable.startAnimation();
            } else {
               draggable.stopAnimation();
            }

            float scale = draggable.getScaleAnimation().getOutput().floatValue();
            if (!(scale <= 0.01F)) {
               draggable.validPosition();
               MathUtil.setAlpha(scale, () -> {
                  try {
                     float draggableScale = draggable.getDraggableScale();
                     if (Math.abs(draggableScale - 1.0F) <= 0.001F) {
                        draggable.drawDraggable(context);
                        return;
                     }

                     context.getMatrices().push();

                     try {
                        float centerX = draggable.getX() + draggable.getWidth() / 2.0F;
                        float centerY = draggable.getY() + draggable.getHeight() / 2.0F;
                        context.getMatrices().translate(centerX, centerY, 0.0F);
                        context.getMatrices().scale(draggableScale, draggableScale, 1.0F);
                        context.getMatrices().translate(-centerX, -centerY, 0.0F);
                        draggable.drawDraggable(context);
                     } finally {
                        context.getMatrices().pop();
                     }
                  } catch (ConcurrentModificationException e) {
                     LoggerUtil.error("Failed to render draggable: " + e.getMessage());
                  }
               });
            }
         }

         try {
            if (!padej.soup.implement.features.modules.hud.TargetHud.getInstance().isEnabled()) {
               return;
            }

            TargetHud targetHud = this.getTargetHudDraggable(draggables);
            if (targetHud != null) {
               targetHud.renderParticlesAlways(context);
            }
         } catch (Exception e) {
            LoggerUtil.error("Failed onRender: " + e.getMessage());
         }
      }
   }

   @Unique
   private TargetHud getTargetHudDraggable(List<AbstractDraggable> draggables) {
      if (this.cachedTargetHudDraggable != null) {
         return this.cachedTargetHudDraggable;
      }

      for (AbstractDraggable draggable : draggables) {
         if (draggable instanceof TargetHud targetHud) {
            this.cachedTargetHudDraggable = targetHud;
            return targetHud;
         }
      }

      return null;
   }

   @Inject(
      method = "renderCrosshair",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;CROSSHAIR_TEXTURE:Lnet/minecraft/util/Identifier;"),
      cancellable = true
   )
   public void renderCrosshairHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      CrossHair crossHair = CrossHair.getInstance();
      if (crossHair.isState()) {
         crossHair.onRenderCrossHair();
         ci.cancel();
      }
   }

   @Inject(at = @At("HEAD"), method = "renderStatusEffectOverlay", cancellable = true)
   public void renderStatusEffectOverlayHook(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (Potions.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void renderScoreboardSidebarHook(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (ScoreBoard.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderOverlayMessage", at = @At("HEAD"), cancellable = true)
   private void renderOverlayMessage(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (HotBar.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
   private void renderExperienceLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (HotBar.getInstance().isEnabled()) {
         ci.cancel();
      }
   }

   @Inject(method = "renderMainHud", at = @At("HEAD"), cancellable = true)
   private void renderMainHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
      if (HotBar.getInstance().isEnabled()) {
         context.drawGuiTexture(RenderLayer::getGuiTextured, InGameHud.HOTBAR_ATTACK_INDICATOR_BACKGROUND_TEXTURE, 0, 0, 1, 1);
         String statusBarsMode = HotBar.getInstance().getStatusBarsMode().getSelected();
         if (this.client.interactionManager.hasStatusBars()) {
            if (statusBarsMode.equals("Bars")) {
               this.renderCustomStatusBars(context);
            } else {
               this.renderStatusBars(context);
            }
         }

         this.renderMountHealth(context);
         ci.cancel();
      }
   }

   @Unique
   private void renderCustomStatusBars(DrawContext context) {
      if (this.client.player != null) {
         int screenWidth = context.getScaledWindowWidth();
         int screenHeight = context.getScaledWindowHeight();
         int offsetT = 12;
         float maxHealth = this.client.player.getMaxHealth();
         float health = this.client.player.getHealth();
         float absorption = this.client.player.getAbsorptionAmount();
         float armor = this.client.player.getArmor();
         float food = this.client.player.getHungerManager().getFoodLevel();
         float saturation = this.client.player.getHungerManager().getSaturationLevel();
         int maxAir = this.client.player.getMaxAir();
         int air = this.client.player.getAir();
         this.displayedHealth = this.displayedHealth + (health - this.displayedHealth) * 0.2F;
         this.displayedAbsorption = this.displayedAbsorption + (absorption - this.displayedAbsorption) * 0.2F;
         this.displayedArmor = this.displayedArmor + (armor - this.displayedArmor) * 0.2F;
         this.displayedFood = this.displayedFood + (food - this.displayedFood) * 0.2F;
         this.displayedAir = this.displayedAir + (air - this.displayedAir) * 0.2F;
         int barWidth = 81;
         int barHeight = 9;
         int cornerRadius = 2;
         int centerX = screenWidth / 2;
         int offset = 91;
         int healthX = centerX - offset;
         int healthY = screenHeight - 55;
         HotBarStatusRenderer.renderHealthBar(
            context, healthX, healthY + offsetT, barWidth, barHeight, cornerRadius, this.displayedHealth, maxHealth, this.displayedAbsorption
         );
         if (armor > 0.0F) {
            int armorY = healthY - 15;
            HotBarStatusRenderer.renderArmorBar(context, healthX, armorY + offsetT, barWidth, barHeight, cornerRadius, this.displayedArmor);
         }

         int hungerX = centerX + offset - barWidth;
         int hungerY = screenHeight - 55;
         HotBarStatusRenderer.renderHungerBar(context, hungerX, hungerY + offsetT, barWidth, barHeight, cornerRadius, this.displayedFood, saturation);
         if (air < maxAir) {
            int airY = hungerY - 15;
            HotBarStatusRenderer.renderAirBar(context, hungerX, airY + offsetT, barWidth, barHeight, cornerRadius, this.displayedAir, maxAir);
         }

         if (HotBar.getInstance().getExperienceBar().isValue()) {
            float experienceProgress = this.client.player.experienceProgress;
            int experienceLevel = this.client.player.experienceLevel;
            int expBarWidth = 186;
            int expBarHeight = 4;
            int expBarX = centerX - expBarWidth / 2 + 2;
            int expBarY = screenHeight - 33;
            HotBarStatusRenderer.renderExperienceBar(context, expBarX, expBarY, expBarWidth, expBarHeight, cornerRadius, experienceProgress, experienceLevel);
         }
      }
   }
}
