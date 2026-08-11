package padej.soup.implement.features.draggables;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector4d;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.animation.implement.EaseInOutAnimation;
import padej.soup.api.system.animation.implement.LinearAnimation;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;
import padej.soup.base.util.entity.VisibleUtils;
import padej.soup.base.util.item.ItemUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.base.util.other.StopWatch;
import padej.soup.base.util.render.Render2DUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.base.util.render.TargetHudRenderer;
import padej.soup.base.util.spatial.SpatialGrid2D;
import padej.soup.core.Main;
import padej.soup.core.server.ServerLimitCfg;
import padej.soup.implement.features.draggables.particles.TargetHudParticle;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class TargetHud extends AbstractDraggable {
   private final Animation animation = new DecelerateAnimation().setMs(200).setValue(1.0);
   private final Animation healthAnimation = new DecelerateAnimation().setMs(150).setValue(1.0);
   private final StopWatch stopWatch = new StopWatch();
   private LivingEntity targetEntity;
   private LivingEntity lastTarget;
   private long lastTargetTime;
   private Item lastItem = Items.AIR;
   private float health;
   private float targetHealth;
   private float animatedHealth;
   private String lastAnimationType = "Decelerate";
   private final List<TargetHudParticle> particles = new ArrayList<>();
   private final List<TargetHudParticle> networkParticles = new ArrayList<>();
   private SpatialGrid2D<TargetHudParticle> spatialGrid;
   private boolean sentParticles = false;
   private int lastHurtTime = 0;

   public TargetHud() {
      super("TargetHud", 10, 40, 100, 36, true);
      this.health = 0.0F;
      this.targetHealth = 0.0F;
      this.animatedHealth = 0.0F;
   }

   @Override
   public boolean visible() {
      return this.scaleAnimation.isDirection(Direction.FORWARDS);
   }

   private void updateScaleAnimation() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      String currentType = hudModule.animationType.getSelected();
      int currentSpeed = (int)hudModule.animationSpeed.getValue();
      if (!currentType.equals(this.lastAnimationType) || this.scaleAnimation.getMs() != currentSpeed) {
         this.lastAnimationType = currentType;
         Direction currentDirection = this.scaleAnimation.isDirection(Direction.FORWARDS) ? Direction.FORWARDS : Direction.BACKWARDS;

         Animation newAnimation = switch (currentType) {
            case "Linear" -> new LinearAnimation();
            case "EaseInOut" -> new EaseInOutAnimation();
            default -> new DecelerateAnimation();
         };
         newAnimation.setMs(currentSpeed);
         newAnimation.setValue(1.0);
         newAnimation.setDirection(currentDirection);

         try {
            Field field = AbstractDraggable.class.getDeclaredField("scaleAnimation");
            field.setAccessible(true);
            field.set(this, newAnimation);
         } catch (Exception e) {
            LoggerUtil.error("Failed to update scale animation: " + e.getMessage());
         }
      }
   }

   public void renderParticlesAlways(DrawContext context) {
      if (!this.particles.isEmpty() || !this.networkParticles.isEmpty()) {
         MatrixStack matrices = context.getMatrices();
         this.renderParticlesIndependent(context);
         this.renderNetworkLinksIndependent(matrices);
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (this.lastTarget != null && VisibleUtils.canBeTargeted(this.lastTarget)) {
         MatrixStack matrix = context.getMatrices();
         this.updateDimensionsForStyle();
         padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
         float scale = hudModule.scale.getValue();
         matrix.push();
         if (hudModule.displayMode.getSelected().equals("3D")) {
            String anchor = hudModule.anchor.getSelected();
            Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, anchor);
            if (projection == null || ProjectionUtil.cantSee(projection)) {
               matrix.pop();
               return;
            }

            double centerX = ProjectionUtil.centerX(projection);
            double centerY = projection.y;
            switch (anchor) {
               case "HEAD":
                  centerY -= this.getHeight() / 2.0F + 5.0F;
               case "BODY":
               default:
                  break;
               case "FEET":
                  centerY += this.getHeight() / 2.0F + 5.0F;
            }

            double hudX = centerX + hudModule.xOffset.getValue();
            matrix.translate(hudX, centerY, 0.0);
         } else {
            matrix.translate(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, 0.0F);
         }

         float animationValue = this.scaleAnimation.getOutput().floatValue();
         String animationMode = hudModule.animationMode.getSelected();
         float scaleMultiplier = 1.0F;
         if (animationMode.equals("Scale") || animationMode.equals("Both")) {
            scaleMultiplier = animationValue;
         }

         matrix.scale(scale * scaleMultiplier, scale * scaleMultiplier, 1.0F);
         matrix.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
         this.drawUsingItem(context, matrix);
         float alpha = 1.0F;
         if (animationMode.equals("Fade") || animationMode.equals("Both")) {
            alpha = animationValue;
         }

         if (this.lastTarget != null) {
            float currentHealth = this.health;
            if (this.targetHealth > 0.0F) {
               float healthProgress = this.healthAnimation.getOutput().floatValue();
               this.animatedHealth = MathHelper.lerp(healthProgress, currentHealth, this.targetHealth);
               this.animatedHealth = MathHelper.clamp(this.animatedHealth, 2.0F, 61.0F);
               if (this.healthAnimation.isDone()) {
                  this.health = this.targetHealth;
               }
            } else {
               this.animatedHealth = currentHealth;
            }
         }

         MathUtil.setAlpha(
            alpha,
            () -> {
               switch (hudModule.style.getSelected()) {
                  case "Default":
                     TargetHudRenderer.renderStyleZenith(
                        context,
                        this.lastTarget,
                        0.0F,
                        0.0F,
                        this.getWidth(),
                        this.getHeight(),
                        ServerLimitCfg.showHp(this.lastTarget) ? this.animatedHealth : 61.0F
                     );
                     break;
                  case "Round":
                     TargetHudRenderer.renderStyleAres(
                        context,
                        this.lastTarget,
                        0.0F,
                        0.0F,
                        this.getWidth(),
                        this.getHeight(),
                        ServerLimitCfg.showHp(this.lastTarget) ? this.animatedHealth : 61.0F
                     );
               }
            }
         );
         matrix.pop();
      }
   }

   private void renderNetworkLinksIndependent(MatrixStack matrices) {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      if (this.networkParticles.size() >= 2 && this.spatialGrid != null) {
         matrices.push();
         if (hudModule.displayMode.getSelected().equals("3D") && this.lastTarget != null) {
            String anchor = hudModule.anchor.getSelected();
            Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, anchor);
            if (projection != null && !ProjectionUtil.cantSee(projection)) {
               double centerX = ProjectionUtil.centerX(projection);
               double centerY = projection.y;
               switch (anchor) {
                  case "HEAD":
                     centerY -= this.getHeight() / 2.0F + 5.0F;
                  case "BODY":
                  default:
                     break;
                  case "FEET":
                     centerY += this.getHeight() / 2.0F + 5.0F;
               }

               double hudX = centerX + hudModule.xOffset.getValue();
               matrices.translate(hudX, centerY, 0.0);
               float scale = hudModule.scale.getValue();
               matrices.scale(scale, scale, 1.0F);
               matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
               this.renderNetworkLinksInternal(matrices);
            }
         } else {
            matrices.translate(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, 0.0F);
            float scale = hudModule.scale.getValue();
            matrices.scale(scale, scale, 1.0F);
            matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
            this.renderNetworkLinksInternal(matrices);
         }

         matrices.pop();
      }
   }

   private void renderNetworkLinksInternal(MatrixStack matrices) {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      float maxLinkDistance = hudModule.linkDistance.getValue();
      int maxLinksPerNode = (int)hudModule.maxLinks.getValue();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      Set<TargetHud.ParticlePair2D> processedPairs = new HashSet<>();
      Map<TargetHudParticle, Integer> linkCounts = new HashMap<>();
      int lineCount = 0;

      for (TargetHudParticle p1 : this.networkParticles) {
         int p1LinkCount = linkCounts.getOrDefault(p1, 0);
         if (p1LinkCount < maxLinksPerNode) {
            float posX1 = MathUtil.interpolate(p1.getPrevX(), p1.getX());
            float posY1 = MathUtil.interpolate(p1.getPrevY(), p1.getY());
            List<SpatialGrid2D.GridEntry2D<TargetHudParticle>> nearbyEntries = this.spatialGrid.queryRadiusWithPositions(posX1, posY1, maxLinkDistance);
            nearbyEntries.sort((ax, bx) -> {
               float dx1 = ax.getX() - posX1;
               float dy1 = ax.getY() - posY1;
               float dist1 = dx1 * dx1 + dy1 * dy1;
               float dx2 = bx.getX() - posX1;
               float dy2 = bx.getY() - posY1;
               float dist2 = dx2 * dx2 + dy2 * dy2;
               return Float.compare(dist1, dist2);
            });

            for (SpatialGrid2D.GridEntry2D<TargetHudParticle> entry : nearbyEntries) {
               TargetHudParticle p2 = entry.getObject();
               if (p1 != p2) {
                  int p2LinkCount = linkCounts.getOrDefault(p2, 0);
                  if (p2LinkCount < maxLinksPerNode) {
                     TargetHud.ParticlePair2D pair = new TargetHud.ParticlePair2D(p1, p2);
                     if (!processedPairs.contains(pair)) {
                        if (p1LinkCount >= maxLinksPerNode) {
                           break;
                        }

                        float posX2 = entry.getX();
                        float posY2 = entry.getY();
                        float dx = posX2 - posX1;
                        float dy = posY2 - posY1;
                        float dist = (float)Math.sqrt(dx * dx + dy * dy);
                        if (dist < maxLinkDistance && dist > 0.01F) {
                           float lineAlpha = 1.0F - dist / maxLinkDistance;
                           float particleAlpha1 = p1.getAlpha();
                           float particleAlpha2 = p2.getAlpha();
                           float alpha = Math.min(lineAlpha, Math.min(particleAlpha1, particleAlpha2));
                           int baseColor = ColorUtil.getClientColor();
                           int color = ColorUtil.multAlpha(baseColor, alpha);
                           int r = color >> 16 & 0xFF;
                           int g = color >> 8 & 0xFF;
                           int b = color & 0xFF;
                           int a = (int)(alpha * 255.0F);
                           bufferBuilder.vertex(matrix, posX1, posY1, 0.0F).color(r, g, b, a);
                           bufferBuilder.vertex(matrix, posX2, posY2, 0.0F).color(r, g, b, a);
                           lineCount++;
                           processedPairs.add(pair);
                           linkCounts.put(p1, p1LinkCount + 1);
                           linkCounts.put(p2, p2LinkCount + 1);
                           p1LinkCount++;
                        }
                     }
                  }
               }
            }
         }
      }

      if (lineCount > 0) {
         BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
      }

      RenderSystem.disableBlend();
   }

   private void renderParticlesIndependent(DrawContext context) {
      if (!this.particles.isEmpty()) {
         MatrixStack matrices = context.getMatrices();
         padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
         matrices.push();
         if (hudModule.displayMode.getSelected().equals("3D") && this.lastTarget != null) {
            String anchor = hudModule.anchor.getSelected();
            Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, anchor);
            if (projection != null && !ProjectionUtil.cantSee(projection)) {
               double centerX = ProjectionUtil.centerX(projection);
               double centerY = projection.y;
               switch (anchor) {
                  case "HEAD":
                     centerY -= this.getHeight() / 2.0F + 5.0F;
                  case "BODY":
                  default:
                     break;
                  case "FEET":
                     centerY += this.getHeight() / 2.0F + 5.0F;
               }

               double hudX = centerX + hudModule.xOffset.getValue();
               matrices.translate(hudX, centerY, 0.0);
               float scale = hudModule.scale.getValue();
               matrices.scale(scale, scale, 1.0F);
               matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
               this.renderParticlesInternal(matrices);
            }
         } else {
            matrices.translate(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, 0.0F);
            float scale = hudModule.scale.getValue();
            matrices.scale(scale, scale, 1.0F);
            matrices.translate(-this.getWidth() / 2.0F, -this.getHeight() / 2.0F, 0.0F);
            this.renderParticlesInternal(matrices);
         }

         matrices.pop();
      }
   }

   private void renderParticlesInternal(MatrixStack matrices) {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      float depthFactor = 1.0F;
      if (hudModule.displayMode.getSelected().equals("3D") && this.lastTarget != null) {
         String anchor = hudModule.anchor.getSelected();
         Vector4d projection = ProjectionUtil.getVector4DForAnchor(this.lastTarget, anchor);
         if (projection != null) {
            depthFactor = MathHelper.clamp(1.0F - (float)projection.z / 10.0F, 0.1F, 1.0F);
         }
      }

      Map<Identifier, List<TargetHud.ParticleRenderData>> texturedBatches = new HashMap<>();
      List<TargetHud.ParticleRenderData> bloomParticles = new ArrayList<>();

      for (TargetHudParticle particle : this.particles) {
         if (!(particle.getAlpha() <= 0.01F) && !particle.isNetworkParticle()) {
            float interpolatedX = MathUtil.interpolate(particle.getPrevX(), particle.getX());
            float interpolatedY = MathUtil.interpolate(particle.getPrevY(), particle.getY());
            float finalScale = hudModule.particleSize.getValue() * depthFactor;
            TargetHud.ParticleRenderData data = new TargetHud.ParticleRenderData(particle, interpolatedX, interpolatedY, finalScale);
            if (particle.getType() != TargetHudParticle.ParticleType.CUBE && particle.getType() != TargetHudParticle.ParticleType.PYRAMID) {
               Identifier texture = particle.getTextureForType();
               if (texture != null) {
                  texturedBatches.computeIfAbsent(texture, k -> new ArrayList<>()).add(data);
               }
            } else {
               bloomParticles.add(data);
               particle.render(matrices, 0.0F, 0.0F, depthFactor);
            }
         }
      }

      this.renderTexturedBatches(matrices, texturedBatches);
      ParticleBatchRenderer.renderBatches();
   }

   private void renderTexturedBatches(MatrixStack matrices, Map<Identifier, List<TargetHud.ParticleRenderData>> batches) {
      if (!batches.isEmpty()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(770, 1);
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

         for (Entry<Identifier, List<TargetHud.ParticleRenderData>> entry : batches.entrySet()) {
            Identifier texture = entry.getKey();
            List<TargetHud.ParticleRenderData> batchData = entry.getValue();
            RenderSystem.setShaderTexture(0, texture);
            BufferBuilder bufferBuilder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (TargetHud.ParticleRenderData data : batchData) {
               matrices.push();
               matrices.translate(data.x, data.y, 0.0F);
               matrices.scale(data.scale, data.scale, data.scale);
               float interpolatedRotation = MathUtil.interpolate(data.particle.getPrevRotation(), data.particle.getRotation());
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(interpolatedRotation));
               int r = data.particle.getColor().getRed();
               int g = data.particle.getColor().getGreen();
               int b = data.particle.getColor().getBlue();
               int a = (int)(data.particle.getAlpha() * 255.0F);
               float halfSize = 0.5F;
               Matrix4f matrix = matrices.peek().getPositionMatrix();
               bufferBuilder.vertex(matrix, -halfSize, halfSize, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
               bufferBuilder.vertex(matrix, halfSize, halfSize, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
               bufferBuilder.vertex(matrix, halfSize, -halfSize, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
               bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
               matrices.pop();
            }

            BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
         }

         RenderSystem.defaultBlendFunc();
         RenderSystem.disableBlend();
      }
   }

   private void updateParticles() {
      this.particles.removeIf(particlex -> {
         boolean isDead = particlex.update();
         if (isDead && particlex.isNetworkParticle()) {
            this.networkParticles.remove(particlex);
            if (this.spatialGrid != null) {
               this.spatialGrid.remove(particlex, particlex.getX(), particlex.getY());
            }
         }

         return isDead;
      });
      if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
         for (TargetHudParticle particle : this.networkParticles) {
            this.spatialGrid.update(particle, particle.getX(), particle.getY(), particle.getX(), particle.getY());
         }
      }
   }

   private String getRandomMode() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      List<String> selected = hudModule.particleMode.getSelected();
      return selected.isEmpty() ? "Stars" : selected.get(QuickImports.random().nextInt(selected.size()));
   }

   private void spawnParticlesOnHurt() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      if (hudModule.particles.isValue() && this.lastTarget != null) {
         int count = (int)hudModule.particleCount.getValue();
         float size = hudModule.particleSize.getValue();
         float speed = hudModule.particleSpeed.getValue();
         float lifetime = hudModule.particleLifetime.getValue();
         int[] customColors = hudModule.getCustomColors();
         double spawnX;
         double spawnY;
         if (hudModule.particleSpawnLoc.isSelected("HP Bar")) {
            float currentHealth = this.animatedHealth;

            spawnX = switch (hudModule.style.getSelected()) {
               case "Default" -> 34.0F + currentHealth;
               case "Round" -> {
                  float widthHp = 68.0F;
                  float healthBarWidth = currentHealth * widthHp / 61.0F;
                  yield 48.0F + healthBarWidth;
               }
               default -> 34.0F + currentHealth;
            };

            spawnY = switch (hudModule.style.getSelected()) {
               case "Default" -> 28.0;
               case "Round" -> 36.5;
               default -> 28.0;
            };
         } else {
            spawnX = switch (hudModule.style.getSelected()) {
               case "Default" -> 15.0;
               case "Round" -> 23.0;
               default -> 15.0;
            };

            spawnY = switch (hudModule.style.getSelected()) {
               case "Default" -> 18.0;
               case "Round" -> 23.0;
               default -> 18.0;
            };
         }

         for (int i = 0; i < count; i++) {
            String randomMode = this.getRandomMode();
            TargetHudParticle.ParticleType type = TargetHudParticle.parseType(randomMode);
            double motionX = (QuickImports.random().nextDouble() * 2.0 - 1.0) * speed;
            double motionY = (QuickImports.random().nextDouble() * 2.0 - 1.0) * speed;
            Color particleColor;
            if (!hudModule.particleColorMode.isSelected("Custom") || customColors == null || customColors.length <= 0) {
               Color color1 = new Color(ColorUtil.getClientColor());
               Color color2 = new Color(ColorUtil.getClientColor(200.0F));
               double colorMix = (Math.sin(System.currentTimeMillis() * 0.001 + i) + 1.0) * 0.5;
               particleColor = this.mixColors(color1, color2, colorMix);
            } else if (hudModule.particleColorAnimation.isSelected("Vertex")) {
               int colorIndex = i % customColors.length;
               particleColor = new Color(customColors[colorIndex], true);
            } else {
               long time = System.currentTimeMillis();
               float phase = ((float)time / 1000.0F + i * 0.1F) % 1.0F;
               int colorIndex = (int)(phase * customColors.length);
               particleColor = new Color(customColors[colorIndex], true);
            }

            TargetHudParticle particle = new TargetHudParticle(
               (float)spawnX,
               (float)spawnY,
               (float)motionX,
               (float)motionY,
               size,
               particleColor,
               type,
               lifetime,
               hudModule.particleSpeed.getValue(),
               hudModule.particleMaxRadius.getValue(),
               "Fly"
            );
            this.particles.add(particle);
            if (type == TargetHudParticle.ParticleType.NETWORK2D) {
               if (this.spatialGrid == null) {
                  this.spatialGrid = new SpatialGrid2D<>(hudModule.linkDistance.getValue());
               }

               this.networkParticles.add(particle);
               this.spatialGrid.insert(particle, particle.getX(), particle.getY());
            }
         }
      }
   }

   private Color mixColors(Color c1, Color c2, double percent) {
      double inverse = 1.0 - percent;
      int r = (int)(c1.getRed() * percent + c2.getRed() * inverse);
      int g = (int)(c1.getGreen() * percent + c2.getGreen() * inverse);
      int b = (int)(c1.getBlue() * percent + c2.getBlue() * inverse);
      return new Color(r, g, b);
   }

   @Override
   public void tick() {
      super.tick();
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      this.updateScaleAnimation();
      this.updateParticles();
      LivingEntity previousTarget = this.targetEntity;
      if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
         if (!(entityHit.getEntity() instanceof PlayerEntity player && !player.isDead() && player.isAlive())) {
            this.targetEntity = null;
         } else if (VisibleUtils.canBeTargeted(player)) {
            this.targetEntity = player;
         } else {
            this.targetEntity = null;
         }
      } else {
         this.targetEntity = null;
      }

      if (this.targetEntity != null && (this.targetEntity.isDead() || !this.targetEntity.isAlive())) {
         this.targetEntity = null;
         if (previousTarget != null) {
            this.lastTargetTime = System.currentTimeMillis();
         }
      }

      if (previousTarget != null && this.targetEntity == null) {
         this.lastTargetTime = System.currentTimeMillis();
      }

      LivingEntity displayTarget = this.getDisplayTarget();
      if (displayTarget != null) {
         this.lastTarget = displayTarget;
         if (hudModule.isEnabled() && ServerLimitCfg.showHp(displayTarget)) {
            int currentHurtTime = displayTarget.hurtTime;
            if (currentHurtTime == 9 && !this.sentParticles && currentHurtTime != this.lastHurtTime) {
               this.spawnParticlesOnHurt();
               this.sentParticles = true;
            }

            if (currentHurtTime == 8) {
               this.sentParticles = false;
            }

            this.lastHurtTime = currentHurtTime;
         }

         float hp = PlayerIntersectionUtil.getHealth(displayTarget);
         float widthHp = 61.0F;
         float newTargetHealth = hp / displayTarget.getMaxHealth() * widthHp;
         if (this.health == 0.0F || Math.abs(this.targetHealth - newTargetHealth) > 0.1F) {
            if (this.health == 0.0F) {
               this.health = newTargetHealth;
               this.targetHealth = newTargetHealth;
               this.animatedHealth = newTargetHealth;
            } else {
               this.targetHealth = newTargetHealth;
               this.healthAnimation.reset();
               this.healthAnimation.setDirection(Direction.FORWARDS);
            }
         }
      }

      if (mc.currentScreen != null && mc.player != null && mc.player.isAlive() && PlayerIntersectionUtil.isChat(mc.currentScreen)) {
         this.lastTarget = mc.player;
         this.startAnimation();
      } else {
         boolean shouldShow = displayTarget != null;
         if (shouldShow && !VisibleUtils.canBeTargeted(displayTarget)) {
            shouldShow = false;
         }

         if (shouldShow && hudModule.displayMode.getSelected().equals("3D")) {
            shouldShow = ProjectionUtil.canSeeEntity(displayTarget);
         }

         if (shouldShow) {
            this.startAnimation();
         } else {
            this.stopAnimation();
         }
      }
   }

   private void drawUsingItem(DrawContext context, MatrixStack matrix) {
      if (this.lastTarget != null && ServerLimitCfg.showItemUsingProgress()) {
         this.animation.setDirection(this.lastTarget.isUsingItem() ? Direction.FORWARDS : Direction.BACKWARDS);
         if (!this.lastTarget.getActiveItem().isEmpty() && this.lastTarget.getActiveItem().getCount() != 0) {
            this.lastItem = this.lastTarget.getActiveItem().getItem();
         }

         if (!this.animation.isFinished(Direction.BACKWARDS) && !this.lastItem.equals(Items.AIR)) {
            int size = 24;
            float anim = this.animation.getOutput().floatValue();
            float progress = (this.lastTarget.getItemUseTime() + tickCounter.getTickDelta(false)) / ItemUtil.maxUseTick(this.lastItem) * 360.0F;
            float x = -(size + 5) * anim;
            float y = 6.0F;
            ScissorManager scissorManager = Main.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), -50.0F, 0.0F, 50.0F, this.getHeight());
            MathUtil.setAlpha(
               anim,
               () -> {
                  blur.render(
                     ShapeProperties.create(matrix, x, y, size, size)
                        .round(12.0F)
                        .softness(1.0F)
                        .thickness(2.0F)
                        .outlineColor(ColorUtil.getOutline())
                        .color(ColorUtil.getRect(0.7F))
                        .build()
                  );
                  arc.render(
                     ShapeProperties.create(matrix, x, y, size, size)
                        .round(0.4F)
                        .thickness(0.2F)
                        .end(progress)
                        .color(ColorUtil.fade(0), ColorUtil.fade(200), ColorUtil.fade(0), ColorUtil.fade(200))
                        .build()
                  );
                  Render2DUtil.defaultDrawStack(context, this.lastItem.getDefaultStack(), x + 3.0F, y + 3.0F, false, false, 1.0F);
               }
            );
            scissorManager.pop();
         }
      }
   }

   private void updateDimensionsForStyle() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      String selectedStyle = hudModule.style.getSelected();
      switch (selectedStyle) {
         case "Default":
            this.setWidth(100);
            this.setHeight(36);
            break;
         case "Round":
            this.setWidth(120);
            this.setHeight(46);
      }
   }

   private LivingEntity getDisplayTarget() {
      if (this.targetEntity != null) {
         return this.targetEntity;
      }

      if (this.lastTarget != null) {
         padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
         long currentTime = System.currentTimeMillis();
         long liveTimeMs = (long)(hudModule.liveTime.getValue() * 1000.0F);
         if (hudModule.liveTime.getValue() == 0.0F) {
            return null;
         }

         if (currentTime - this.lastTargetTime < liveTimeMs) {
            if (!this.lastTarget.isDead() && this.lastTarget.isAlive()) {
               return this.lastTarget;
            }

            return null;
         }
      }

      return null;
   }

   @Override
   protected float getInteractionScale() {
      padej.soup.implement.features.modules.hud.TargetHud hudModule = padej.soup.implement.features.modules.hud.TargetHud.getInstance();
      return hudModule.scale.getValue();
   }

   @Override
   public float getDraggableScale() {
      return 1.0F;
   }

   public StopWatch getStopWatch() {
      return this.stopWatch;
   }

   private record ParticlePair2D(TargetHudParticle p1, TargetHudParticle p2) {
      private ParticlePair2D {
         if (System.identityHashCode(p1) < System.identityHashCode(p2)) {
            ;
         }
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            TargetHud.ParticlePair2D that = (TargetHud.ParticlePair2D)o;
            return this.p1 == that.p1 && this.p2 == that.p2;
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return 31 * System.identityHashCode(this.p1) + System.identityHashCode(this.p2);
      }
   }

   private record ParticleRenderData(TargetHudParticle particle, float x, float y, float scale) {
   }
}
