package padej.soup.api.system.shape.implement;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL20;
import padej.soup.api.system.shape.Shape;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;

public class Image implements Shape, QuickImports {
   private String texture;

   @Override
   public void render(ShapeProperties shape) {
      MatrixStack matrix = shape.getMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderTexture(0, Identifier.of(this.texture));
      GL20.glTexParameteri(3553, 10241, 9729);
      GL20.glTexParameteri(3553, 10240, 9729);
      float width = shape.getWidth();
      float x = shape.getX() + width;
      float y = shape.getY();
      matrix.push();
      matrix.translate(x, y, 0.0F);
      matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(shape.getRotation()));
      matrix.translate(-x, -y, 0.0F);
      drawEngine.quad(matrix.peek().getPositionMatrix(), x, y, shape.getHeight(), width, shape.getColor().x);
      matrix.pop();
      RenderSystem.disableBlend();
   }

   public Image setTexture(String texture) {
      this.texture = texture;
      return this;
   }
}
