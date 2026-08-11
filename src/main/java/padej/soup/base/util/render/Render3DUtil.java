package padej.soup.base.util.render;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public final class Render3DUtil implements QuickImports {
   private static final Map<VoxelShape, Pair<List<Box>, List<Render3DUtil.Line>>> SHAPE_OUTLINES = new HashMap<>();
   public static final List<Render3DUtil.Texture> TEXTURE_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Texture> TEXTURE = new ArrayList<>();
   public static final List<Render3DUtil.Line> LINE_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Line> LINE = new ArrayList<>();
   public static final List<Render3DUtil.Quad> QUAD_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.Quad> QUAD = new ArrayList<>();
   public static final List<Render3DUtil.GhostTexture> GHOST_TEXTURE_DEPTH = new ArrayList<>();
   public static final List<Render3DUtil.GhostTexture> GHOST_TEXTURE = new ArrayList<>();
   private static final Map<Identifier, List<Render3DUtil.Texture>> TEXTURE_BATCHES = new LinkedHashMap<>();
   private static final Map<Identifier, List<Render3DUtil.Texture>> TEXTURE_DEPTH_BATCHES = new LinkedHashMap<>();
   private static final Map<Float, List<Render3DUtil.Line>> LINE_BATCHES = new LinkedHashMap<>();
   private static final Map<Float, List<Render3DUtil.Line>> LINE_DEPTH_BATCHES = new LinkedHashMap<>();
   private static final Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> GHOST_BATCHES = new LinkedHashMap<>();
   private static final Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> GHOST_DEPTH_BATCHES = new LinkedHashMap<>();
   public static Matrix4f lastProjMat = new Matrix4f();
   public static net.minecraft.client.util.math.MatrixStack.Entry lastWorldSpaceMatrix = new MatrixStack().peek();
   private static final Matrix4f lastWorldRotationMatrix = new Matrix4f();
   private static Vec3d lastCameraPos = Vec3d.ZERO;

   public static void onWorldRender() {
      prepareWorldPrecisionState();
      ParticleBatchRenderer.renderBatches();
      if (!TEXTURE.isEmpty()) {
         groupTexturesById(TEXTURE, TEXTURE_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

         for (Entry<Identifier, List<Render3DUtil.Texture>> batchEntry : TEXTURE_BATCHES.entrySet()) {
            RenderSystem.setShaderTexture(0, batchEntry.getKey());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Render3DUtil.Texture tex : batchEntry.getValue()) {
               quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
         }

         RenderSystem.disableBlend();
         TEXTURE.clear();
      }

      if (!TEXTURE_DEPTH.isEmpty()) {
         groupTexturesById(TEXTURE_DEPTH, TEXTURE_DEPTH_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_CONSTANT_ALPHA);

         for (Entry<Identifier, List<Render3DUtil.Texture>> batchEntry : TEXTURE_DEPTH_BATCHES.entrySet()) {
            RenderSystem.setShaderTexture(0, batchEntry.getKey());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (Render3DUtil.Texture tex : batchEntry.getValue()) {
               quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
         }

         RenderSystem.disableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         TEXTURE_DEPTH.clear();
      }

      if (!LINE.isEmpty()) {
         GL11.glEnable(2881);
         groupLinesByWidth(LINE, LINE_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

         for (Entry<Float, List<Render3DUtil.Line>> batchEntry : LINE_BATCHES.entrySet()) {
            RenderSystem.lineWidth(batchEntry.getKey());
            BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);

            for (Render3DUtil.Line line : batchEntry.getValue()) {
               vertexLine(line.entry, buffer, line.start, line.end, line.colorStart, line.colorEnd);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
         }

         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         LINE.clear();
         GL11.glDisable(2881);
      }

      if (!QUAD.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.disableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         QUAD.clear();
      }

      if (!LINE_DEPTH.isEmpty()) {
         GL11.glEnable(2881);
         groupLinesByWidth(LINE_DEPTH, LINE_DEPTH_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);

         for (Entry<Float, List<Render3DUtil.Line>> batchEntry : LINE_DEPTH_BATCHES.entrySet()) {
            RenderSystem.lineWidth(batchEntry.getKey());
            BufferBuilder buffer = tessellator.begin(DrawMode.LINES, VertexFormats.LINES);

            for (Render3DUtil.Line line : batchEntry.getValue()) {
               vertexLine(line.entry, buffer, line.start, line.end, line.colorStart, line.colorEnd);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
         }

         RenderSystem.depthMask(true);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         LINE_DEPTH.clear();
         GL11.glDisable(2881);
      }

      if (!QUAD_DEPTH.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.disableCull();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         QUAD_DEPTH.forEach(quad -> vertexQuad(quad.entry, buffer, quad.x, quad.y, quad.w, quad.z, quad.color));
         BufferRenderer.drawWithGlobalProgram(buffer.end());
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         QUAD_DEPTH.clear();
      }

      if (!GHOST_TEXTURE.isEmpty()) {
         groupGhostTextures(GHOST_TEXTURE, GHOST_BATCHES);
         RenderSystem.enableBlend();

         for (Entry<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> textureBatch : GHOST_BATCHES.entrySet()) {
            RenderSystem.setShaderTexture(0, textureBatch.getKey());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

            for (Entry<Runnable, List<Render3DUtil.GhostTexture>> blendBatch : textureBatch.getValue().entrySet()) {
               blendBatch.getKey().run();
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (Render3DUtil.GhostTexture tex : blendBatch.getValue()) {
                  quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
               }

               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
         }

         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
         GHOST_TEXTURE.clear();
      }

      if (!GHOST_TEXTURE_DEPTH.isEmpty()) {
         groupGhostTextures(GHOST_TEXTURE_DEPTH, GHOST_DEPTH_BATCHES);
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(false);

         for (Entry<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> textureBatch : GHOST_DEPTH_BATCHES.entrySet()) {
            RenderSystem.setShaderTexture(0, textureBatch.getKey());
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

            for (Entry<Runnable, List<Render3DUtil.GhostTexture>> blendBatch : textureBatch.getValue().entrySet()) {
               blendBatch.getKey().run();
               BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

               for (Render3DUtil.GhostTexture tex : blendBatch.getValue()) {
                  quadTexture(tex.entry, buffer, tex.x, tex.y, tex.width, tex.height, tex.color);
               }

               BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
         }

         RenderSystem.disableBlend();
         RenderSystem.depthMask(true);
         RenderSystem.defaultBlendFunc();
         GHOST_TEXTURE_DEPTH.clear();
      }
   }

   public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
      Vec3d vec3d = Vec3d.of(blockPos);
      if (ProjectionUtil.canSee(new Box(blockPos))) {
         if (SHAPE_OUTLINES.containsKey(voxelShape)) {
            Pair<List<Box>, List<Render3DUtil.Line>> pair = SHAPE_OUTLINES.get(voxelShape);
            if (fill) {
               for (Box box : pair.getLeft()) {
                  drawBox(box.offset(vec3d), color, width, false, true, depth);
               }
            }

            for (Render3DUtil.Line line : pair.getRight()) {
               drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth);
            }

            return;
         }

         List<Render3DUtil.Line> lines = new ArrayList<>();
         voxelShape.forEachEdge(
            (minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Render3DUtil.Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0.0F))
         );
         SHAPE_OUTLINES.put(voxelShape, new Pair<>(voxelShape.getBoundingBoxes(), lines));
      }
   }

   public static void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
      drawBox(null, box, color, width, line, fill, depth);
   }

   public static void drawBox(
      net.minecraft.client.util.math.MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth
   ) {
      box = box.expand(0.001);
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      if (fill) {
         int fillColor = ColorUtil.multAlpha(color, 0.1F);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
         drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
         drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
      }

      if (line) {
         drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
         drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
         drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
         drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
         drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
         drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
         drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
         drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
         drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
         drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
         drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
         drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
      }
   }

   public static void vertexLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor) {
      vertexLine(matrices != null ? matrices.peek() : null, buffer, start, end, startColor, endColor);
   }

   public static void vertexLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vec3d start, Vec3d end, int startColor, int endColor
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      boolean useWorldRebase = usesLegacyWorldSpace(entry);
      Matrix4f positionMatrix = useWorldRebase ? lastWorldRotationMatrix : entry.getPositionMatrix();
      Vector3f startVec = useWorldRebase ? toCameraRelative(start) : start.toVector3f();
      Vector3f endVec = useWorldRebase ? toCameraRelative(end) : end.toVector3f();
      Vector3f vec = getNormal(startVec, endVec);
      buffer.vertex(positionMatrix, startVec.x, startVec.y, startVec.z).color(startColor).normal(entry, vec);
      buffer.vertex(positionMatrix, endVec.x, endVec.y, endVec.z).color(endColor).normal(entry, vec);
   }

   public static void vertexLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vector3f start, Vector3f end, int startColor, int endColor
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      Vector3f vec = getNormal(start, end);
      Matrix4f positionMatrix = entry.getPositionMatrix();
      buffer.vertex(positionMatrix, start.x, start.y, start.z).color(startColor).normal(entry, vec);
      buffer.vertex(positionMatrix, end.x, end.y, end.z).color(endColor).normal(entry, vec);
   }

   public static void vertexQuad(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vec3d vec1, Vec3d vec2, Vec3d vec3, Vec3d vec4, int color
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      boolean useWorldRebase = usesLegacyWorldSpace(entry);
      Matrix4f positionMatrix = useWorldRebase ? lastWorldRotationMatrix : entry.getPositionMatrix();
      Vector3f v1 = useWorldRebase ? toCameraRelative(vec1) : vec1.toVector3f();
      Vector3f v2 = useWorldRebase ? toCameraRelative(vec2) : vec2.toVector3f();
      Vector3f v3 = useWorldRebase ? toCameraRelative(vec3) : vec3.toVector3f();
      Vector3f v4 = useWorldRebase ? toCameraRelative(vec4) : vec4.toVector3f();
      vertexQuad(positionMatrix, buffer, v1, v2, v3, v4, color);
   }

   public static void vertexQuad(
      net.minecraft.client.util.math.MatrixStack.Entry entry, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color
   ) {
      if (entry == null) {
         entry = lastWorldSpaceMatrix;
      }

      vertexQuad(entry.getPositionMatrix(), buffer, vec1, vec2, vec3, vec4, color);
   }

   public static void quadTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry, BufferBuilder buffer, float x, float y, float width, float height, Vector4i color
   ) {
      buffer.vertex(entry, x, y + height, 0.0F).texture(0.0F, 0.0F).color(color.x);
      buffer.vertex(entry, x + width, y + height, 0.0F).texture(0.0F, 1.0F).color(color.y);
      buffer.vertex(entry, x + width, y, 0.0F).texture(1.0F, 1.0F).color(color.w);
      buffer.vertex(entry, x, y, 0.0F).texture(1.0F, 0.0F).color(color.z);
   }

   @NotNull
   public static Vector3f getNormal(Vector3f start, Vector3f end) {
      Vector3f normal = new Vector3f(start).sub(end);
      float lengthSq = normal.lengthSquared();
      return lengthSq <= 1.0E-8F ? new Vector3f(0.0F, 1.0F, 0.0F) : normal.div(MathHelper.sqrt(lengthSq));
   }

   public static void drawLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry,
      double minX,
      double minY,
      double minZ,
      double maxX,
      double maxY,
      double maxZ,
      int color,
      float width,
      boolean depth
   ) {
      drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
   }

   public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
      drawLine(null, start, end, color, color, width, depth);
   }

   public static void drawLine(
      net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth
   ) {
      Render3DUtil.Line line = new Render3DUtil.Line(entry, start, end, colorStart, colorEnd, width);
      if (depth) {
         LINE_DEPTH.add(line);
      } else {
         LINE.add(line);
      }
   }

   public static void drawQuad(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
      Render3DUtil.Quad quad = new Render3DUtil.Quad(entry, x, y, w, z, color);
      if (depth) {
         QUAD_DEPTH.add(quad);
      } else {
         QUAD.add(quad);
      }
   }

   public static void drawTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color, boolean depth
   ) {
      Render3DUtil.Texture texture = new Render3DUtil.Texture(entry, id, x, y, width, height, color);
      if (depth) {
         TEXTURE_DEPTH.add(texture);
      } else {
         TEXTURE.add(texture);
      }
   }

   public static void drawGhostTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry,
      Identifier id,
      float x,
      float y,
      float width,
      float height,
      Vector4i color,
      boolean depth,
      Runnable setupBlendFunc
   ) {
      Render3DUtil.GhostTexture texture = new Render3DUtil.GhostTexture(entry, id, x, y, width, height, color, setupBlendFunc);
      if (depth) {
         GHOST_TEXTURE_DEPTH.add(texture);
      } else {
         GHOST_TEXTURE.add(texture);
      }
   }

   private static void groupTexturesById(List<Render3DUtil.Texture> textures, Map<Identifier, List<Render3DUtil.Texture>> batches) {
      batches.clear();

      for (Render3DUtil.Texture texture : textures) {
         batches.computeIfAbsent(texture.id, ignored -> new ArrayList<>()).add(texture);
      }
   }

   private static void groupLinesByWidth(List<Render3DUtil.Line> lines, Map<Float, List<Render3DUtil.Line>> batches) {
      batches.clear();

      for (Render3DUtil.Line line : lines) {
         batches.computeIfAbsent(line.width, ignored -> new ArrayList<>()).add(line);
      }
   }

   private static void groupGhostTextures(List<Render3DUtil.GhostTexture> textures, Map<Identifier, Map<Runnable, List<Render3DUtil.GhostTexture>>> batches) {
      batches.clear();

      for (Render3DUtil.GhostTexture texture : textures) {
         Map<Runnable, List<Render3DUtil.GhostTexture>> byBlendFunc = batches.computeIfAbsent(texture.id, ignored -> new LinkedHashMap<>());
         byBlendFunc.computeIfAbsent(texture.setupBlendFunc, ignored -> new ArrayList<>()).add(texture);
      }
   }

   private static void prepareWorldPrecisionState() {
      if (mc != null && mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
         lastCameraPos = mc.gameRenderer.getCamera().getPos();
      } else {
         lastCameraPos = Vec3d.ZERO;
      }

      if (lastWorldSpaceMatrix != null) {
         lastWorldRotationMatrix.set(lastWorldSpaceMatrix.getPositionMatrix());
         lastWorldRotationMatrix.setTranslation(0.0F, 0.0F, 0.0F);
      } else {
         lastWorldRotationMatrix.identity();
      }
   }

   private static boolean usesLegacyWorldSpace(net.minecraft.client.util.math.MatrixStack.Entry entry) {
      if (entry == null || lastWorldSpaceMatrix == null) {
         return false;
      } else {
         return entry == lastWorldSpaceMatrix
            ? true
            : entry.getPositionMatrix().equals(lastWorldSpaceMatrix.getPositionMatrix())
               && entry.getNormalMatrix().equals(lastWorldSpaceMatrix.getNormalMatrix());
      }
   }

   private static Vector3f toCameraRelative(Vec3d worldPos) {
      return new Vector3f((float)(worldPos.x - lastCameraPos.x), (float)(worldPos.y - lastCameraPos.y), (float)(worldPos.z - lastCameraPos.z));
   }

   private static void vertexQuad(Matrix4f matrix, VertexConsumer buffer, Vector3f vec1, Vector3f vec2, Vector3f vec3, Vector3f vec4, int color) {
      buffer.vertex(matrix, vec1.x, vec1.y, vec1.z).color(color);
      buffer.vertex(matrix, vec2.x, vec2.y, vec2.z).color(color);
      buffer.vertex(matrix, vec3.x, vec3.y, vec3.z).color(color);
      buffer.vertex(matrix, vec4.x, vec4.y, vec4.z).color(color);
   }

   private Render3DUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static void setLastProjMat(Matrix4f lastProjMat) {
      Render3DUtil.lastProjMat = lastProjMat;
   }

   public static void setLastWorldSpaceMatrix(net.minecraft.client.util.math.MatrixStack.Entry lastWorldSpaceMatrix) {
      Render3DUtil.lastWorldSpaceMatrix = lastWorldSpaceMatrix;
   }

   public record GhostTexture(
      net.minecraft.client.util.math.MatrixStack.Entry entry,
      Identifier id,
      float x,
      float y,
      float width,
      float height,
      Vector4i color,
      Runnable setupBlendFunc
   ) {
   }

   public record Line(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
   }

   public record Quad(net.minecraft.client.util.math.MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
   }

   public record Texture(net.minecraft.client.util.math.MatrixStack.Entry entry, Identifier id, float x, float y, float width, float height, Vector4i color) {
   }
}
