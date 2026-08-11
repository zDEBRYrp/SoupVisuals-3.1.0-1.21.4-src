package padej.soup.implement.features.draggables.watermark;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.implement.features.modules.hud.Watermark;

public class ClientIconComponent implements WatermarkComponent, QuickImports {
   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      float imgScale = height - 2.0F;
      float offset = (height - imgScale) / 2.0F;
      boolean darkBg = ColorUtil.isDarkBackground();
      if (Watermark.getInstance().getShowBackground().isValue()) {
         blur.render(
            ShapeProperties.create(matrix, x, y, height, height)
               .round(3.0F)
               .softness(1.0F)
               .thickness(2.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
      }

      float iconX = x + offset;
      float iconY = y + offset + 0.5F;
      RenderSystem.enableBlend();
      int color;
      if (darkBg) {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShaderTexture(0, Identifier.of("textures/particles/soupapi_3.png"));
         color = ColorUtil.fade(50);
      } else {
         RenderSystem.setShaderTexture(0, Identifier.of("textures/particles/soupapi_2.png"));
         color = ColorUtil.getText();
      }

      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      matrix.push();
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      buffer.vertex(matrix4f, iconX, iconY + imgScale, 0.0F).texture(0.0F, 1.0F).color(ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), 0.5F));
      buffer.vertex(matrix4f, iconX + imgScale, iconY + imgScale, 0.0F).texture(1.0F, 1.0F).color(ColorUtil.multAlpha(ColorUtil.multDark(color, 0.4F), 0.5F));
      buffer.vertex(matrix4f, iconX + imgScale, iconY, 0.0F).texture(1.0F, 0.0F).color(color);
      buffer.vertex(matrix4f, iconX, iconY, 0.0F).texture(0.0F, 0.0F).color(color);
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      matrix.pop();
      return height;
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      return height;
   }

   @Override
   public String getName() {
      return "Icon";
   }
}
