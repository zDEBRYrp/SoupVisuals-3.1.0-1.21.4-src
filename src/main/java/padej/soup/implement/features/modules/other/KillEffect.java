package padej.soup.implement.features.modules.other;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import padej.soup.api.event.EventHandler;
import padej.soup.api.feature.module.IParticleModule;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.implement.BooleanSetting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.feature.module.setting.implement.MultiColorSetting;
import padej.soup.api.feature.module.setting.implement.MultiSelectSetting;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.feature.module.setting.implement.ValueSetting;
import padej.soup.api.repository.friend.FriendUtils;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.base.util.particle.ParticleUpdateExecutor;
import padej.soup.base.util.spatial.SpatialGrid3D;
import padej.soup.implement.events.player.EventAttack;
import padej.soup.implement.events.player.TickEvent;
import padej.soup.implement.events.render.WorldRenderEvent;
import padej.soup.implement.features.modules.particles.ParticleData;
import padej.soup.implement.features.modules.particles.render.NetworkRenderer;
import padej.soup.implement.features.modules.particles.types.WorldParticle;

public class KillEffect extends Module implements IParticleModule {
   private final Map<Entity, KillEffect.EntityHealthData> trackedEntities = new ConcurrentHashMap<>();
   private final Map<Entity, Long> recentlyAttacked = new ConcurrentHashMap<>();
   private final Map<Entity, Long> recentlyAttackedByFriends = new ConcurrentHashMap<>();
   private static final long ATTACK_TIMEOUT = 5000L;
   private final List<WorldParticle> particles = new ArrayList<>();
   private final List<WorldParticle> networkParticles = new ArrayList<>();
   private SpatialGrid3D<WorldParticle> spatialGrid = null;
   private final SelectSetting effectType = new SelectSetting("setting.killeffect.effecttype.name", "setting.killeffect.effecttype.desc")
      .value("Particles", "Thunder")
      .selected("Particles");
   private final SelectSetting friendEffectType = new SelectSetting("setting.killeffect.friendeffecttype.name", "setting.killeffect.friendeffecttype.desc")
      .value("Particles", "Thunder")
      .selected("Thunder");
   private final BooleanSetting showForFriends = new BooleanSetting("setting.killeffect.showforfriends.name", "setting.killeffect.showforfriends.desc")
      .setValue(true);
   private final SelectSetting targetType = new SelectSetting("setting.killeffect.targettype.name", "setting.killeffect.targettype.desc")
      .value("Mobs", "Player", "Both")
      .selected("Both");
   public final SelectSetting colorMode = new SelectSetting("setting.killeffect.colormode.name", "setting.killeffect.colormode.desc")
      .value("Sync", "Custom")
      .selected("Sync")
      .visible(this::isParticleMode);
   private final SelectSetting customColorsCount = new SelectSetting("setting.killeffect.colorcount.name", "setting.killeffect.colorcount.desc")
      .value("Solo", "Duo", "Triple", "Quartet")
      .selected("Solo")
      .visible(() -> this.colorMode.isSelected("Custom") && this.isParticleMode());
   private final MultiColorSetting customColors = new MultiColorSetting("setting.killeffect.gradientcolors.name", "setting.killeffect.gradientcolors.desc")
      .colors("Color 1", "Color 2", "Color 3", "Color 4")
      .defaultColors(-1499549, -13273872, -6596170, -409301)
      .visible(() -> this.colorMode.isSelected("Custom") && this.isParticleMode());
   public final SelectSetting colorAnimation = new SelectSetting("setting.killeffect.coloranimation.name", "setting.killeffect.coloranimation.desc")
      .value("Wave", "Vertex")
      .selected("Wave")
      .visible(() -> this.colorMode.isSelected("Custom") && this.isParticleMode());
   private final MultiSelectSetting mode = new MultiSelectSetting("setting.killeffect.mode.name", "setting.killeffect.mode.desc")
      .value(
         "Stars",
         "Hearts",
         "Bloom",
         "Glyph",
         "Things",
         "Blink",
         "Coron",
         "Dollar",
         "Flame",
         "Geometric",
         "Snowflake",
         "Logo",
         "Virus",
         "SoupAPI Old",
         "Sword",
         "Network",
         "Cube",
         "Pyramid"
      )
      .selected("Stars", "Hearts", "Bloom")
      .visible(this::isParticleMode);
   private final MultiSelectSetting physics = new MultiSelectSetting("setting.killeffect.physics.name", "setting.killeffect.physics.desc")
      .value("Fall", "Fly", "Emerge")
      .selected("Emerge")
      .visible(this::isParticleMode);
   private final ValueSetting scale = new ValueSetting("setting.killeffect.scale.name", "setting.killeffect.scale.desc")
      .setValue(1.5F)
      .range(0.5F, 5.0F)
      .visible(this::isParticleMode);
   private final ValueSetting lifeTime = new ValueSetting("setting.killeffect.lifetime.name", "setting.killeffect.lifetime.desc")
      .setValue(3.0F)
      .range(1, 10)
      .visible(this::isParticleMode);
   private final ValueSetting speed = new ValueSetting("setting.killeffect.speed.name", "setting.killeffect.speed.desc")
      .setValue(1.0F)
      .range(0.1F, 3.0F)
      .visible(this::isParticleMode);
   private final ValueSetting amount = new ValueSetting("setting.killeffect.amount.name", "setting.killeffect.amount.desc")
      .setValue(30.0F)
      .range(10, 70)
      .visible(this::isParticleMode);
   public final ValueSetting linkDistance = new ValueSetting("setting.killeffect.linkdistance.name", "setting.killeffect.linkdistance.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(() -> this.isParticleMode() && this.mode.getSelected().contains("Network"));
   private final BooleanSetting enableSound = new BooleanSetting("setting.killeffect.enablesound.name", "setting.killeffect.enablesound.desc").setValue(false);
   private final SelectSetting soundType = new SelectSetting("setting.killeffect.soundtype.name", "setting.killeffect.soundtype.desc")
      .value("Abmiss", "Critow", "Final Blink", "Final Pok", "Neptune", "Rust Headshot", "Whii")
      .selected("Neptune")
      .visible(this.enableSound::isValue);
   private final ValueSetting soundVolume = new ValueSetting("setting.killeffect.soundvolume.name", "setting.killeffect.soundvolume.desc")
      .setValue(1.0F)
      .range(0.1F, 2.0F)
      .visible(this.enableSound::isValue);
   private final ValueSetting soundPitch = new ValueSetting("setting.killeffect.soundpitch.name", "setting.killeffect.soundpitch.desc")
      .setValue(1.0F)
      .range(0.5F, 2.0F)
      .visible(this.enableSound::isValue);

   private boolean isParticleMode() {
      return this.effectType.isSelected("Particles") || this.showForFriends.isValue() && this.friendEffectType.isSelected("Particles");
   }

   public static KillEffect getInstance() {
      return Instance.get(KillEffect.class);
   }

   public KillEffect() {
      super("module.killeffect.name", ModuleCategory.OTHER);
      GroupSetting effectGroup = new GroupSetting("group.killeffect.effect.name", "group.killeffect.effect.desc", false)
         .settings(this.effectType, this.friendEffectType, this.showForFriends, this.targetType);
      GroupSetting colorGroup = new GroupSetting("group.killeffect.colors.name", "group.killeffect.colors.desc", false)
         .settings(this.colorMode, this.customColorsCount, this.customColors, this.colorAnimation)
         .visible(this::isParticleMode);
      GroupSetting appearanceGroup = new GroupSetting("group.killeffect.appearance.name", "group.killeffect.appearance.desc", false)
         .settings(this.mode, this.physics, this.scale)
         .visible(this::isParticleMode);
      GroupSetting behaviorGroup = new GroupSetting("group.killeffect.behavior.name", "group.killeffect.behavior.desc", false)
         .settings(this.lifeTime, this.speed, this.amount)
         .visible(this::isParticleMode);
      GroupSetting soundGroup = new GroupSetting("group.killeffect.sound.name", "group.killeffect.sound.desc", false)
         .settings(this.enableSound, this.soundType, this.soundVolume, this.soundPitch);
      this.setup(effectGroup, colorGroup, appearanceGroup, behaviorGroup, this.linkDistance, soundGroup);
   }

   @Override
   public SelectSetting getColorMode() {
      return this.colorMode;
   }

   @Override
   public SelectSetting getColorAnimation() {
      return this.colorAnimation;
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

   private String getRandomMode() {
      List<String> selected = this.mode.getSelected();
      return selected.isEmpty() ? "Stars" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   private String getRandomPhysics() {
      List<String> selected = this.physics.getSelected();
      return selected.isEmpty() ? "Emerge" : selected.get(ThreadLocalRandom.current().nextInt(selected.size()));
   }

   @EventHandler
   public void onAttack(EventAttack event) {
      if (mc.player != null && mc.world != null) {
         if (!event.isPre()) {
            Entity target = event.getTarget();
            if (target instanceof LivingEntity) {
               this.recentlyAttacked.put(target, System.currentTimeMillis());
            }
         }
      }
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         long currentTime = System.currentTimeMillis();
         ParticleUpdateExecutor.updateParticlesInPlace(this.particles, currentTime, p -> true, null, particle -> {
            if ("Network".equals(particle.getParticleMode())) {
               if (this.spatialGrid != null) {
                  this.spatialGrid.remove(particle, particle.getX(), particle.getY(), particle.getZ());
               }

               this.networkParticles.remove(particle);
            }
         });
         if (this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            this.networkParticles
               .forEach(
                  particle -> this.spatialGrid
                     .update(particle, particle.getPx(), particle.getPy(), particle.getPz(), particle.getX(), particle.getY(), particle.getZ())
               );
         }

         this.recentlyAttacked.entrySet().removeIf(entry -> currentTime - entry.getValue() > 5000L);
         this.recentlyAttackedByFriends.entrySet().removeIf(entry -> currentTime - entry.getValue() > 5000L);

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && entity != mc.player && entity.isAlive() && !livingEntity.isDead()) {
               float currentHealth = livingEntity.getHealth();
               KillEffect.EntityHealthData data = this.trackedEntities
                  .computeIfAbsent(entity, k -> new KillEffect.EntityHealthData(currentHealth, livingEntity.getMaxHealth()));
               if (currentHealth < data.previousHealth) {
                  Entity attacker = livingEntity.getAttacker();
                  if (attacker instanceof PlayerEntity && FriendUtils.isFriend(attacker)) {
                     this.recentlyAttackedByFriends.put(entity, currentTime);
                  }
               }

               if (data.wasAlive && !livingEntity.isAlive()) {
                  if (this.recentlyAttacked.containsKey(entity)) {
                     this.onEntityKilled(entity, livingEntity, false);
                  } else if (this.recentlyAttackedByFriends.containsKey(entity)) {
                     this.onEntityKilled(entity, livingEntity, true);
                  }
               } else if (data.previousHealth > 0.0F && currentHealth <= 0.0F) {
                  if (this.recentlyAttacked.containsKey(entity)) {
                     this.onEntityKilled(entity, livingEntity, false);
                  } else if (this.recentlyAttackedByFriends.containsKey(entity)) {
                     this.onEntityKilled(entity, livingEntity, true);
                  }
               } else if (data.previousHealth > 2.0F && currentHealth <= 2.0F) {
                  if (this.recentlyAttacked.containsKey(entity)) {
                     data.markedForKill = true;
                     data.killedByFriend = false;
                     data.killMarkTime = currentTime;
                  } else if (this.recentlyAttackedByFriends.containsKey(entity)) {
                     data.markedForKill = true;
                     data.killedByFriend = true;
                     data.killMarkTime = currentTime;
                  }
               }

               data.previousHealth = currentHealth;
               data.wasAlive = livingEntity.isAlive();
            }
         }

         this.trackedEntities.entrySet().removeIf(entry -> {
            Entity ent = entry.getKey();
            KillEffect.EntityHealthData datax = entry.getValue();
            if (datax.markedForKill && ent.isRemoved()) {
               if (this.recentlyAttacked.containsKey(ent)) {
                  this.onEntityKilled(ent, (LivingEntity)ent, false);
               } else if (this.recentlyAttackedByFriends.containsKey(ent)) {
                  this.onEntityKilled(ent, (LivingEntity)ent, true);
               }

               return true;
            } else if (!ent.isRemoved() && ent.isAlive()) {
               if (datax.markedForKill && currentTime - datax.killMarkTime > 1000L) {
                  datax.markedForKill = false;
               }

               return false;
            } else {
               if (this.recentlyAttacked.containsKey(ent) && datax.previousHealth <= 2.0F) {
                  this.onEntityKilled(ent, (LivingEntity)ent, false);
               } else if (this.recentlyAttackedByFriends.containsKey(ent) && datax.previousHealth <= 2.0F) {
                  this.onEntityKilled(ent, (LivingEntity)ent, true);
               }

               return true;
            }
         });
      }
   }

   private void onEntityKilled(Entity entity, LivingEntity livingEntity, boolean killedByFriend) {
      this.recentlyAttacked.remove(entity);
      this.recentlyAttackedByFriends.remove(entity);
      this.trackedEntities.remove(entity);
      boolean isPlayer = entity instanceof PlayerEntity;
      String targetTypeSelected = this.targetType.getSelected();
      if (!targetTypeSelected.equals("Player") || isPlayer) {
         if (!targetTypeSelected.equals("Mobs") || !isPlayer) {
            if (!killedByFriend || this.showForFriends.isValue()) {
               String selectedEffectType = killedByFriend ? this.friendEffectType.getSelected() : this.effectType.getSelected();
               Vec3d position = entity.getPos().add(0.0, livingEntity.getHeight() / 2.0, 0.0);
               if (selectedEffectType.equals("Particles")) {
                  this.spawnParticlesEffect(position);
               } else if (selectedEffectType.equals("Thunder")) {
                  this.spawnThunderEffect(position);
               }

               if (this.enableSound.isValue()) {
                  this.playKillSound();
               }
            }
         }
      }
   }

   private void playKillSound() {
      SoundEvent sound = switch (this.soundType.getSelected()) {
         case "Abmiss" -> SoundManager.KILL_ABMISS;
         case "Critow" -> SoundManager.KILL_CRITOW;
         case "Final Blink" -> SoundManager.KILL_FINAL_BLINK;
         case "Final Pok" -> SoundManager.KILL_FINAL_POK;
         case "Neptune" -> SoundManager.KILL_NEPTUNE;
         case "Rust Headshot" -> SoundManager.KILL_RUST_HEADSHOT;
         case "Whii" -> SoundManager.KILL_WHII;
         default -> SoundManager.KILL_NEPTUNE;
      };
      SoundManager.playSound(sound, this.soundVolume.getValue(), this.soundPitch.getValue());
   }

   private void spawnParticlesEffect(Vec3d position) {
      int colorInt;
      if (this.colorMode.isSelected("Sync")) {
         colorInt = ColorUtil.getClientColor();
      } else {
         int[] colors = this.getCustomColors();
         colorInt = colors != null && colors.length > 0 ? colors[0] : -1;
      }

      for (int i = 0; i < this.amount.getValue(); i++) {
         String selectedMode = this.getRandomMode();
         String selectedPhysics = selectedMode.equals("Network") ? "Fly" : this.getRandomPhysics();
         float x = (float)position.x;
         float y = (float)position.y;
         float z = (float)position.z;
         ThreadLocalRandom random = ThreadLocalRandom.current();
         WorldParticle particle = new WorldParticle(
            x,
            y,
            z,
            new Color(colorInt),
            (float)(random.nextDouble() * 360.0),
            (float)(random.nextDouble() * 25.0 + 5.0),
            0.0F,
            selectedMode,
            selectedPhysics,
            selectedMode.equals("Glyph")
               ? ParticleData.getRandomGlyphTexture()
               : (selectedMode.equals("Things") ? ParticleData.getRandomGlyphAltTexture() : null),
            this.lifeTime.getValue(),
            this.scale.getValue(),
            this.speed.getValue(),
            this
         ) {
            @Override
            protected void initMotion(float speed) {
               ThreadLocalRandom rng = ThreadLocalRandom.current();
               float scale = speed * 0.05F;
               this.motionX = (float)(rng.nextGaussian() * scale);
               this.motionY = (float)(rng.nextGaussian() * scale);
               this.motionZ = (float)(rng.nextGaussian() * scale);
            }
         };
         this.particles.add(particle);
         if (selectedMode.equals("Network")) {
            if (this.spatialGrid == null) {
               this.spatialGrid = new SpatialGrid3D<>(this.linkDistance.getValue());
            }

            this.networkParticles.add(particle);
            this.spatialGrid.insert(particle, x, y, z);
         }
      }
   }

   private void spawnThunderEffect(Vec3d position) {
      if (mc.world != null) {
         LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
         lightning.refreshPositionAfterTeleport(position);
         lightning.setCosmetic(true);
         mc.world.addEntity(lightning);
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      MatrixStack stack = event.getStack();
      if (mc.player != null && mc.world != null) {
         long currentTime = System.currentTimeMillis();

         for (WorldParticle particle : this.particles) {
            particle.render(stack, currentTime);
         }

         if (this.mode.getSelected().contains("Network") && this.spatialGrid != null && !this.networkParticles.isEmpty()) {
            NetworkRenderer.renderNetworkLinks(stack, this.networkParticles, this.spatialGrid, currentTime, this.linkDistance.getValue(), this);
         }
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.particles.clear();
      this.networkParticles.clear();
      this.trackedEntities.clear();
      this.recentlyAttacked.clear();
      this.recentlyAttackedByFriends.clear();
      if (this.spatialGrid != null) {
         this.spatialGrid = null;
      }
   }

   public SpatialGrid3D<WorldParticle> getSpatialGrid() {
      return this.spatialGrid;
   }

   public SelectSetting getEffectType() {
      return this.effectType;
   }

   public SelectSetting getFriendEffectType() {
      return this.friendEffectType;
   }

   public BooleanSetting getShowForFriends() {
      return this.showForFriends;
   }

   public SelectSetting getTargetType() {
      return this.targetType;
   }

   public ValueSetting getLinkDistance() {
      return this.linkDistance;
   }

   public BooleanSetting getEnableSound() {
      return this.enableSound;
   }

   public SelectSetting getSoundType() {
      return this.soundType;
   }

   public ValueSetting getSoundVolume() {
      return this.soundVolume;
   }

   public ValueSetting getSoundPitch() {
      return this.soundPitch;
   }

   private static class EntityHealthData {
      float previousHealth;
      float maxHealth;
      boolean wasAlive;
      boolean markedForKill;
      boolean killedByFriend;
      long killMarkTime;

      EntityHealthData(float currentHealth, float maxHealth) {
         this.previousHealth = currentHealth;
         this.maxHealth = maxHealth;
         this.wasAlive = true;
         this.markedForKill = false;
         this.killedByFriend = false;
         this.killMarkTime = 0L;
      }
   }
}
