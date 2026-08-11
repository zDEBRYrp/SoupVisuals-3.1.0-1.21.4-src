package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class MemoryComponent implements WatermarkComponent, QuickImports {
   private float smoothedWidth = 0.0F;

   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      Runtime runtime = Runtime.getRuntime();
      long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
      long maxMemory = runtime.maxMemory() / 1024L / 1024L;
      int percentage = (int)(usedMemory * 100L / maxMemory);
      String memoryText = percentage + "%";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         image.setTexture("textures/ram.png")
            .render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, memoryText, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(memoryText);
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      Runtime runtime = Runtime.getRuntime();
      long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L;
      long maxMemory = runtime.maxMemory() / 1024L / 1024L;
      int percentage = (int)(usedMemory * 100L / maxMemory);
      int digitCount = String.valueOf(percentage).length();
      String widestMemory = "8".repeat(digitCount) + "%";
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(widestMemory);
   }

   @Override
   public float getSmoothedWidth(FontRenderer font, float height) {
      return this.getWidth(font, height);
   }

   @Override
   public String getName() {
      return "Memory";
   }
}
