package padej.soup.implement.features.modules.visuals;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import padej.soup.api.event.EventHandler;
import padej.soup.api.feature.module.ITargetRenderModule;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.base.util.math.ProjectionUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.render.TargetRenderer;
import padej.soup.implement.events.player.TickEvent;
import padej.soup.implement.events.render.WorldRenderEvent;
import padej.soup.implement.features.modules.particles.render.ParticleBatchRenderer;

public class TargetRender extends Module implements ITargetRenderModule {
   private LivingEntity targetEntity;
   private LivingEntity lastTargetEntity;
   private long lastTargetTime;
   private final Animation renderAnimation = new DecelerateAnimation().setMs(500).setValue(1.0);
   private final SelectSetting colorMode = new SelectSetting("setting.targetrender.colormode.name", "setting.targetrender.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync");
   private final SelectSetting customColorsCount = new SelectSetting("setting.targetrender.colorcount.name", "setting.targetrender.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final MultiColorSetting customColors = new MultiColorSetting("setting.targetrender.gradientcolors.name", "setting.targetrender.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom"));
   private final SelectSetting targetRenderType = new SelectSetting("setting.targetrender.rendertype.name", "setting.targetrender.rendertype.desc")
      .value("Legacy", "Circle", "Ghosts", "Crystals")
      .selected("Legacy");
   private final SelectSetting legacyTexture = new SelectSetting("setting.targetrender.legacytexture.name", "setting.targetrender.legacytexture.desc")
      .value("Amogus", "Bo", "Capture", "Jeka", "Marker", "Scifi", "Simple", "Skull", "Vegas", "Rockfly")
      .selected("Capture");
   private final BooleanSetting optimalAim = new BooleanSetting("setting.targetrender.optimalaim.name", "setting.targetrender.optimalaim.desc").setValue(false);
   private final BooleanSetting staticMode = new BooleanSetting("setting.targetrender.staticmode.name", "setting.targetrender.staticmode.desc").setValue(false);
   private final ValueSetting legacySize = new ValueSetting("setting.targetrender.legacysize.name", "setting.targetrender.legacysize.desc")
      .setValue(1.0F)
      .range(0.5F, 1.5F);
   private final SelectSetting ghostsTexture = new SelectSetting("setting.targetrender.ghoststexture.name", "setting.targetrender.ghoststexture.desc")
      .value("Bloom", "Soft")
      .selected("Soft");
   private final SelectSetting ghostsBlend = new SelectSetting("setting.targetrender.ghostsblend.name", "setting.targetrender.ghostsblend.desc")
      .value("Smoke", "Plasma")
      .selected("Plasma");
   private final ValueSetting ghostsLength = new ValueSetting("setting.targetrender.ghostslength.name", "setting.targetrender.ghostslength.desc")
      .setValue(1.3F)
      .range(0.5F, 1.5F);
   private final SelectSetting ghostsTrajectory = new SelectSetting("setting.targetrender.ghoststrajectory.name", "setting.targetrender.ghoststrajectory.desc")
      .value("Standard", "Spiral", "Atomic")
      .selected("Standard");
   private final ValueSetting ghostsRadiusModifier = new ValueSetting(
         "setting.targetrender.ghostsradiusmodifier.name", "setting.targetrender.ghostsradiusmodifier.desc"
      )
      .setValue(1.6F)
      .range(0.5F, 2.0F);
   private final ValueSetting ghostsEndSize = new ValueSetting("setting.targetrender.ghostsstartsize.name", "setting.targetrender.ghostsstartsize.desc")
      .setValue(0.1F)
      .range(0.1F, 1.0F);
   private final ValueSetting ghostsStartSize = new ValueSetting("setting.targetrender.ghostsendsize.name", "setting.targetrender.ghostsendsize.desc")
      .setValue(0.7F)
      .range(0.1F, 1.0F);
   private final ValueSetting ghostsCount = new ValueSetting("setting.targetrender.ghostscount.name", "setting.targetrender.ghostscount.desc")
      .setValue(4.0F)
      .range(1, 8);
   private final ValueSetting ghostsSubdivision = new ValueSetting("setting.targetrender.ghostssubdivision.name", "setting.targetrender.ghostssubdivision.desc")
      .setValue(5.0F)
      .range(1, 8);
   private final ValueSetting crystalsDistance = new ValueSetting("setting.targetrender.crystalsdistance.name", "setting.targetrender.crystalsdistance.desc")
      .setValue(0.7F)
      .range(0.1F, 1.0F);
   private final ValueSetting crystalsSize = new ValueSetting("setting.targetrender.crystalssize.name", "setting.targetrender.crystalssize.desc")
      .setValue(0.35F)
      .range(0.2F, 0.4F);
   private final BooleanSetting crystalsGlow = new BooleanSetting("setting.targetrender.crystalsglow.name", "setting.targetrender.crystalsglow.desc")
      .setValue(true);
   private final ValueSetting crystalsGlowSize = new ValueSetting("setting.targetrender.crystalsglowsize.name", "setting.targetrender.crystalsglowsize.desc")
      .setValue(1.5F)
      .range(1.5F, 3.0F);
   private final SelectSetting crystalsOrientation = new SelectSetting(
         "setting.targetrender.crystalsorientation.name", "setting.targetrender.crystalsorientation.desc"
      )
      .value("Center", "Horizontal")
      .selected("Center");
   private final ValueSetting followTime = new ValueSetting("setting.targetrender.followtime.name", "setting.targetrender.followtime.desc")
      .setValue(0.0F)
      .range(0, 10);
   private final ValueSetting speedMod = new ValueSetting("setting.targetrender.speedmod.name", "setting.targetrender.speedmod.desc")
      .setValue(0.5F)
      .range(0.5F, 2.0F);
   private final ValueSetting maxDistance = new ValueSetting("setting.targetrender.maxdistance.name", "setting.targetrender.maxdistance.desc")
      .setValue(64.0F)
      .range(16.0F, 128.0F);
   private final BooleanSetting disableRedEffect = new BooleanSetting(
         "setting.targetrender.disableredeffect.name", "setting.targetrender.disableredeffect.desc"
      )
      .setValue(false);

   public static TargetRender getInstance() {
      return Instance.get(TargetRender.class);
   }

   public TargetRender() {
      super("module.targetrender.name", ModuleCategory.VISUALS);
      GroupSetting colorGroup = new GroupSetting("group.targetrender.colors.name", "group.targetrender.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors);
      GroupSetting legacyGroup = new GroupSetting("group.targetrender.legacy.name", "group.targetrender.legacy.desc", false)
         .settings(this.legacyTexture, this.optimalAim, this.staticMode, this.legacySize)
         .visible(() -> this.targetRenderType.isSelected("Legacy"));
      GroupSetting ghostsVisualGroup = new GroupSetting("group.targetrender.ghosts.visual.name", "group.targetrender.ghosts.visual.desc", false)
         .settings(this.ghostsTexture, this.ghostsBlend);
      GroupSetting ghostsAnimationGroup = new GroupSetting("group.targetrender.ghosts.animation.name", "group.targetrender.ghosts.animation.desc", false)
         .settings(this.ghostsLength, this.ghostsTrajectory);
      GroupSetting ghostsSizeGroup = new GroupSetting("group.targetrender.ghosts.size.name", "group.targetrender.ghosts.size.desc", false)
         .settings(this.ghostsRadiusModifier, this.ghostsStartSize, this.ghostsEndSize, this.ghostsCount, this.ghostsSubdivision);
      GroupSetting ghostsGroup = new GroupSetting("group.targetrender.ghosts.name", "group.targetrender.ghosts.desc", false)
         .settings(ghostsVisualGroup, ghostsAnimationGroup, ghostsSizeGroup)
         .visible(() -> this.targetRenderType.isSelected("Ghosts"));
      GroupSetting crystalsGroup = new GroupSetting("group.targetrender.crystals.name", "group.targetrender.crystals.desc", false)
         .settings(this.crystalsDistance, this.crystalsSize, this.crystalsGlow, this.crystalsGlowSize, this.crystalsOrientation)
         .visible(() -> this.targetRenderType.isSelected("Crystals"));
      GroupSetting behaviorGroup = new GroupSetting("group.targetrender.behavior.name", "group.targetrender.behavior.desc", false)
         .settings(this.followTime, this.speedMod, this.maxDistance, this.disableRedEffect);
      this.setup(colorGroup, this.targetRenderType, legacyGroup, ghostsGroup, crystalsGroup, behaviorGroup);
   }

   @Override
   public void deactivate() {
      this.targetEntity = null;
      this.lastTargetEntity = null;
      this.lastTargetTime = 0L;
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent e) {
      if (!this.shouldSkipDueToPriority()) {
         boolean shouldRender = this.canRenderTarget(this.targetEntity);
         if (this.targetEntity == null && this.lastTargetEntity != null) {
            long currentTime = System.currentTimeMillis();
            long followTimeMs = (long)(this.followTime.getValue() * 1000.0F);
            shouldRender = currentTime - this.lastTargetTime < followTimeMs && this.canRenderTarget(this.lastTargetEntity);
         }

         this.renderAnimation.setDirection(shouldRender ? Direction.FORWARDS : Direction.BACKWARDS);
         float animationDelta = this.renderAnimation.getOutput().floatValue();
         if (this.targetEntity != null) {
            this.lastTargetEntity = this.targetEntity;
         }

         if (this.lastTargetEntity != null && !this.renderAnimation.isFinished(Direction.BACKWARDS)) {
            float tickDelta = tickCounter.getTickDelta(false);
            float red = this.disableRedEffect.isValue() ? 0.0F : MathHelper.clamp((this.lastTargetEntity.hurtTime - tickDelta) / 10.0F, 0.0F, 1.0F);
            switch (this.targetRenderType.getSelected()) {
               case "Legacy":
                  TargetRenderer.drawLegacy(this.lastTargetEntity, animationDelta, red, this);
                  break;
               case "Circle":
                  TargetRenderer.drawCircle(e.getStack(), this.lastTargetEntity, animationDelta, red, this);
                  break;
               case "Ghosts":
                  TargetRenderer.drawGhosts(
                     this.lastTargetEntity,
                     animationDelta,
                     red,
                     this.speedMod.getValue(),
                     this.ghostsLength.getValue(),
                     this.ghostsRadiusModifier.getValue(),
                     this.ghostsStartSize.getValue(),
                     this.ghostsEndSize.getValue(),
                     this.ghostsSubdivision.getValue(),
                     this
                  );
                  ParticleBatchRenderer.renderBatches();
                  break;
               case "Crystals":
                  TargetRenderer.drawCrystals(e, this.lastTargetEntity, animationDelta, red, this);
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      LivingEntity previousTarget = this.targetEntity;
      if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
         if (!(entityHit.getEntity() instanceof LivingEntity entity && !entity.isDead() && entity.isAlive())) {
            this.targetEntity = null;
         } else if (this.canRenderTarget(entity)) {
            this.targetEntity = entity;
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

      TargetRenderer.updateAnimations(this.speedMod.getValue());
   }

   private boolean canRenderTarget(LivingEntity entity) {
      if (entity == null || mc.player == null) {
         return false;
      } else if (!entity.isDead() && entity.isAlive()) {
         return mc.player.getPos().squaredDistanceTo(entity.getPos()) > this.maxDistance.getValue() * this.maxDistance.getValue()
            ? false
            : ProjectionUtil.canSeeEntity(entity, this.maxDistance.getValue());
      } else {
         return false;
      }
   }

   @Override
   public int[] getCustomColors() {
      if (!this.colorMode.isSelected("Custom")) {
         return null;
      }

      return switch (this.customColorsCount.getSelected()) {
         case "Solo" -> new int[]{this.customColors.getColor1().getColor()};
         case "Duo" -> new int[]{this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor()};
         case "Triple" -> new int[]{
            this.customColors.getColor1().getColor(), this.customColors.getColor2().getColor(), this.customColors.getColor3().getColor()
         };
         case "Quartet" -> this.customColors.getColorValues();
         default -> null;
      };
   }

   @Override
   public boolean isOptimalAim() {
      return this.optimalAim.isValue();
   }

   @Override
   public boolean isStaticMode() {
      return this.staticMode.isValue();
   }

   @Override
   public float getLegacySize() {
      return this.legacySize.getValue();
   }

   public boolean isRedEffectDisabled() {
      return this.disableRedEffect.isValue();
   }

   @Override
   public Identifier getGhostTexture() {
      return switch (this.ghostsTexture.getSelected()) {
         case "Bloom" -> Identifier.of("textures/particles/bloom/bloom.png");
         case "Soft" -> Identifier.of("textures/particles/bloom/bloom_soft.png");
         default -> null;
      };
   }

   @Override
   public Identifier getLegacyTexture() {
      return switch (this.legacyTexture.getSelected()) {
         case "Amogus" -> Identifier.of("textures/legacy/amongus.png");
         case "Bo" -> Identifier.of("textures/legacy/bo.png");
         case "Capture" -> Identifier.of("textures/legacy/capture.png");
         case "Jeka" -> Identifier.of("textures/legacy/jeka.png");
         case "Marker" -> Identifier.of("textures/legacy/marker.png");
         case "Scifi" -> Identifier.of("textures/legacy/scifi.png");
         case "Simple" -> Identifier.of("textures/legacy/simple.png");
         case "Skull" -> Identifier.of("textures/legacy/skull.png");
         case "Vegas" -> Identifier.of("textures/legacy/vegas.png");
         case "Rockfly" -> Identifier.of("textures/legacy/rockfly.png");
         default -> null;
      };
   }

   @Override
   public float getCrystalsDistance() {
      return this.crystalsDistance.getValue();
   }

   @Override
   public float getCrystalsSize() {
      return this.crystalsSize.getValue();
   }

   @Override
   public boolean isCrystalsGlow() {
      return this.crystalsGlow.isValue();
   }

   @Override
   public float getCrystalsGlowSize() {
      return this.crystalsGlowSize.getValue();
   }

   @Override
   public boolean isCrystalsHorizontal() {
      return this.crystalsOrientation.isSelected("Horizontal");
   }

   private boolean shouldSkipDueToPriority() {
      FriendsTargetRender friendsTargetRender = FriendsTargetRender.getInstance();
      if (friendsTargetRender == null || !friendsTargetRender.isEnabled()) {
         return false;
      }

      if (this.targetEntity == null) {
         return false;
      }

      boolean friendTargetingSame = friendsTargetRender.isFriendTargeting(this.targetEntity);
      if (!friendTargetingSame) {
         return false;
      }

      String priority = friendsTargetRender.getRenderPriority().getSelected();
      return "Friend".equals(priority);
   }

   public LivingEntity getTargetEntity() {
      return this.targetEntity;
   }

   public LivingEntity getLastTargetEntity() {
      return this.lastTargetEntity;
   }

   public long getLastTargetTime() {
      return this.lastTargetTime;
   }

   public Animation getRenderAnimation() {
      return this.renderAnimation;
   }

   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   public SelectSetting getCustomColorsCount() {
      return this.customColorsCount;
   }

   public SelectSetting getTargetRenderType() {
      return this.targetRenderType;
   }

   public BooleanSetting getOptimalAim() {
      return this.optimalAim;
   }

   public BooleanSetting getStaticMode() {
      return this.staticMode;
   }

   public SelectSetting getGhostsTexture() {
      return this.ghostsTexture;
   }

   @Override
   public SelectSetting getGhostsBlend() {
      return this.ghostsBlend;
   }

   public ValueSetting getGhostsLength() {
      return this.ghostsLength;
   }

   @Override
   public SelectSetting getGhostsTrajectory() {
      return this.ghostsTrajectory;
   }

   public ValueSetting getGhostsRadiusModifier() {
      return this.ghostsRadiusModifier;
   }

   public ValueSetting getGhostsEndSize() {
      return this.ghostsEndSize;
   }

   public ValueSetting getGhostsStartSize() {
      return this.ghostsStartSize;
   }

   @Override
   public ValueSetting getGhostsCount() {
      return this.ghostsCount;
   }

   public ValueSetting getGhostsSubdivision() {
      return this.ghostsSubdivision;
   }

   public BooleanSetting getCrystalsGlow() {
      return this.crystalsGlow;
   }

   public SelectSetting getCrystalsOrientation() {
      return this.crystalsOrientation;
   }

   public ValueSetting getFollowTime() {
      return this.followTime;
   }

   public ValueSetting getSpeedMod() {
      return this.speedMod;
   }

   public ValueSetting getMaxDistance() {
      return this.maxDistance;
   }

   public BooleanSetting getDisableRedEffect() {
      return this.disableRedEffect;
   }
}
