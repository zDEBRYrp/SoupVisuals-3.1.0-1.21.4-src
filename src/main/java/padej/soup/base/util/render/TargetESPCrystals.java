package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.joml.Vector4i;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.Main;
import padej.soup.implement.events.render.WorldRenderEvent;

public class TargetESPCrystals {
   public static final TargetESPCrystals instance = new TargetESPCrystals();
   private final MinecraftClient mc;
   private final List<TargetESPCrystals.Crystal> crystals;
   private float crackAmount;
   private float hitAnimation;
   private float lastHurtTime;
   private long lastUpdateTick;
   private static final boolean PULSE = true;
   private static final int NUM_SIDES = 4;
   private static final float SPEED = 3.0F;
   private static final boolean ROTATION = true;
   private static final float TRANSPARENCY = 0.8F;

   public TargetESPCrystals() {
      this.mc = Main.mc;
      this.crystals = new ArrayList<>();
      this.crackAmount = 0.0F;
      this.hitAnimation = 0.0F;
      this.lastHurtTime = 0.0F;
      this.lastUpdateTick = -1L;
   }

   public void onRenderWorldEvent(
      WorldRenderEvent event,
      LivingEntity target,
      float distance,
      float size,
      boolean glow,
      float glowSize,
      boolean horizontal,
      float anim,
      float red,
      int[] customColors
   ) {
      if (target != null && target.isAlive()) {
         if (this.crystals.isEmpty()) {
            this.createCrystals(target, distance, size);
         }

         this.updateAnimations(target);
         this.renderCrystals(event, target, anim, red, distance, size, glow, glowSize, horizontal, customColors);
      }
   }

   private void updateAnimations(LivingEntity target) {
      if (target != null) {
         long currentTick = this.mc.world != null ? this.mc.world.getTime() : 0L;
         if (this.lastUpdateTick != currentTick) {
            this.lastUpdateTick = currentTick;
            if (target.isAlive()) {
               this.crackAmount = 1.0F - target.getHealth() / target.getMaxHealth();
            } else if (this.crackAmount < 1.0F) {
               this.crackAmount += 0.05F;
            }

            this.crackAmount = MathHelper.clamp(this.crackAmount, 0.0F, 1.0F);
            if (target.hurtTime > this.lastHurtTime) {
               this.hitAnimation = 1.0F;
            }

            this.lastHurtTime = target.hurtTime;
            if (this.hitAnimation > 0.0F) {
               this.hitAnimation -= 0.04F;
            }

            this.hitAnimation = MathHelper.clamp(this.hitAnimation, 0.0F, 1.0F);
         }
      }
   }

   private void renderCrystals(
      WorldRenderEvent event,
      LivingEntity target,
      float anim,
      float red,
      float distance,
      float size,
      boolean glow,
      float glowSize,
      boolean horizontal,
      int[] customColors
   ) {
      if (!(anim <= 0.0F) && event != null) {
         if (RenderSystem.isOnRenderThread()) {
            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            Vec3d targetPos = MathUtil.interpolate(target);
            boolean canSee = this.mc.player != null && this.mc.player.canSee(target);
            if (anim > 0.01F && !this.crystals.isEmpty()) {
               for (int i = 0; i < this.crystals.size(); i++) {
                  TargetESPCrystals.Crystal crystal = this.crystals.get(i);
                  this.renderCrystal(event, crystal, target, targetPos, canSee, i, anim, red, distance, size, glow, glowSize, horizontal, customColors);
               }
            }

            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         }
      }
   }

   private void createCrystals(LivingEntity target, float distance, float size) {
      this.crystals.clear();
      float targetHeight = target.getHeight();
      float targetWidth = target.getWidth();
      float topCrystalY = targetHeight * 0.6F;
      float bottomCrystalY = -targetHeight * 0.1F;
      this.crystals.add(new TargetESPCrystals.Crystal(new Vector3f(0.0F, topCrystalY, 0.0F), new Vector3f(0.0F, 0.0F, 0.0F), size));
      this.crystals.add(new TargetESPCrystals.Crystal(new Vector3f(0.0F, bottomCrystalY, 0.0F), new Vector3f(0.0F, 0.0F, 0.0F), size));

      for (int i = 0; i < 17; i++) {
         float angle = (float)(i * 2 * Math.PI / 17.0);
         float radius = distance * 2.0F * (targetWidth / 0.6F);
         float heightBase = targetHeight * 0.5F;
         float height = heightBase + (float)Math.sin(angle * 3.0F) * (targetHeight * 0.45F);
         Vector3f position = new Vector3f((float)(Math.cos(angle) * radius), height, (float)(Math.sin(angle) * radius));
         Vector3f rotation = new Vector3f((float)(Math.sin(angle) * 30.0), angle * 180.0F / (float) Math.PI, (float)(Math.cos(angle) * 30.0));
         this.crystals.add(new TargetESPCrystals.Crystal(position, rotation, size));
      }
   }

   private void renderCrystal(
      WorldRenderEvent event,
      TargetESPCrystals.Crystal crystal,
      LivingEntity target,
      Vec3d targetPos,
      boolean canSee,
      int crystalIndex,
      float anim,
      float red,
      float distance,
      float size,
      boolean glow,
      float glowSize,
      boolean horizontal,
      int[] customColors
   ) {
      if (event != null && crystal != null) {
         MatrixStack eventStack = event.getStack();
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.peek().getPositionMatrix().set(eventStack.peek().getPositionMatrix());
         matrixStack.peek().getPositionMatrix().setTranslation(0.0F, 0.0F, 0.0F);
         matrixStack.peek().getNormalMatrix().set(eventStack.peek().getNormalMatrix());
         Vec3d cameraPos = this.mc.getEntityRenderDispatcher().camera.getPos();
         Vec3d localTargetPos = targetPos.subtract(cameraPos);
         float scale = crystal.size * anim;
         if (this.hitAnimation > 0.0F) {
            scale *= 1.0F + this.hitAnimation * 0.5F;
         }

         if (this.crackAmount > 0.0F) {
            scale *= 1.0F - this.crackAmount * 0.3F;
         }

         float sharedCircleStep = TargetRenderer.getInterpolatedCircleStep();
         float pulseAmount = (float)Math.sin(sharedCircleStep * 2.0) * 0.1F;
         scale *= 1.0F + pulseAmount;
         matrixStack.push();
         float targetHeight = target.getHeight();
         float targetWidth = target.getWidth();
         double cs = sharedCircleStep * 3.0F * 0.5F;
         float angle = (float)(crystalIndex * 2 * Math.PI / this.crystals.size());
         float radius = this.getAnimatedDistance(anim, distance) * (targetWidth / 0.6F);
         float heightBase = targetHeight * 0.5F;
         float height = heightBase + (float)Math.sin(angle * 3.0F) * (targetHeight * 0.45F);
         float cos = (float)Math.cos(-cs);
         float sin = (float)Math.sin(-cs);
         float baseX = (float)(Math.cos(angle) * radius);
         float baseZ = (float)(Math.sin(angle) * radius);
         pulseAmount = baseX * cos - baseZ * sin;
         float finalZ = baseX * sin + baseZ * cos;
         float finalY = height;
         matrixStack.translate(localTargetPos.x + pulseAmount, localTargetPos.y + finalY, localTargetPos.z + finalZ);
         float angleToPlayer = (float)Math.atan2(pulseAmount, finalZ);
         matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angleToPlayer * 180.0F / (float) Math.PI));
         if (!horizontal) {
            float distanceFromCenter = (float)Math.sqrt(pulseAmount * pulseAmount + finalZ * finalZ);
            angle = (float)Math.atan2(finalY, distanceFromCenter) * 180.0F / (float) Math.PI;
            radius = targetHeight * 0.5F;
            if (finalY > radius) {
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-angle - 45.0F));
            } else {
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-angle));
            }
         } else {
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
         }

         matrixStack.scale(size, size, size);
         int color = this.getColorByPosition(crystal.position, customColors, crystalIndex);
         if (red > 0.0F) {
            color = ColorUtil.gradientToRed(color, red);
         }

         if (anim < 1.0F) {
            angle = this.smoothStepEaseInOut(anim);
            int alpha = (int)(255.0F * angle);
            heightBase = 0.3F + 0.7F * angle;
            color = ColorUtil.multDark(color, heightBase);
            color = color & 16777215 | alpha << 24;
         }

         this.drawCrystal(matrixStack, color, 4, canSee, size, anim);
         if (glow) {
            Vec3d crystalWorldPos = new Vec3d(targetPos.x + pulseAmount, targetPos.y + finalY, targetPos.z + finalZ);
            this.drawCrystalGlow(matrixStack, color, event, crystalWorldPos, size, glowSize, 0.8F);
         }

         matrixStack.pop();
      }
   }

   private void drawCrystal(MatrixStack matrixStack, int color, int sides, boolean canSee, float crystalSize, float anim) {
      if (canSee) {
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
      } else {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder bufferBuilder = tessellator.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      float s = crystalSize;
      float h_pyramid = s * 1.2F;
      float base_radius = s * 0.8F;
      List<Vector3f> topVertices = new ArrayList<>();
      List<Vector3f> bottomVertices = new ArrayList<>();

      for (int i = 0; i < sides; i++) {
         float angle = (float)((Math.PI * 2) * i / sides);
         float x = (float)(base_radius * Math.cos(angle));
         float z = (float)(base_radius * Math.sin(angle));
         topVertices.add(new Vector3f(x, 0.0F, z));
         bottomVertices.add(new Vector3f(x, 0.0F, z));
      }

      Vector3f vTop = new Vector3f(0.0F, h_pyramid, 0.0F);
      Vector3f vBottom = new Vector3f(0.0F, -h_pyramid, 0.0F);

      for (int i = 0; i < sides; i++) {
         Vector3f v1 = topVertices.get(i);
         Vector3f v2 = topVertices.get((i + 1) % sides);
         float gradientFactor = (float)i / sides;
         int topColor = this.getFaceColor(color, gradientFactor, 0);
         this.drawTriangle(bufferBuilder, matrixStack, vTop, v1, v2, topColor, anim);
         Vector3f v3 = bottomVertices.get(i);
         Vector3f v4 = bottomVertices.get((i + 1) % sides);
         int bottomColor = this.getFaceColor(color, gradientFactor, 1);
         this.drawTriangle(bufferBuilder, matrixStack, vBottom, v4, v3, bottomColor, anim);
      }

      for (int i = 0; i < sides; i++) {
         Vector3f v1 = topVertices.get(i);
         Vector3f v2 = topVertices.get((i + 1) % sides);
         Vector3f v3 = bottomVertices.get(i);
         Vector3f v4 = bottomVertices.get((i + 1) % sides);
         float sideGradient = (float)i / sides;
         int sideColor = this.getFaceColor(color, sideGradient, 2);
         this.drawTriangle(bufferBuilder, matrixStack, v1, v2, v3, sideColor, anim);
         this.drawTriangle(bufferBuilder, matrixStack, v2, v4, v3, sideColor, anim);
      }

      try {
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      } catch (Exception var24) {
      }

      RenderSystem.enableCull();
      if (canSee) {
         RenderSystem.disableDepthTest();
      } else {
         RenderSystem.enableDepthTest();
      }

      RenderSystem.depthMask(true);
   }

   private float smoothStepEaseInOut(float t) {
      return t < 0.5F ? 4.0F * t * t * t : 1.0F - (float)Math.pow(-2.0F * t + 2.0F, 3.0) / 2.0F;
   }

   private float getAnimatedDistance(float anim, float crystalDistance) {
      float maxDistance = crystalDistance * 2.0F;
      float minDistance = crystalDistance;
      float smoothAnim = this.smoothStepEaseInOut(1.0F - anim);
      return minDistance + (maxDistance - minDistance) * smoothAnim;
   }

   private int getColorByPosition(Vector3f position, int[] customColors, int crystalIndex) {
      if (customColors != null && customColors.length != 0) {
         float sharedCircleStep = TargetRenderer.getInterpolatedCircleStep();
         float anglePos = (float)crystalIndex / this.crystals.size();
         float flowSpeed = 0.3F;
         float flowOffset = sharedCircleStep * flowSpeed % 1.0F;
         float gradientPos = (anglePos + flowOffset) % 1.0F;
         if (customColors.length == 1) {
            return customColors[0];
         }

         float scaledPos = gradientPos * customColors.length;
         int colorIndex1 = (int)scaledPos % customColors.length;
         int colorIndex2 = (colorIndex1 + 1) % customColors.length;
         float blend = scaledPos - (int)scaledPos;
         blend = (float)(Math.sin((blend - 0.5F) * Math.PI) * 0.5 + 0.5);
         return this.lerpColor(customColors[colorIndex1], customColors[colorIndex2], blend);
      } else {
         return ColorUtil.getClientColor();
      }
   }

   private int lerpColor(int color1, int color2, float t) {
      int a1 = color1 >> 24 & 0xFF;
      int r1 = color1 >> 16 & 0xFF;
      int g1 = color1 >> 8 & 0xFF;
      int b1 = color1 & 0xFF;
      int a2 = color2 >> 24 & 0xFF;
      int r2 = color2 >> 16 & 0xFF;
      int g2 = color2 >> 8 & 0xFF;
      int b2 = color2 & 0xFF;
      int a = (int)(a1 + (a2 - a1) * t);
      int r = (int)(r1 + (r2 - r1) * t);
      int g = (int)(g1 + (g2 - g1) * t);
      int b = (int)(b1 + (b2 - b1) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private int getFaceColor(int baseColor, float gradientFactor, int faceType) {
      int r = baseColor >> 16 & 0xFF;
      int g = baseColor >> 8 & 0xFF;
      int b = baseColor & 0xFF;
      if (faceType == 0) {
         r = Math.min(255, (int)(r * (1.5F + gradientFactor * 0.8F)));
         g = Math.max(0, (int)(g * (0.6F + gradientFactor * 0.4F)));
         b = Math.max(0, (int)(b * (0.4F - gradientFactor * 0.2F)));
      } else if (faceType == 1) {
         r = Math.max(0, (int)(r * (0.4F - gradientFactor * 0.2F)));
         g = Math.max(0, (int)(g * (0.5F + gradientFactor * 0.3F)));
         b = Math.min(255, (int)(b * (1.5F + gradientFactor * 0.8F)));
      } else {
         r = Math.max(0, (int)(r * (0.7F + Math.sin(gradientFactor * Math.PI * 2.0) * 0.3F)));
         g = Math.min(255, (int)(g * (1.3F + Math.cos(gradientFactor * Math.PI * 2.0) * 0.5)));
         b = Math.max(0, (int)(b * (0.6F + Math.sin(gradientFactor * Math.PI * 3.0) * 0.2F)));
      }

      float brightness = 0.7F + 0.3F * (float)Math.sin(gradientFactor * Math.PI * 4.0);
      r = (int)(r * brightness);
      g = (int)(g * brightness);
      b = (int)(b * brightness);
      r = Math.max(0, Math.min(255, r));
      g = Math.max(0, Math.min(255, g));
      b = Math.max(0, Math.min(255, b));
      return 0xFF000000 | r << 16 | g << 8 | b;
   }

   private void drawCrystalGlow(
      MatrixStack matrixStack, int color, WorldRenderEvent event, Vec3d crystalWorldPos, float crystalSize, float glowSize, float transparency
   ) {
      Camera camera = this.mc.getEntityRenderDispatcher().camera;
      Vec3d vec = crystalWorldPos.subtract(camera.getPos());
      MatrixStack billboardStack = new MatrixStack();
      billboardStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      billboardStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
      billboardStack.translate(vec.x, vec.y, vec.z);
      billboardStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
      billboardStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
      Entry entry = billboardStack.peek().copy();
      float size = crystalSize * glowSize;
      int glowColor = ColorUtil.multAlpha(color, transparency);
      Vector4i colorVec = new Vector4i(glowColor);
      Identifier glowTexture = Identifier.of("textures/particles/bloom/bloom_soft.png");
      Render3DUtil.drawTexture(entry, glowTexture, -size / 2.0F, -size / 2.0F, size, size, colorVec, true);
   }

   private void drawTriangle(BufferBuilder bufferBuilder, MatrixStack matrixStack, Vector3f v1, Vector3f v2, Vector3f v3, int color, float anim) {
      Entry entry = matrixStack.peek();
      Vector3f p1 = entry.getPositionMatrix().transformPosition(v1.x, v1.y, v1.z, new Vector3f());
      Vector3f p2 = entry.getPositionMatrix().transformPosition(v2.x, v2.y, v2.z, new Vector3f());
      Vector3f p3 = entry.getPositionMatrix().transformPosition(v3.x, v3.y, v3.z, new Vector3f());
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      float a = anim;
      bufferBuilder.vertex(p1.x, p1.y, p1.z).color(r, g, b, a);
      bufferBuilder.vertex(p2.x, p2.y, p2.z).color(r, g, b, a);
      bufferBuilder.vertex(p3.x, p3.y, p3.z).color(r, g, b, a);
   }

   public void reset() {
      this.crackAmount = 0.0F;
      this.hitAnimation = 0.0F;
      this.lastHurtTime = 0.0F;
      this.crystals.clear();
   }

   private static class Crystal {
      final Vector3f position;
      final Vector3f rotation;
      final float size;

      Crystal(Vector3f position, Vector3f rotation, float size) {
         this.position = position;
         this.rotation = rotation;
         this.size = size;
      }
   }
}
