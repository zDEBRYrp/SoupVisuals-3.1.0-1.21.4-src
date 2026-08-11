package padej.soup.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.boss.BossBar.Color;
import org.joml.Vector4f;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;

public class BossBars extends AbstractDraggable {
   public BossBars() {
      super("BossBars", 0, 0, 0, 0, false);
   }

   @Override
   public boolean visible() {
      return !mc.inGameHud.getBossBarHud().bossBars.isEmpty();
   }

   @Override
   public void drawDraggable(DrawContext context) {
      this.setX(mc.getWindow().getScaledWidth() / 2);
      MatrixStack matrix = context.getMatrices();
      float y = 10.0F;
      float width = 156.0F;
      float height = 3.5F;
      FontRenderer font = Fonts.getSize(18);

      for (ClientBossBar bossInfo : mc.inGameHud.getBossBarHud().bossBars.values()) {
         Vector4f rounds = bossInfo.getPercent() != 1.0F ? new Vector4f(0.0F, 0.0F, height / 2.0F, height / 2.0F) : new Vector4f(height / 2.0F);
         int color = this.getColor(bossInfo.getColor());
         rectangle.render(
            ShapeProperties.create(matrix, this.getX() - width / 2.0F, y + 10.0F, width, height).color(ColorUtil.getRect(0.8F)).round(1.75F).build()
         );
         rectangle.render(
            ShapeProperties.create(matrix, this.getX() - width / 2.0F, y + 10.0F, width, height).color(ColorUtil.multAlpha(color, 0.2F)).round(1.75F).build()
         );
         rectangle.render(
            ShapeProperties.create(matrix, this.getX() - width / 2.0F, y + 10.0F, width * bossInfo.getPercent(), height)
               .color(ColorUtil.multAlpha(color, 0.8F))
               .round(rounds)
               .build()
         );
         font.drawText(matrix, bossInfo.getName(), (int)(this.getX() - font.getStringWidth(bossInfo.getName()) / 2.0F), y);
         y += 22.0F;
      }
   }

   public int getColor(Color color) {
      return switch (color) {
         case PINK -> new java.awt.Color(16734900).getRGB();
         case PURPLE -> new java.awt.Color(8469759).getRGB();
         case RED -> new java.awt.Color(16725815).getRGB();
         case BLUE -> new java.awt.Color(41215).getRGB();
         case GREEN -> new java.awt.Color(5635925).getRGB();
         default -> new java.awt.Color(color.getTextFormat().getColorValue()).getRGB();
      };
   }
}
