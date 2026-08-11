package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import padej.soup.api.system.shape.Shape;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;

public class Blur implements Shape, QuickImports {
   private final ShaderProgramKey SHADER_KEY = new ShaderProgramKey(Identifier.of("minecraft", "core/blur"), VertexFormats.POSITION, Defines.EMPTY);
   public Framebuffer input;
   public Vector2f resolution = new Vector2f();

   @Override
   public void render(ShapeProperties shape) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      float scale = (float)mc.getWindow().getScaleFactor();
      float alpha = RenderSystem.getShaderColor()[3];
      Matrix4f matrix4f = shape.getMatrix().peek().getPositionMatrix();
      Vector3f pos = matrix4f.transformPosition(shape.getX(), shape.getY(), 0.0F, new Vector3f()).mul(scale);
      Vector3f size = matrix4f.getScale(new Vector3f()).mul(scale);
      Vector4f round = shape.getRound().mul(size.y);
      float quality = shape.getQuality();
      float softness = shape.getSoftness();
      float thickness = shape.getThickness();
      float width = shape.getWidth() * size.x;
      float height = shape.getHeight() * size.y;
      BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION);
      drawEngine.quad(
         matrix4f, buffer, shape.getX() - softness / 2.0F, shape.getY() - softness / 2.0F, shape.getWidth() + softness, shape.getHeight() + softness
      );
      GlStateManager._activeTexture(33984);
      if (this.input != null) {
         RenderSystem.bindTexture(this.input.getColorAttachment());
      }

      ShaderProgram shader = RenderSystem.setShader(this.SHADER_KEY);
      if (shader != null) {
         shader.getUniformOrDefault("size").set(width, height);
         shader.getUniformOrDefault("location").set(pos.x, window.getHeight() - height - pos.y);
         shader.getUniformOrDefault("radius").set(round);
         shader.getUniformOrDefault("softness").set(softness);
         shader.getUniformOrDefault("thickness").set(thickness);
         shader.getUniformOrDefault("Quality").set(quality);
         shader.getUniformOrDefault("color1")
            .set(
               ColorUtil.redf(shape.getColor().x),
               ColorUtil.greenf(shape.getColor().x),
               ColorUtil.bluef(shape.getColor().x),
               ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().x, alpha))
            );
         shader.getUniformOrDefault("color2")
            .set(
               ColorUtil.redf(shape.getColor().y),
               ColorUtil.greenf(shape.getColor().y),
               ColorUtil.bluef(shape.getColor().y),
               ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().y, alpha))
            );
         shader.getUniformOrDefault("color3")
            .set(
               ColorUtil.redf(shape.getColor().z),
               ColorUtil.greenf(shape.getColor().z),
               ColorUtil.bluef(shape.getColor().z),
               ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().z, alpha))
            );
         shader.getUniformOrDefault("color4")
            .set(
               ColorUtil.redf(shape.getColor().w),
               ColorUtil.greenf(shape.getColor().w),
               ColorUtil.bluef(shape.getColor().w),
               ColorUtil.alphaf(ColorUtil.multAlpha(shape.getColor().w, alpha))
            );
         shader.getUniformOrDefault("outlineColor")
            .set(
               ColorUtil.redf(shape.getOutlineColor()),
               ColorUtil.greenf(shape.getOutlineColor()),
               ColorUtil.bluef(shape.getOutlineColor()),
               ColorUtil.alphaf(ColorUtil.multAlpha(shape.getOutlineColor(), alpha))
            );
         shader.getUniformOrDefault("InputResolution").set(this.resolution.x, this.resolution.y);
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.disableBlend();
      }
   }

   public void setup() {
      Framebuffer buffer = mc.getFramebuffer();
      if (this.input == null) {
         this.input = new SimpleFramebuffer(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), false);
      }

      this.input.beginWrite(false);
      buffer.draw(this.input.textureWidth, this.input.textureHeight);
      buffer.beginWrite(false);
      if (this.input != null
         && (this.input.textureWidth != mc.getWindow().getFramebufferWidth() || this.input.textureHeight != mc.getWindow().getFramebufferHeight())) {
         this.input.resize(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight());
      }

      this.resolution.set(buffer.textureWidth, buffer.textureHeight);
   }
}
