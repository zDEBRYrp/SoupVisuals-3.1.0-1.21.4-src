package padej.soup.base.util.render;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.ItemOverlay;

public final class ItemOverlayRenderer implements QuickImports {
   public static boolean renderItemBar(DrawContext context, ItemStack stack, int x, int y) {
      ItemOverlay itemOverlay = ItemOverlay.getInstance();
      if (!itemOverlay.isEnabled() || !itemOverlay.getCustomDurabilityBar().isValue()) {
         return false;
      }

      if (!stack.isItemBarVisible()) {
         return false;
      }

      int barX = x + 2;
      int barY = y + 13;
      int barWidth = stack.getItemBarStep();
      boolean drawShadow = itemOverlay.getDurabilityBarShadow().isValue();
      int barColor = getBarColor(itemOverlay);
      if (drawShadow) {
         context.fill(RenderLayer.getGui(), barX, barY, barX + 13, barY + 2, 200, -16777216);
      }

      context.fill(RenderLayer.getGui(), barX, barY, barX + barWidth, barY + 1, 200, barColor);
      return true;
   }

   private static int getBarColor(ItemOverlay itemOverlay) {
      int[] colors = itemOverlay.getCustomBarColors();
      if (colors == null) {
         return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
      }

      String animation = itemOverlay.getBarColorAnimation().getSelected();
      return "Wave".equals(animation) ? getWaveColor(colors) : ColorUtil.multAlpha(colors[0], 1.0F);
   }

   private static int getWaveColor(int[] colors) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], 1.0F);
      } else if (colors.length == 2) {
         int angle = (int)(System.currentTimeMillis() / 8L % 360L);
         angle = angle >= 180 ? 360 - angle : angle;
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), 1.0F);
      } else {
         float timeProgress = (float)(System.currentTimeMillis() / 10L % (colors.length * 360L)) / 360.0F;
         int index1 = (int)Math.floor(timeProgress) % colors.length;
         int index2 = (index1 + 1) % colors.length;
         float lerp = timeProgress - (int)Math.floor(timeProgress);
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), 1.0F);
      }
   }

   public static boolean renderStackCount(DrawContext context, TextRenderer textRenderer, ItemStack stack, int x, int y, @Nullable String stackCountText) {
      ItemOverlay itemOverlay = ItemOverlay.getInstance();
      if (itemOverlay.isEnabled() && itemOverlay.getCustomStackCount().isValue()) {
         if (stack.getCount() == 1 && stackCountText == null) {
            return false;
         }

         String string = stackCountText == null ? String.valueOf(stack.getCount()) : stackCountText;
         if (itemOverlay.getAddPrefix().isValue() && stackCountText == null) {
            string = "x" + string;
         }

         MatrixStack matrices = context.getMatrices();
         matrices.push();
         matrices.translate(0.0F, 0.0F, 200.0F);
         int color = itemOverlay.getEnableCustomColor().isValue() ? itemOverlay.getStackCountColor().getColor() : ColorUtil.getText();
         if (itemOverlay.getUseCustomFont().isValue()) {
            FontRenderer font = Fonts.getSize(18, Fonts.Type.SF_BOLD);
            int textX = x + 19 - 2 - (int)font.getStringWidth(string);
            int textY = y + 6 + 4;
            font.drawString(matrices, string, textX, textY, color);
         } else {
            int textX = x + 19 - 2 - textRenderer.getWidth(string);
            int textY = y + 6 + 3;
            context.drawText(textRenderer, string, textX, textY, color, true);
         }

         matrices.pop();
         return true;
      } else {
         return false;
      }
   }

   public static boolean renderCooldownProgress(DrawContext context, int x, int y, float cooldownProgress) {
      ItemOverlay itemOverlay = ItemOverlay.getInstance();
      if (!itemOverlay.isEnabled() || !itemOverlay.getCustomCooldown().isValue()) {
         return false;
      }

      if (cooldownProgress <= 0.0F) {
         return false;
      }

      String cooldownStyle = itemOverlay.getCooldownStyle().getSelected();
      if ("Ring".equals(cooldownStyle)) {
         renderCooldownRing(context, x, y, cooldownProgress, itemOverlay);
      } else {
         renderCooldownBar(context, x, y, cooldownProgress, itemOverlay);
      }

      if (itemOverlay.getShowCooldownNumber().isValue()) {
         renderCooldownNumber(context, x, y, cooldownProgress);
      }

      return true;
   }

   private static void renderCooldownRing(DrawContext context, int x, int y, float cooldownProgress, ItemOverlay itemOverlay) {
      if (!(cooldownProgress < 0.01F)) {
         int color = getCooldownColor(itemOverlay);
         arc.render(
            ShapeProperties.create(context.getMatrices(), x - 2, y - 2, 20.0, 20.0)
               .round(0.27F)
               .thickness(0.25F)
               .end(cooldownProgress * 360.0F)
               .color(color)
               .build()
         );
      }
   }

   private static void renderCooldownBar(DrawContext context, int x, int y, float cooldownProgress, ItemOverlay itemOverlay) {
      if (!(cooldownProgress < 0.01F)) {
         MatrixStack matrices = context.getMatrices();
         matrices.push();
         matrices.translate(x, y, 0.0F);
         matrices.translate(0.0F, 16.0F * (1.0F - cooldownProgress), 0.0F);
         matrices.scale(1.0F, cooldownProgress, 1.0F);
         int color = getCooldownColor(itemOverlay);
         context.fill(RenderLayer.getGui(), 0, 0, 16, 16, 200, color);
         matrices.pop();
      }
   }

   private static void renderCooldownNumber(DrawContext context, int x, int y, float remainingSeconds) {
      String text = String.format("%.1f", remainingSeconds);
      MatrixStack matrices = context.getMatrices();
      matrices.push();
      matrices.translate(0.0F, 0.0F, 250.0F);
      FontRenderer font = Fonts.getSize(14, Fonts.Type.SF_BOLD);
      float textWidth = font.getStringWidth(text);
      float textX = x + 8.0F - textWidth / 2.0F;
      float textY = y + 8.0F;
      font.drawString(matrices, text, textX, textY, ColorUtil.getText());
      matrices.pop();
   }

   private static int getCooldownColor(ItemOverlay itemOverlay) {
      int baseColor;
      if (itemOverlay.getCooldownColorMode().isSelected("Custom")) {
         baseColor = itemOverlay.getCooldownColor().getColor();
      } else {
         baseColor = ColorUtil.getClientColor();
      }

      float alpha = itemOverlay.getCooldownAlpha().getValue();
      return ColorUtil.replAlpha(baseColor, alpha);
   }

   private ItemOverlayRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
