package padej.soup.implement.features.draggables.playerinfo;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;

public class BPSComponent implements PlayerInfoComponent, QuickImports {
   private float smoothedWidth = 0.0F;

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      float bpsValue = mc.player != null ? (float)(this.getSpeedSqrt(mc.player) * 20.0) : 0.0F;
      String bps = MathUtil.round(bpsValue, 0.1F) + " BPS";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setTexture("textures/bps.png")
            .render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, bps, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(bps);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      float bpsValue = mc.player != null ? (float)(this.getSpeedSqrt(mc.player) * 20.0) : 0.0F;
      String bps = MathUtil.round(bpsValue, 0.1F) + " BPS";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(bps);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      float targetWidth = this.getWidth(font, height);
      this.smoothedWidth = MathUtil.interpolate(this.smoothedWidth, targetWidth);
      return this.smoothedWidth;
   }

   @Override
   public String getName() {
      return "BPS";
   }

   private double getSpeedSqrt(Entity entity) {
      return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.prevX, entity.prevY, entity.prevZ)));
   }
}
