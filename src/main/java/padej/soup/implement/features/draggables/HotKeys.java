package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.feature.module.Module;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.core.Main;
import padej.soup.implement.features.modules.hud.Icons;

public class HotKeys extends AbstractDraggable {
   private final List<Module> keysList = new ArrayList<>();

   public HotKeys() {
      super("HotKeys", 300, 10, 80, 23, true);
   }

   @Override
   public boolean visible() {
      return !this.keysList.isEmpty() || PlayerIntersectionUtil.isChatOrMenu(mc.currentScreen);
   }

   @Override
   public void tick() {
      String mode = padej.soup.implement.features.modules.hud.HotKeys.getInstance().mode.getSelected();
      boolean onlyActive = mode.equals("Active");
      this.keysList.clear();

      for (Module module : Main.getInstance().getModuleProvider().getModules()) {
         if (module.getKey() != -1 && module.isCanBind() && (!onlyActive || !(module.getAnimation().getOutput().floatValue() <= 0.0F))) {
            this.keysList.add(module);
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext e) {
      MatrixStack matrix = e.getMatrices();
      float centerX = this.getX() + this.getWidth() / 2.0F;
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_DEFAULT);
      FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.INTER_DEFAULT);
      FontRenderer fontModuleBold = Fonts.getSize(13, Fonts.Type.INTER_BOLD);
      padej.soup.implement.features.modules.hud.HotKeys hotKeysModule = padej.soup.implement.features.modules.hud.HotKeys.getInstance();
      float headerHeight = 16.0F;
      float padding = 5.0F;
      boolean showHeader = hotKeysModule.getShowHeader().isValue();
      boolean darkenHeader = hotKeysModule.getDarkenHeader().isValue();
      float animatedHeight = this.getAnimatedHeight();
      if (showHeader) {
         int headerColor = darkenHeader ? ColorUtil.getRectDarker(0.9F) : ColorUtil.getRect(0.7F);
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), headerHeight)
               .round(4.0F, 0.0F, 4.0F, 0.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(headerColor)
               .build()
         );
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY() + headerHeight, this.getWidth(), animatedHeight - headerHeight)
               .round(0.0F, 4.0F, 0.0F, 4.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
      } else {
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), animatedHeight)
               .round(4.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
      }

      if (showHeader) {
         font.drawString(matrix, this.getName(), (int)(this.getX() + padding), (int)(this.getY() + 6.5F), ColorUtil.getText());
         float iconSize = 8.0F;
         float iconPadding = 4.0F;
         Icons iconsModule = Icons.getInstance();
         int iconColor;
         if (!iconsModule.getColoredIcons().isValue()) {
            iconColor = ColorUtil.getText();
         } else if (iconsModule.getIconGradient().isValue()) {
            iconColor = ColorUtil.fade(8);
         } else {
            iconColor = ColorUtil.getClientColor();
         }

         image.setTexture("textures/keys.png")
            .render(
               ShapeProperties.create(matrix, this.getX() + this.getWidth() - iconSize - iconPadding, this.getY() + 4, iconSize, iconSize)
                  .color(iconColor)
                  .build()
            );
      }

      int offset = showHeader ? (int)(headerHeight + 7.0F) : 7;
      int maxWidth = 80;
      String mode = padej.soup.implement.features.modules.hud.HotKeys.getInstance().mode.getSelected();

      for (Module module : this.keysList) {
         String bind = StringUtil.getBindName(module.getKey());
         float centerY = this.getY() + offset;
         boolean isActive = module.getAnimation().getOutput().floatValue() > 0.0F;
         float animation = module.getAnimation().getOutput().floatValue();
         FontRenderer bindFont = Fonts.getSize(12, Fonts.Type.INTER_BOLD);
         FontRenderer currentFont;
         if (mode.equals("All") && isActive) {
            currentFont = fontModuleBold;
         } else {
            currentFont = fontModule;
         }

         float bindWidth = bindFont.getStringWidth(bind);
         float moduleNameWidth = currentFont.getStringWidth(module.getVisibleName());
         float totalWidth = moduleNameWidth + bindWidth + 25.0F;
         if (mode.equals("Active")) {
            MathUtil.scale(matrix, centerX, centerY, 1.0F, animation, () -> {
               currentFont.drawString(matrix, module.getVisibleName(), this.getX() + 6, centerY + 1.0F, ColorUtil.getText());
               this.drawBindBadge(matrix, bindFont, bind, bindWidth, centerY + 1.0F);
            });
            offset += (int)(animation * 11.0F);
         } else {
            currentFont.drawString(matrix, module.getVisibleName(), this.getX() + 6, centerY + 1.0F, ColorUtil.getText());
            this.drawBindBadge(matrix, bindFont, bind, bindWidth, centerY + 1.0F);
            offset += 11;
         }

         maxWidth = (int)Math.max(totalWidth, maxWidth);
      }

      this.setWidth(maxWidth);
      this.setHeight(mode.equals("Active") ? (int)this.getAnimatedHeight() : offset);
   }

   private void drawBindBadge(MatrixStack matrix, FontRenderer bindFont, String bind, float bindWidth, float textY) {
      float bindX = this.getX() + this.getWidth() - bindWidth - 10.0F;
      float bindY = textY - 3.5F;
      rectangle.render(
         ShapeProperties.create(matrix, bindX, bindY, bindWidth + 6.0F, 9.0)
            .round(2.0F)
            .thickness(2.0F)
            .outlineColor(ColorUtil.getOutline())
            .color(ColorUtil.getGuiRectColor(1.0F))
            .build()
      );
      int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);
      bindFont.drawString(matrix, bind, bindX + 3.0F, textY, bindingColor);
   }

   private float getAnimatedHeight() {
      padej.soup.implement.features.modules.hud.HotKeys hotKeysModule = padej.soup.implement.features.modules.hud.HotKeys.getInstance();
      String mode = hotKeysModule.mode.getSelected();
      if (!mode.equals("Active")) {
         return this.getHeight();
      }

      float headerHeight = 16.0F;
      boolean showHeader = hotKeysModule.getShowHeader().isValue();
      float animatedOffset = showHeader ? headerHeight + 7.0F : 7.0F;

      for (Module module : this.keysList) {
         float animation = module.getAnimation().getOutput().floatValue();
         animatedOffset += animation * 11.0F;
      }

      return animatedOffset;
   }
}
