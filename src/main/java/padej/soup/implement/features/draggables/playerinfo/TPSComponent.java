package padej.soup.implement.features.draggables.playerinfo;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.world.ServerUtil;

public class TPSComponent implements PlayerInfoComponent, QuickImports {
   private float tpsValue = 20.0F;
   private float smoothedWidth = 0.0F;

   @Override
   public void tick() {
      this.tpsValue = MathUtil.interpolate(this.tpsValue, ServerUtil.TPS);
   }

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      String tps = MathUtil.round(this.tpsValue, 0.1F) + " TPS";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setTexture("textures/tps.png")
            .render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, tps, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(tps);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      String tps = MathUtil.round(this.tpsValue, 0.1F) + " TPS";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(tps);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      float targetWidth = this.getWidth(font, height);
      this.smoothedWidth = MathUtil.interpolate(this.smoothedWidth, targetWidth);
      return this.smoothedWidth;
   }

   @Override
   public String getName() {
      return "TPS";
   }
}
