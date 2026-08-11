package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.setting.Setting;
import padej.soup.api.feature.module.setting.implement.GroupSetting;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.LinearAnimation;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.core.Main;

public class ModulesList extends AbstractDraggable {
   private final Map<Module, Boolean> moduleEnabledStates = new HashMap<>();
   private final Map<String, Animation> categoryAnimations = new HashMap<>();
   private final Map<Module, Float> targetYPositions = new HashMap<>();
   private final Map<Module, Float> currentYPositions = new HashMap<>();
   private final Map<String, Float> categoryTargetYPositions = new HashMap<>();
   private final Map<String, Float> categoryCurrentYPositions = new HashMap<>();
   private ArrayList<ModulesList.ModuleEntry> sortedModules = new ArrayList<>();
   private boolean positionInitialized = false;
   private float cachedMaxWidth = 0.0F;
   private boolean lastLowercaseState = false;
   private final Map<String, Float> moduleNameWidthCache = new HashMap<>();
   private final Map<Module, Integer> moduleSettingsCountCache = new HashMap<>();
   private String lastSortMode = "";
   private long lastSortUpdate = 0L;
   private static final long SORT_UPDATE_INTERVAL = 33L;
   private static final float TARGET_FRAME_TIME_30FPS = 33.333332F;
   private static final float BASE_POSITION_SMOOTHNESS_30FPS = 0.3F;
   private long lastPositionUpdateTime = System.currentTimeMillis();
   private boolean needsResort = true;

   public ModulesList() {
      super("ModulesList", 0, 3, 100, 100, true);
   }

   @Override
   public void tick() {
      boolean stateChanged = false;
      List<Module> modules = Main.getInstance().getModuleProvider().getModules();
      this.moduleEnabledStates.keySet().removeIf(module -> !modules.contains(module));

      for (Module module : modules) {
         boolean enabled = module.isEnabled();
         Boolean previous = this.moduleEnabledStates.put(module, enabled);
         if (previous == null || previous != enabled) {
            stateChanged = true;
         }
      }

      boolean hudEnabled = this.hasAnyCategoryEnabled("HUD");
      boolean worldEnabled = this.hasAnyCategoryEnabled("WORLD");
      boolean hudChanged = this.updateCategoryAnimation("HUD", hudEnabled);
      boolean worldChanged = this.updateCategoryAnimation("World", worldEnabled);
      if (stateChanged || hudChanged || worldChanged) {
         this.needsResort = true;
      }
   }

   private float getCachedModuleNameWidth(Module module, FontRenderer font, padej.soup.implement.features.modules.hud.ModulesList modulesListModule) {
      String name = this.getModuleName(module, modulesListModule);
      return this.moduleNameWidthCache.computeIfAbsent(name, font::getStringWidth);
   }

   private int getCachedSettingsCount(Module module) {
      return this.moduleSettingsCountCache.computeIfAbsent(module, this::countAllSettings);
   }

   private void clearNameWidthCache() {
      this.moduleNameWidthCache.clear();
      this.cachedMaxWidth = 0.0F;
   }

   private boolean updateCategoryAnimation(String categoryName, boolean shouldShow) {
      Direction newDirection = shouldShow ? Direction.FORWARDS : Direction.BACKWARDS;
      if (!this.categoryAnimations.containsKey(categoryName)) {
         Animation animation = new LinearAnimation().setMs(100).setValue(1.0);
         animation.setDirection(newDirection);
         this.categoryAnimations.put(categoryName, animation);
         return true;
      } else {
         Animation animation = this.categoryAnimations.get(categoryName);
         if (!animation.isDirection(newDirection)) {
            animation.setDirection(newDirection);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean hasAnyCategoryEnabled(String categoryName) {
      return Main.getInstance()
         .getModuleProvider()
         .getModules()
         .stream()
         .anyMatch(module -> module.getCategory().name().equals(categoryName) && module.isEnabled());
   }

   private void updateSortedModules() {
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_BOLD);
      padej.soup.implement.features.modules.hud.ModulesList modulesListModule = padej.soup.implement.features.modules.hud.ModulesList.getInstance();
      String currentSortMode = modulesListModule.getSortMode().getSelected();
      long currentTime = System.currentTimeMillis();
      boolean sortModeChanged = !currentSortMode.equals(this.lastSortMode);
      boolean enoughTimePassed = currentTime - this.lastSortUpdate >= 33L;
      if (sortModeChanged) {
         this.lastSortMode = currentSortMode;
      }

      ArrayList<ModulesList.ModuleEntry> moduleEntries = Main.getInstance()
         .getModuleProvider()
         .getModules()
         .stream()
         .filter(module -> !module.getCategory().name().equals("HUD"))
         .filter(module -> !module.getCategory().name().equals("WORLD"))
         .filter(
            module -> {
               String className = module.getClass().getSimpleName();
               return !className.equals("Language")
                  && !className.equals("Bright")
                  && !className.equals("Fog")
                  && !className.equals("Time")
                  && !className.equals("Light");
            }
         )
         .map(module -> {
            Animation animation = module.getAnimation();
            if (animation == null || !module.isEnabled() && !(animation.getOutput() > 0.01)) {
               return null;
            }

            this.currentYPositions.putIfAbsent(module, 0.0F);
            float currentY = this.currentYPositions.getOrDefault(module, 0.0F);
            float targetYx = this.targetYPositions.getOrDefault(module, 0.0F);
            return new ModulesList.ModuleEntry(module, animation, targetYx, currentY);
         })
         .filter(Objects::nonNull)
         .collect(Collectors.toCollection(ArrayList::new));
      ArrayList<ModulesList.ModuleEntry> categoryEntries = new ArrayList<>();
      Animation hudAnimation = this.categoryAnimations.get("HUD");
      if (hudAnimation != null && (this.hasAnyCategoryEnabled("HUD") || hudAnimation.getOutput() > 0.01)) {
         this.categoryCurrentYPositions.putIfAbsent("HUD", 0.0F);
         float currentY = this.categoryCurrentYPositions.getOrDefault("HUD", 0.0F);
         float targetY = this.categoryTargetYPositions.getOrDefault("HUD", 0.0F);
         categoryEntries.add(new ModulesList.ModuleEntry("HUD", hudAnimation, targetY, currentY));
      }

      Animation worldAnimation = this.categoryAnimations.get("World");
      if (worldAnimation != null && (this.hasAnyCategoryEnabled("WORLD") || worldAnimation.getOutput() > 0.01)) {
         this.categoryCurrentYPositions.putIfAbsent("World", 0.0F);
         float currentY = this.categoryCurrentYPositions.getOrDefault("World", 0.0F);
         float targetY = this.categoryTargetYPositions.getOrDefault("World", 0.0F);
         categoryEntries.add(new ModulesList.ModuleEntry("World", worldAnimation, targetY, currentY));
      }

      moduleEntries.addAll(categoryEntries);
      boolean entriesChanged = this.hasEntryMismatch(moduleEntries);
      if (entriesChanged) {
         this.needsResort = true;
      }

      boolean shouldResort = sortModeChanged || entriesChanged || this.needsResort && enoughTimePassed;
      if (shouldResort) {
         this.sortedModules = moduleEntries.stream()
            .sorted(modulesListModule.getSortMode().isSelected("Length") ? Comparator.nullsLast(Comparator.comparing(entry -> {
               if (entry == null) {
                  return 0.0F;
               } else if (entry.module != null) {
                  return this.getCachedModuleNameWidth(entry.module, font, modulesListModule);
               } else {
                  return entry.categoryLabel != null ? font.getStringWidth(entry.categoryLabel) : 0.0F;
               }
            }, Comparator.reverseOrder())) : Comparator.nullsLast(Comparator.comparing(entry -> {
               if (entry == null) {
                  return 0;
               } else if (entry.module != null) {
                  return this.getCachedSettingsCount(entry.module);
               } else {
                  return entry.categoryLabel != null ? 0 : 0;
               }
            }, Comparator.reverseOrder())))
            .collect(Collectors.toCollection(ArrayList::new));
         this.needsResort = false;
         this.lastSortUpdate = currentTime;
      } else {
         for (ModulesList.ModuleEntry entry : this.sortedModules) {
            if (entry.module != null) {
               Animation animation = entry.module.getAnimation();
               if (animation != null) {
                  entry.animation = animation;
               }
            }
         }
      }

      float lineHeight = 12.0F;
      float lineGap = 0.0F;
      AtomicReference<Float> targetY = new AtomicReference<>(0.0F);
      this.sortedModules.forEach(entry -> {
         if (entry.module != null) {
            this.targetYPositions.put(entry.module, targetY.get());
         } else if (entry.categoryLabel != null) {
            this.categoryTargetYPositions.put(entry.categoryLabel, targetY.get());
         }

         entry.targetY = targetY.get();
         targetY.updateAndGet(y -> y + lineHeight + lineGap);
      });
      long now = System.currentTimeMillis();
      float deltaMs = Math.max(1.0F, Math.min(100.0F, (float)(now - this.lastPositionUpdateTime)));
      this.lastPositionUpdateTime = now;
      float frameRatio = deltaMs / 33.333332F;
      float smoothness = 1.0F - (float)Math.pow(0.7F, frameRatio);
      this.sortedModules.forEach(entry -> {
         if (entry.module != null) {
            float current = this.currentYPositions.getOrDefault(entry.module, entry.targetY);
            float target = entry.targetY;
            float interpolated = current + (target - current) * smoothness;
            this.currentYPositions.put(entry.module, interpolated);
            entry.currentY = interpolated;
         } else if (entry.categoryLabel != null) {
            float current = this.categoryCurrentYPositions.getOrDefault(entry.categoryLabel, entry.targetY);
            float target = entry.targetY;
            float interpolated = current + (target - current) * smoothness;
            this.categoryCurrentYPositions.put(entry.categoryLabel, interpolated);
            entry.currentY = interpolated;
         }
      });
   }

   private boolean hasEntryMismatch(List<ModulesList.ModuleEntry> nextEntries) {
      if (this.sortedModules.size() != nextEntries.size()) {
         return true;
      }

      Set<Object> nextKeys = new HashSet<>();

      for (ModulesList.ModuleEntry entry : nextEntries) {
         nextKeys.add(this.getEntryKey(entry));
      }

      for (ModulesList.ModuleEntry entry : this.sortedModules) {
         if (!nextKeys.contains(this.getEntryKey(entry))) {
            return true;
         }
      }

      return false;
   }

   private Object getEntryKey(ModulesList.ModuleEntry entry) {
      return entry.module != null ? entry.module : "category:" + entry.categoryLabel;
   }

   @Override
   public void drawDraggable(DrawContext e) {
      MatrixStack matrix = e.getMatrices();
      FontRenderer font = Fonts.getSize(15, Fonts.Type.INTER_BOLD);
      if (!this.positionInitialized) {
         if (this.getX() == 0 && this.getY() == 3) {
            this.setX(window.getScaledWidth() - 105);
         }

         this.positionInitialized = true;
      }

      padej.soup.implement.features.modules.hud.ModulesList modulesListModule = padej.soup.implement.features.modules.hud.ModulesList.getInstance();
      if (this.lastLowercaseState != modulesListModule.getLowercase().isValue()) {
         this.clearNameWidthCache();
         this.needsResort = true;
         this.lastLowercaseState = modulesListModule.getLowercase().isValue();
      }

      this.updateSortedModules();
      if (this.sortedModules.isEmpty()) {
         String placeholder = this.getPlaceholder();
         float padding = 5.0F;
         float lineHeight = 16.0F;
         float width = font.getStringWidth(placeholder) + padding * 2.0F;
         if (modulesListModule.getShowBackground().isValue()) {
            blur.render(
               ShapeProperties.create(matrix, this.getX(), this.getY(), width, lineHeight)
                  .round(3.0F)
                  .softness(1.0F)
                  .thickness(2.0F)
                  .outlineColor(ColorUtil.getOutline())
                  .color(ColorUtil.getRect(0.7F))
                  .build()
            );
         }

         font.drawString(matrix, placeholder, this.getX() + padding, this.getY() + 5.5F, ColorUtil.getText());
         this.setWidth((int)width);
         this.setHeight((int)lineHeight);
      } else {
         float padding = 4.0F;
         float lineHeight = 12.0F;
         float lineGap = 0.0F;
         float sideLineWidth = 2.5F;
         boolean isRightSide = this.getX() > window.getScaledWidth() / 2.0F;
         if (this.cachedMaxWidth == 0.0F) {
            Main.getInstance()
               .getModuleProvider()
               .getModules()
               .stream()
               .filter(module -> !module.getCategory().name().equals("HUD"))
               .filter(module -> !module.getCategory().name().equals("WORLD"))
               .forEach(module -> {
                  float textWidthx = this.getCachedModuleNameWidth(module, font, modulesListModule);
                  this.cachedMaxWidth = Math.max(this.cachedMaxWidth, textWidthx);
               });
            this.cachedMaxWidth = Math.max(this.cachedMaxWidth, font.getStringWidth("HUD"));
            this.cachedMaxWidth = Math.max(this.cachedMaxWidth, font.getStringWidth("World"));
         }

         float totalWidth = this.cachedMaxWidth + padding * 2.0F + (modulesListModule.getSideLine().isValue() ? sideLineWidth : 0.0F);
         long visibleCount = this.sortedModules.stream().filter(entryx -> entryx.animation.getOutput() > 0.01F).count();
         float totalHeight = (float)visibleCount * (lineHeight + lineGap);
         int lineIndex = 0;

         for (ModulesList.ModuleEntry entry : this.sortedModules) {
            Animation animation = entry.animation;
            float animationProgress = animation.getOutput().floatValue();
            if (!(animationProgress < 0.01F)) {
               String displayName;
               if (entry.module != null) {
                  displayName = this.getModuleName(entry.module, modulesListModule);
               } else {
                  if (entry.categoryLabel == null) {
                     continue;
                  }

                  displayName = entry.categoryLabel;
               }

               float textWidth = font.getStringWidth(displayName);
               float lineWidth = textWidth + padding * 2.0F + (modulesListModule.getSideLine().isValue() ? sideLineWidth : 0.0F);
               float backgroundWidth = textWidth + padding * 2.0F;
               float animatedY = this.getY() + entry.currentY;
               float textX;
               float lineX;
               float backgroundX;
               if (isRightSide) {
                  float gap = -0.4F;
                  backgroundX = this.getX() + totalWidth - lineWidth + (modulesListModule.getSideLine().isValue() ? sideLineWidth + gap : 0.0F);
                  textX = backgroundX + padding;
                  lineX = this.getX() + totalWidth - sideLineWidth;
               } else {
                  float gap = -2.1F;
                  backgroundX = this.getX() + (modulesListModule.getSideLine().isValue() ? sideLineWidth + gap : 0.0F);
                  textX = backgroundX + padding;
                  lineX = this.getX();
               }

               float finalBackgroundX = backgroundX;
               float finalTextX = textX;
               float finalLineX = lineX;
               float slideOffset = (1.0F - animationProgress) * 20.0F * (isRightSide ? 1 : -1);
               int currentLineIndex = lineIndex;
               MathUtil.setAlpha(
                  animationProgress,
                  () -> {
                     matrix.push();
                     matrix.translate(slideOffset, 0.0F, 0.0F);
                     if (modulesListModule.getShowBackground().isValue()) {
                        blur.render(
                           ShapeProperties.create(matrix, finalBackgroundX, animatedY, backgroundWidth, lineHeight)
                              .round(3.0F)
                              .softness(1.0F)
                              .thickness(2.0F)
                              .outlineColor(ColorUtil.getOutline())
                              .color(ColorUtil.getRect(0.7F))
                              .build()
                        );
                     }

                     int lineColor;
                     if (modulesListModule.getSideLine().isValue()) {
                        if (modulesListModule.getGradientSideLine().isValue()) {
                           lineColor = ColorUtil.fade(currentLineIndex, 20);
                        } else {
                           lineColor = ColorUtil.getClientColor();
                        }

                        rectangle.render(ShapeProperties.create(matrix, finalLineX, animatedY, sideLineWidth, lineHeight + 0.45F).color(lineColor).build());
                     } else {
                        lineColor = ColorUtil.getClientColor();
                     }

                     float textY = animatedY + 4.0F;
                     if (modulesListModule.getGradientText().isValue()) {
                        int textColor = modulesListModule.getGradientSideLine().isValue() ? lineColor : ColorUtil.fade(currentLineIndex, 70);
                        font.drawString(matrix, displayName, finalTextX, textY, textColor);
                     } else {
                        font.drawString(matrix, displayName, finalTextX, textY, ColorUtil.getText());
                     }

                     matrix.pop();
                  }
               );
               lineIndex++;
            }
         }

         this.setWidth((int)totalWidth);
         this.setHeight((int)totalHeight);
      }
   }

   @NotNull
   private String getPlaceholder() {
      List<Module> modules = Main.getInstance().getModuleProvider().getModules();
      long totalEnabledCount = modules.stream().filter(Module::isEnabled).count();
      long hudModulesCount = modules.stream().filter(Module::isEnabled).filter(module -> module.getCategory().name().equals("HUD")).count();
      return totalEnabledCount == 0L ? "No active modules" : (totalEnabledCount == hudModulesCount ? "Enable non-HUD modules" : "Modules List");
   }

   private String getModuleName(Module module, padej.soup.implement.features.modules.hud.ModulesList modulesListModule) {
      String name = module.getLocalizedName();
      if (modulesListModule.getLowercase().isValue()) {
         name = name.toLowerCase();
      }

      return name;
   }

   private int countAllSettings(Module module) {
      int count = 0;

      for (Setting setting : module.settings()) {
         count++;
         if (setting instanceof GroupSetting groupSetting) {
            count += this.countSettingsInGroup(groupSetting);
         }
      }

      return count;
   }

   private int countSettingsInGroup(GroupSetting group) {
      int count = 0;

      for (Setting setting : group.getSubSettings()) {
         count++;
         if (setting instanceof GroupSetting nestedGroup) {
            count += this.countSettingsInGroup(nestedGroup);
         }
      }

      return count;
   }

   private static class ModuleEntry {
      Module module;
      Animation animation;
      float targetY;
      float currentY;
      String categoryLabel;

      ModuleEntry(Module module, Animation animation, float targetY, float currentY) {
         this.module = module;
         this.animation = animation;
         this.targetY = targetY;
         this.currentY = currentY;
         this.categoryLabel = null;
      }

      ModuleEntry(String categoryLabel, Animation animation, float targetY, float currentY) {
         this.module = null;
         this.animation = animation;
         this.targetY = targetY;
         this.currentY = currentY;
         this.categoryLabel = categoryLabel;
      }
   }
}
