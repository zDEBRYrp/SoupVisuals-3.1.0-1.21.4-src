package padej.soup.implement.features.draggables.playerinfo;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;

public class XYZComponent implements PlayerInfoComponent, QuickImports {
   private BlockPos lastPos = BlockPos.ORIGIN;
   private float smoothedWidth = 0.0F;

   @Override
   public void tick() {
      if (mc.player != null) {
         this.lastPos = mc.player.getBlockPos();
      }
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      String xyz = this.lastPos.getX() + ", " + this.lastPos.getY() + ", " + this.lastPos.getZ();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setTexture("textures/xyz.png")
            .render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, xyz, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(xyz);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      String xyz = this.lastPos.getX() + ", " + this.lastPos.getY() + ", " + this.lastPos.getZ();
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(xyz);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      float targetWidth = this.getWidth(font, height);
      this.smoothedWidth = MathUtil.interpolate(this.smoothedWidth, targetWidth);
      return this.smoothedWidth;
   }

   @Override
   public String getName() {
      return "XYZ";
   }
}
