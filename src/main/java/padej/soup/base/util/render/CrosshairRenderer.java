package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters;
import net.minecraft.client.render.RenderPhase.Transparency;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Matrix4f;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.animation.Interpolation;
import padej.soup.base.util.animation.Interpolations;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.features.modules.hud.CrossHair;

public final class CrosshairRenderer implements QuickImports {
   private static final RenderLayer CROSSHAIR_INVERT_LAYER = RenderLayer.of(
      "crosshair_invert",
      VertexFormats.POSITION_COLOR,
      DrawMode.QUADS,
      1536,
      MultiPhaseParameters.builder().program(RenderPhase.POSITION_COLOR_PROGRAM).transparency(new Transparency("crosshair_transparency", () -> {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
      }, () -> {
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      })).build(false)
   );
   private static final RenderLayer CROSSHAIR_INVERT_TRIANGLES = RenderLayer.of(
      "crosshair_invert_triangles",
      VertexFormats.POSITION_COLOR,
      DrawMode.TRIANGLES,
      1536,
      MultiPhaseParameters.builder().program(RenderPhase.POSITION_COLOR_PROGRAM).transparency(new Transparency("crosshair_transparency_tri", () -> {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(SrcFactor.ONE_MINUS_DST_COLOR, DstFactor.ONE_MINUS_SRC_COLOR, SrcFactor.ONE, DstFactor.ZERO);
      }, () -> {
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
      })).build(false)
   );
   private static float colorAnimation = 0.0F;
   private static float hitAnimation = 0.0F;
   private static float lastAttackProgress = 1.0F;
   private static long lastBlobUpdateTime = System.currentTimeMillis();

   public static void render(CrossHair module) {
      float colorSpeed = module.getStyleSetting().getSelected().equals("Blob") ? module.getColorAnimationSpeedSetting().getValue() : 2.0F;
      colorAnimation = MathUtil.interpolateSmooth(colorSpeed, colorAnimation, mc.crosshairTarget instanceof EntityHitResult ? 1.0F : 0.0F);
      int mainColor = getCrosshairColor(module);
      float centerX = window.getScaledWidth() / 2.0F;
      float centerY = window.getScaledHeight() / 2.0F;
      if (module.getDynamicSetting().isValue()) {
         updateDynamicPosition(module, centerX, centerY);
         centerX = module.xAnim;
         centerY = module.yAnim;
      } else {
         module.xAnim = centerX;
         module.yAnim = centerY;
      }

      float cooldown = module.getAttackSetting().getInt()
         - module.getAttackSetting().getInt() * mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false));
      String style = module.getStyleSetting().getSelected();
      if ("Cross".equals(style)) {
         renderCross(module, centerX, centerY, cooldown, mainColor);
      } else if ("Blob".equals(style)) {
         renderBlob(module, centerX, centerY, mainColor);
      } else if ("Ring".equals(style)) {
         renderRing(module, centerX, centerY, mainColor);
      }
   }

   private static void updateDynamicPosition(CrossHair module, float midX, float midY) {
      float yawDelta = mc.player.prevHeadYaw - mc.player.getHeadYaw();
      float pitchDelta = module.prevPitch - mc.player.getPitch();
      module.prevPitch = mc.player.getPitch();
      float invertMultiplier = module.getDynamicInvertSetting().isValue() ? -1.0F : 1.0F;
      float speed = module.getDynamicSpeedSetting().getValue() * 0.05F;
      module.xAnim += yawDelta * speed * invertMultiplier;
      module.yAnim += pitchDelta * speed * invertMultiplier;
      float offsetX = module.xAnim - midX;
      float offsetY = module.yAnim - midY;
      double distance = Math.sqrt(offsetX * offsetX + offsetY * offsetY);
      float range = module.getDynamicRangeSetting().getInt();
      double attractionFactor = Math.min(distance / range, 1.0);
      float maxSmoothingFactor = module.getDynamicBackSpeedSetting().getValue() * 0.1F;
      float smoothingFactor = (float)(maxSmoothingFactor * attractionFactor);
      smoothingFactor = Math.min(smoothingFactor, 0.8F);
      module.xAnim = module.xAnim + (midX - module.xAnim) * smoothingFactor;
      module.yAnim = module.yAnim + (midY - module.yAnim) * smoothingFactor;
   }

   private static void renderCross(CrossHair module, float centerX, float centerY, float cooldown, int mainColor) {
      float width = module.getSize1Setting().getValue();
      float height = module.getSize2Setting().getValue();
      float offset = height / 2.0F;
      float indent = module.getIndentSetting().getInt() + cooldown;
      if (module.getInvertColorSetting().isValue()) {
         MatrixStack matrices = new MatrixStack();
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
         VertexConsumer vertexConsumer = immediate.getBuffer(CROSSHAIR_INVERT_LAYER);
         int white = -1;
         renderQuad(vertexConsumer, matrix, centerX - offset, centerY - width - indent, height, width, white);
         renderQuad(vertexConsumer, matrix, centerX - offset, centerY + indent, height, width, white);
         renderQuad(vertexConsumer, matrix, centerX - width - indent, centerY - offset, width, height, white);
         renderQuad(vertexConsumer, matrix, centerX + indent, centerY - offset, width, height, white);
         immediate.draw();
      } else {
         int outlineColor = ColorUtil.BLACK;
         renderCrosshairLines(centerX, centerY, width, height, 1.0F, indent, offset, outlineColor);
         renderCrosshairLines(centerX, centerY, width, height, 0.0F, indent, offset, mainColor);
      }
   }

   private static void renderQuad(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, int color) {
      buffer.vertex(matrix, x, y, 0.0F).color(color);
      buffer.vertex(matrix, x, y + height, 0.0F).color(color);
      buffer.vertex(matrix, x + width, y + height, 0.0F).color(color);
      buffer.vertex(matrix, x + width, y, 0.0F).color(color);
   }

   private static void renderBlob(CrossHair module, float centerX, float centerY, int mainColor) {
      DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
      MatrixStack matrix = context.getMatrices();
      float attackSizeMultiplier = module.getAttackSizeMultiplierSetting().getValue();
      float entityWidthMultiplier = module.getEntityWidthMultiplierSetting().getValue();
      float entityHeightMultiplier = module.getEntityHeightMultiplierSetting().getValue();
      float hitStretchMultiplier = module.getHitStretchMultiplierSetting().getValue();
      float hitDetectionThreshold = 0.9F;
      float hitResetThreshold = 0.1F;
      float outlineThickness = module.getOutlineThicknessSetting().getValue();
      float fillMaxAlpha = module.getFillMaxAlphaSetting().getValue();
      boolean enableFill = module.getEnableFillSetting().isValue();
      float baseSize = module.getBlobSizeSetting().getValue();
      float thickness = module.getBlobThicknessSetting().getValue();
      float thicknessFactor = Math.max(0.1F, thickness / 2.0F);
      float attackProgress = 1.0F - mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false));
      float attackProgressDelta = attackProgress - lastAttackProgress;
      boolean attackedThisFrame = attackProgressDelta > 0.35F || lastAttackProgress < hitResetThreshold && attackProgress > hitDetectionThreshold;
      if (attackedThisFrame && mc.crosshairTarget instanceof EntityHitResult) {
         hitAnimation = 1.0F;
      }

      lastAttackProgress = attackProgress;
      long currentTime = System.currentTimeMillis();
      float deltaMs = Math.max(1.0F, Math.min(100.0F, (float)(currentTime - lastBlobUpdateTime)));
      lastBlobUpdateTime = currentTime;
      float hitSpeed = module.getHitAnimationSpeedSetting().getValue();
      float hitDecay = (float)Math.exp(-hitSpeed * deltaMs * 0.0035F);
      hitAnimation *= hitDecay;
      if (hitAnimation < 0.001F) {
         hitAnimation = 0.0F;
      }

      Interpolation interpolation = Interpolations.getByName(module.getAnimationTypeSetting().getSelected());
      float easedProgress = (float)interpolation.interpolate(attackProgress);
      float animatedSize = baseSize + baseSize * attackSizeMultiplier * easedProgress;
      float width = animatedSize * (1.0F + colorAnimation * entityWidthMultiplier);
      float height = baseSize * thicknessFactor * (1.0F - colorAnimation * entityHeightMultiplier);
      float rounded = height / 2.0F;
      float hitMultiplier = 1.0F + hitAnimation * hitStretchMultiplier;
      width *= hitMultiplier;
      height /= hitMultiplier;
      int fillColor = enableFill ? ColorUtil.replAlpha(mainColor, easedProgress * fillMaxAlpha) : ColorUtil.replAlpha(mainColor, 0);
      rectangle.render(
         ShapeProperties.create(matrix, centerX - width / 2.0F, centerY - height / 2.0F, width, height)
            .round(rounded)
            .thickness(outlineThickness)
            .outlineColor(mainColor)
            .color(fillColor)
            .build()
      );
   }

   private static void renderEllipse(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, int color) {
      int segments = 64;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      float radiusX = width / 2.0F;
      float radiusY = height / 2.0F;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float)((Math.PI * 2) * i / segments);
         float angle2 = (float)((Math.PI * 2) * (i + 1) / segments);
         float x1 = centerX + (float)Math.cos(angle1) * radiusX;
         float y1 = centerY + (float)Math.sin(angle1) * radiusY;
         float x2 = centerX + (float)Math.cos(angle2) * radiusX;
         float y2 = centerY + (float)Math.sin(angle2) * radiusY;
         buffer.vertex(matrix, centerX, centerY, 0.0F).color(color);
         buffer.vertex(matrix, x1, y1, 0.0F).color(color);
         buffer.vertex(matrix, x2, y2, 0.0F).color(color);
      }
   }

   private static void renderRoundedRect(VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, float radius, int color) {
      float maxRadius = Math.min(width, height) / 2.0F;
      radius = Math.min(radius, maxRadius);
      if (radius <= 0.0F) {
         renderQuad(buffer, matrix, x, y, width, height, color);
      } else {
         float innerX = x + radius;
         float innerY = y + radius;
         float innerWidth = width - radius * 2.0F;
         float innerHeight = height - radius * 2.0F;
         if (innerWidth > 0.0F) {
            renderQuad(buffer, matrix, innerX, y, innerWidth, height, color);
         }

         if (innerHeight > 0.0F) {
            renderQuad(buffer, matrix, x, innerY, radius, innerHeight, color);
            renderQuad(buffer, matrix, innerX + innerWidth, innerY, radius, innerHeight, color);
         }

         int cornerSegments = 16;
         renderCorner(buffer, matrix, innerX, innerY, radius, 180.0F, 270.0F, cornerSegments, color);
         renderCorner(buffer, matrix, innerX + innerWidth, innerY, radius, 270.0F, 360.0F, cornerSegments, color);
         renderCorner(buffer, matrix, innerX + innerWidth, innerY + innerHeight, radius, 0.0F, 90.0F, cornerSegments, color);
         renderCorner(buffer, matrix, innerX, innerY + innerHeight, radius, 90.0F, 180.0F, cornerSegments, color);
      }
   }

   private static void renderCorner(
      VertexConsumer buffer, Matrix4f matrix, float centerX, float centerY, float radius, float startAngle, float endAngle, int segments, int color
   ) {
      float angleStep = (endAngle - startAngle) / segments;

      for (int i = 0; i < segments; i++) {
         float angle1 = (float)Math.toRadians(startAngle + i * angleStep);
         float angle2 = (float)Math.toRadians(startAngle + (i + 1) * angleStep);
         float x1 = centerX + (float)Math.cos(angle1) * radius;
         float y1 = centerY + (float)Math.sin(angle1) * radius;
         float x2 = centerX + (float)Math.cos(angle2) * radius;
         float y2 = centerY + (float)Math.sin(angle2) * radius;
         buffer.vertex(matrix, centerX, centerY, 0.0F).color(color);
         buffer.vertex(matrix, x1, y1, 0.0F).color(color);
         buffer.vertex(matrix, x2, y2, 0.0F).color(color);
         buffer.vertex(matrix, centerX, centerY, 0.0F).color(color);
      }
   }

   private static void renderCrosshairLines(float x, float y, float width, float height, float padding, float indent, float offset, int color) {
      Render2DUtil.drawQuad(x - offset - padding / 2.0F, y - width - indent - padding / 2.0F, height + padding, width + padding, color);
      Render2DUtil.drawQuad(x - offset - padding / 2.0F, y + indent - padding / 2.0F, height + padding, width + padding, color);
      Render2DUtil.drawQuad(x - width - indent - padding / 2.0F, y - offset - padding / 2.0F, width + padding, height + padding, color);
      Render2DUtil.drawQuad(x + indent - padding / 2.0F, y - offset - padding / 2.0F, width + padding, height + padding, color);
   }

   private static void renderRing(CrossHair module, float centerX, float centerY, int mainColor) {
      DrawContext context = new DrawContext(mc, mc.getBufferBuilders().getEntityVertexConsumers());
      MatrixStack matrix = context.getMatrices();
      float ringSize = module.getRingSizeSetting().getValue();
      float thickness = module.getRingThicknessSetting().getValue();
      float roundness = module.getRingRoundnessSetting().getValue();
      float attackProgress = mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false)) * 360.0F;
      int color1 = getRingVertexColor(module, 0);
      int color2 = getRingVertexColor(module, 90);
      int color3 = getRingVertexColor(module, 180);
      int color4 = getRingVertexColor(module, 270);
      arc.render(
         ShapeProperties.create(matrix, centerX - ringSize / 2.0F, centerY - ringSize / 2.0F, ringSize, ringSize)
            .round(roundness)
            .thickness(thickness)
            .end(attackProgress)
            .color(color1, color2, color3, color4)
            .build()
      );
   }

   private static void renderArcTriangles(
      VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, float startAngle, float endAngle, float thickness, int color
   ) {
      int segments = 64;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      float outerRadiusX = width / 2.0F;
      float outerRadiusY = height / 2.0F;
      float actualThickness = thickness;
      if (thickness < 1.0F) {
         actualThickness = outerRadiusX * thickness;
      }

      float innerRadiusX = Math.max(0.1F, outerRadiusX - actualThickness);
      float innerRadiusY = Math.max(0.1F, outerRadiusY - actualThickness);
      if (!(endAngle <= 0.0F)) {
         int actualSegments = Math.max(4, (int)(segments * endAngle / 360.0F));
         float angleStep = endAngle / actualSegments;

         for (int i = 0; i < actualSegments; i++) {
            float angle1 = (float)Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float)Math.toRadians(startAngle + (i + 1) * angleStep);
            float cos1 = (float)Math.cos(angle1);
            float sin1 = (float)Math.sin(angle1);
            float cos2 = (float)Math.cos(angle2);
            float sin2 = (float)Math.sin(angle2);
            float x1Outer = centerX + cos1 * outerRadiusX;
            float y1Outer = centerY + sin1 * outerRadiusY;
            float x2Outer = centerX + cos2 * outerRadiusX;
            float y2Outer = centerY + sin2 * outerRadiusY;
            float x1Inner = centerX + cos1 * innerRadiusX;
            float y1Inner = centerY + sin1 * innerRadiusY;
            float x2Inner = centerX + cos2 * innerRadiusX;
            float y2Inner = centerY + sin2 * innerRadiusY;
            buffer.vertex(matrix, x1Inner, y1Inner, 0.0F).color(color);
            buffer.vertex(matrix, x1Outer, y1Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Outer, y2Outer, 0.0F).color(color);
            buffer.vertex(matrix, x1Inner, y1Inner, 0.0F).color(color);
            buffer.vertex(matrix, x2Outer, y2Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Inner, y2Inner, 0.0F).color(color);
         }
      }
   }

   private static void renderArc(
      VertexConsumer buffer, Matrix4f matrix, float x, float y, float width, float height, float startAngle, float endAngle, float thickness, int color
   ) {
      int segments = 64;
      float centerX = x + width / 2.0F;
      float centerY = y + height / 2.0F;
      float outerRadiusX = width / 2.0F;
      float outerRadiusY = height / 2.0F;
      float actualThickness = thickness;
      if (thickness < 1.0F) {
         actualThickness = outerRadiusX * thickness;
      }

      float innerRadiusX = Math.max(0.1F, outerRadiusX - actualThickness);
      float innerRadiusY = Math.max(0.1F, outerRadiusY - actualThickness);
      if (!(endAngle <= 0.0F)) {
         int actualSegments = Math.max(4, (int)(segments * endAngle / 360.0F));
         float angleStep = endAngle / actualSegments;

         for (int i = 0; i < actualSegments; i++) {
            float angle1 = (float)Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float)Math.toRadians(startAngle + (i + 1) * angleStep);
            float cos1 = (float)Math.cos(angle1);
            float sin1 = (float)Math.sin(angle1);
            float cos2 = (float)Math.cos(angle2);
            float sin2 = (float)Math.sin(angle2);
            float x1Outer = centerX + cos1 * outerRadiusX;
            float y1Outer = centerY + sin1 * outerRadiusY;
            float x2Outer = centerX + cos2 * outerRadiusX;
            float y2Outer = centerY + sin2 * outerRadiusY;
            float x1Inner = centerX + cos1 * innerRadiusX;
            float y1Inner = centerY + sin1 * innerRadiusY;
            float x2Inner = centerX + cos2 * innerRadiusX;
            float y2Inner = centerY + sin2 * innerRadiusY;
            buffer.vertex(matrix, x1Inner, y1Inner, 0.0F).color(color);
            buffer.vertex(matrix, x1Outer, y1Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Outer, y2Outer, 0.0F).color(color);
            buffer.vertex(matrix, x2Inner, y2Inner, 0.0F).color(color);
         }
      }
   }

   private static int getRingVertexColor(CrossHair module, int vertexAngle) {
      int baseColor = getBaseColorAtAngle(module, vertexAngle);
      int targetColor = ColorUtil.multAlpha(module.getTargetColorSetting().getColor(), 1.0F);
      return ColorUtil.overCol(baseColor, targetColor, colorAnimation);
   }

   private static int getBaseColorAtAngle(CrossHair module, int angle) {
      if (module.getColorMode().isSelected("Custom")) {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            String animType = module.getColorAnimation().getSelected();
            if ("Wave".equals(animType)) {
               return getWaveColorAtAngle(colors, angle);
            }

            return ColorUtil.multAlpha(colors[0], 1.0F);
         }
      }

      return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
   }

   private static int getWaveColorAtAngle(int[] colors, int angle) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], 1.0F);
      }

      long time = System.currentTimeMillis() / 10L;
      float totalAngle = (float)((angle + time) % 360L);
      float anglePerColor = 360.0F / colors.length;
      float colorProgress = totalAngle / anglePerColor;
      int index1 = (int)Math.floor(colorProgress) % colors.length;
      int index2 = (index1 + 1) % colors.length;
      float lerp = colorProgress - (int)Math.floor(colorProgress);
      return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), 1.0F);
   }

   private static int getCrosshairColor(CrossHair module) {
      int defaultColor = getBaseColor(module);
      int targetColor = ColorUtil.multAlpha(module.getTargetColorSetting().getColor(), 1.0F);
      return ColorUtil.overCol(defaultColor, targetColor, colorAnimation);
   }

   private static int getBaseColor(CrossHair module) {
      if (module.getColorMode().isSelected("Custom")) {
         int[] colors = module.getCustomColors();
         if (colors != null && colors.length > 0) {
            String animType = module.getColorAnimation().getSelected();
            if ("Wave".equals(animType)) {
               return getWaveColor(colors);
            }

            return ColorUtil.multAlpha(colors[0], 1.0F);
         }
      }

      return ColorUtil.multAlpha(ColorUtil.getClientColor(), 1.0F);
   }

   private static int getWaveColor(int[] colors) {
      if (colors.length == 1) {
         return ColorUtil.multAlpha(colors[0], 1.0F);
      } else if (colors.length == 2) {
         int angle = (int)(System.currentTimeMillis() / 8L % 360L);
         angle = angle >= 180 ? 360 - angle : angle;
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[0], colors[1], angle / 180.0F), 1.0F);
      } else {
         float timeProgress = (float)(System.currentTimeMillis() / 10L % (colors.length * 360L)) / 360.0F;
         int index1 = (int)Math.floor(timeProgress) % colors.length;
         int index2 = (index1 + 1) % colors.length;
         float lerp = timeProgress - (int)Math.floor(timeProgress);
         return ColorUtil.multAlpha(ColorUtil.overCol(colors[index1], colors[index2], lerp), 1.0F);
      }
   }

   private CrosshairRenderer() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
