package padej.soup.base.util.render;

import java.util.List;
import java.util.stream.StreamSupport;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.core.Main;
import padej.soup.core.server.ServerLimitCfg;
import padej.soup.implement.features.modules.hud.TargetHud;

public final class TargetHudRenderer implements QuickImports {
   public static void renderStyleZenith(DrawContext context, LivingEntity target, float x, float y, float width, float height, float health) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(18);
      float hp = PlayerIntersectionUtil.getHealth(target);
      float widthHp = 61.0F;
      String stringHp = ServerLimitCfg.showHp(target) ? PlayerIntersectionUtil.getHealthString(hp) : "??";
      blur.render(
         ShapeProperties.create(matrix, x, y, width, height)
            .round(3.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getRect(TargetHud.getInstance().backAlpha.getValue()))
            .build()
      );
      String displayName = VisibleUtils.getDisplayName(target);
      if (font.getStringWidth(displayName) > 60.0F) {
         ScissorManager scissorManager = Main.getInstance().getScissorManager();
         scissorManager.push(matrix.peek().getPositionMatrix(), x, y, width - 1.0F, height);
         font.drawString(matrix, displayName, x + 34.0F, y + 8.0F, ColorUtil.getText());
         scissorManager.pop();
      } else {
         font.drawString(matrix, displayName, x + 34.0F, y + 8.0F, ColorUtil.getText());
      }

      rectangle.render(ShapeProperties.create(matrix, x + 34.0F, y + 27.0F, widthHp, 2.0).round(0.75F).color(-16382190).build());
      rectangle.render(
         ShapeProperties.create(matrix, x + 34.0F, y + 27.0F, health, 2.0).softness(4.0F).round(1.0F).color(ColorUtil.roundClientColor(0.2F)).build()
      );
      rectangle.render(ShapeProperties.create(matrix, x + 34.0F, y + 27.0F, health, 2.0).round(0.75F).color(ColorUtil.roundClientColor(1.0F)).build());
      float textWidth = Fonts.getSize(14).getStringWidth(stringHp);
      Fonts.getSize(14)
         .drawString(matrix, stringHp, x + MathHelper.clamp(34.0F + health - textWidth / 2.0F, 34.0F, 95.0F - textWidth), y + 21.0F, ColorUtil.getText());
      if (VisibleUtils.shouldShowSkin(target)) {
         renderFace(context, target, x + 5.0F, y + 5.5F, 3.0F, 25.0F);
      } else if (VisibleUtils.isPartiallyVisible(target)) {
         renderBlurFace(context, target, x + 5.0F, y + 5.5F, 3.0F, 25.0F);
      }

      if (ServerLimitCfg.showItems()) {
         renderArmorStyleZenith(context, target, x, y, width);
      }
   }

   public static void renderArmorStyleZenith(DrawContext context, LivingEntity target, float x, float y, float hudWidth) {
      List<ItemStack> items = StreamSupport.stream(target.getEquippedItems().spliterator(), false).filter(s -> !s.isEmpty()).toList();
      if (!items.isEmpty()) {
         MatrixStack matrix = context.getMatrices();
         float armorX = x + hudWidth / 2.0F - items.size() * 5.5F;
         float armorY = y - 13.0F;
         float itemX = -10.5F;
         matrix.push();
         matrix.translate(armorX, armorY, -200.0F);
         blur.render(
            ShapeProperties.create(matrix, 0.0, 0.0, items.size() * 11, 11.0)
               .round(2.5F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(TargetHud.getInstance().backAlpha.getValue()))
               .build()
         );

         for (ItemStack stack : items) {
            Render2DUtil.defaultDrawStack(context, stack, itemX += 11.0F, 0.5F, false, ServerLimitCfg.showItemsOverlay(), 0.5F);
         }

         matrix.pop();
      }
   }

   public static void renderStyleAres(DrawContext context, LivingEntity target, float x, float y, float width, float height, float health) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(18, Fonts.Type.INTER_BOLD);
      float hp = PlayerIntersectionUtil.getHealth(target);
      float widthHp = 68.0F;
      String stringHp = "HP: " + (ServerLimitCfg.showHp(target) ? PlayerIntersectionUtil.getHealthString(hp) : "??");
      float healthBarWidth = health * widthHp / 61.0F;
      blur.render(
         ShapeProperties.create(matrix, x, y, width, height)
            .round(7.0F)
            .softness(1.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getRect(TargetHud.getInstance().backAlpha.getValue()))
            .build()
      );
      String displayName = VisibleUtils.getDisplayName(target);
      if (font.getStringWidth(displayName) > 60.0F) {
         ScissorManager scissorManager = Main.getInstance().getScissorManager();
         scissorManager.push(matrix.peek().getPositionMatrix(), x, y, width - 1.0F, height);
         font.drawString(matrix, displayName, x + 48.0F, y + 8.0F, ColorUtil.getText());
         scissorManager.pop();
      } else {
         font.drawString(matrix, displayName, x + 48.0F, y + 7.0F, ColorUtil.getText());
      }

      float barX = x + 48.0F;
      float barY = y + 32.0F;
      float barHeight = 9.0F;
      rectangle.render(ShapeProperties.create(matrix, barX, barY, widthHp, barHeight).round(1.5F).color(ColorUtil.getMainGuiColor()).build());
      rectangle.render(
         ShapeProperties.create(matrix, barX - 1.0F, barY - 1.0F, healthBarWidth + 2.0F, barHeight + 2.0F)
            .softness(10.0F)
            .round(2.0F)
            .color(ColorUtil.roundClientColor(0.2F))
            .build()
      );
      rectangle.render(ShapeProperties.create(matrix, barX, barY, healthBarWidth, barHeight).round(2.0F).color(ColorUtil.roundClientColor(1.0F)).build());
      Fonts.getSize(16, Fonts.Type.INTER_BOLD).drawString(matrix, stringHp, x + 48.0F, y + 20.5F, ColorUtil.getText());
      if (VisibleUtils.shouldShowSkin(target)) {
         renderFace(context, target, x + 3.0F, y + 3.0F, 6.0F, 40.0F);
      } else if (VisibleUtils.isPartiallyVisible(target)) {
         renderBlurFace(context, target, x + 3.0F, y + 3.0F, 6.0F, 40.0F);
      }
   }

   private static void renderFace(DrawContext context, LivingEntity target, float x, float y, float round, float size) {
      if (mc.getEntityRenderDispatcher().getRenderer(target) instanceof LivingEntityRenderer renderer) {
         LivingEntityRenderState state = (LivingEntityRenderState)renderer.getAndUpdateRenderState(target, tickCounter.getTickDelta(false));
         Identifier textureLocation = renderer.getTexture(state);
         float hurtScale = 1.0F;
         if (target.hurtTime > 0) {
            float hurtProgress = target.hurtTime / 10.0F;
            hurtScale = (float)(1.0 - 0.075F * Math.sin(hurtProgress * Math.PI));
         }

         MatrixStack matrix = context.getMatrices();
         matrix.push();
         float centerX = x + size / 2.0F;
         float centerY = y + size / 2.0F;
         matrix.translate(centerX, centerY, 0.0F);
         matrix.scale(hurtScale, hurtScale, 1.0F);
         matrix.translate(-centerX, -centerY, 0.0F);
         Render2DUtil.drawHead(context, textureLocation, x, y, size, round, ColorUtil.getRect(1.0F), ColorUtil.multRed(-1, 1.0F + target.hurtTime / 4.0F));
         matrix.pop();
      }
   }

   private static void renderBlurFace(DrawContext context, LivingEntity target, float x, float y, float round, float size) {
      MatrixStack matrix = context.getMatrices();
      float hurtScale = 1.0F;
      if (target.hurtTime > 0) {
         float hurtProgress = target.hurtTime / 10.0F;
         hurtScale = (float)(1.0 - 0.075F * Math.sin(hurtProgress * Math.PI));
      }

      matrix.push();
      float centerX = x + size / 2.0F;
      float centerY = y + size / 2.0F;
      matrix.translate(centerX, centerY, 0.0F);
      matrix.scale(hurtScale, hurtScale, 1.0F);
      matrix.translate(-centerX, -centerY, 0.0F);
      int blurColor = ColorUtil.multRed(ColorUtil.getRect(0.2F), 1.0F + target.hurtTime / 4.0F);
      blur.render(
         ShapeProperties.create(matrix, x, y, size, size)
            .round(round)
            .softness(1.0F)
            .thickness(2.0F)
            .color(blurColor)
            .outlineColor(ColorUtil.getOutline())
            .build()
      );
      matrix.pop();
   }

   private TargetHudRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
