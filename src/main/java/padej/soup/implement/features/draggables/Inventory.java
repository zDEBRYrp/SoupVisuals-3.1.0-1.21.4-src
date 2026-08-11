package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.implement.features.modules.hud.Icons;

public class Inventory extends AbstractDraggable {
   private final List<ItemStack> stacks = new ArrayList<>(27);

   public Inventory() {
      super("Inventory", 390, 10, 123, 60, true);
   }

   @Override
   public boolean visible() {
      for (ItemStack stack : this.stacks) {
         if (!stack.isEmpty()) {
            return true;
         }
      }

      return PlayerIntersectionUtil.isChatOrMenu(mc.currentScreen);
   }

   @Override
   public void tick() {
      this.stacks.clear();
      if (mc.player != null) {
         for (int i = 9; i < 36; i++) {
            this.stacks.add(mc.player.inventory.getStack(i));
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      padej.soup.implement.features.modules.hud.Inventory inventoryModule = padej.soup.implement.features.modules.hud.Inventory.getInstance();
      float headerHeight = 16.0F;
      float padding = 5.0F;
      boolean showHeader = inventoryModule.getShowHeader().isValue();
      boolean darkenHeader = inventoryModule.getDarkenHeader().isValue();
      int itemsPerRow = 9;
      int totalItems = 27;
      int itemSize = 13;
      int itemPadding = 4;
      int rows = (int)Math.ceil((double)totalItems / itemsPerRow);
      float contentHeight = rows * itemSize + itemPadding * 2;
      float totalHeight = showHeader ? headerHeight + contentHeight : contentHeight;
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
            ShapeProperties.create(matrix, this.getX(), this.getY() + headerHeight, this.getWidth(), contentHeight)
               .quality(25.0F)
               .round(0.0F, 4.0F, 0.0F, 4.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
      } else {
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), contentHeight)
               .quality(25.0F)
               .round(4.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
      }

      if (showHeader) {
         Fonts.getSize(15, Fonts.Type.INTER_DEFAULT)
            .drawString(matrix, this.getName(), (int)(this.getX() + padding), (int)(this.getY() + 6.5F), ColorUtil.getText());
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

         image.setTexture("textures/inv.png")
            .render(
               ShapeProperties.create(matrix, this.getX() + this.getWidth() - iconSize - iconPadding, this.getY() + 4, iconSize, iconSize)
                  .color(iconColor)
                  .build()
            );
      }

      int offsetY = showHeader ? (int)(headerHeight + itemPadding) : itemPadding;
      int offsetX = itemPadding;

      for (ItemStack stack : this.stacks) {
         Render2DUtil.defaultDrawStack(context, stack, this.getX() + offsetX, this.getY() + offsetY, false, true, 0.5F);
         offsetX += itemSize;
         if (offsetX > this.getWidth() - itemSize) {
            offsetY += itemSize;
            offsetX = itemPadding;
         }
      }

      this.setHeight((int)totalHeight);
   }
}
