package padej.soup.implement.menu.components.implement.other;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.system.animation.Animation;
import padej.soup.api.system.animation.Direction;
import padej.soup.api.system.animation.implement.DecelerateAnimation;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.implement.menu.MenuScreen;
import padej.soup.implement.menu.components.AbstractComponent;
import padej.soup.implement.menu.components.implement.category.CategoryComponent;

public class CategoryContainerComponent extends AbstractComponent {
   private static final List<ModuleCategory> EXCLUDED_CATEGORIES = List.of(ModuleCategory.PERSONAL_INFO, ModuleCategory.SEARCH, ModuleCategory.HOME);
   private final List<CategoryComponent> categoryComponents = new ArrayList<>();
   private float selectionX = 0.0F;
   private float selectionY = 0.0F;
   private ModuleCategory previousCategory = null;
   private final Animation selectionColorAnimation = new DecelerateAnimation().setMs(300).setValue(1.0);

   public void initializeCategoryComponents() {
      this.categoryComponents.clear();

      for (ModuleCategory category : ModuleCategory.values()) {
         if (!EXCLUDED_CATEGORIES.contains(category)) {
            this.categoryComponents.add(new CategoryComponent(category));
         }
      }

      this.categoryComponents.add(new CategoryComponent(ModuleCategory.HOME));
      this.categoryComponents.add(new CategoryComponent(ModuleCategory.PERSONAL_INFO));
      this.categoryComponents.add(new CategoryComponent(ModuleCategory.SEARCH));
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      ModuleCategory currentCategory = MenuScreen.INSTANCE.getCategory();
      float offset = 0.0F;
      float targetX = 0.0F;
      float targetY = 0.0F;
      boolean foundTarget = false;

      for (CategoryComponent component : this.categoryComponents) {
         if (!EXCLUDED_CATEGORIES.contains(component.getCategory())) {
            component.x = this.x + 11.0F;
            component.y = this.y + 49.0F + offset;
            component.width = 11.0F;
            component.height = 11.0F;
            if (component.getCategory() == currentCategory) {
               targetX = component.x;
               targetY = component.y;
               foundTarget = true;
            }

            offset += component.height + 14.0F;
         }
      }

      if (this.previousCategory != currentCategory && !EXCLUDED_CATEGORIES.contains(currentCategory)) {
         if (this.previousCategory != null && !EXCLUDED_CATEGORIES.contains(this.previousCategory)) {
            this.selectionColorAnimation.setDirection(Direction.BACKWARDS);
            this.selectionColorAnimation.reset();
            this.selectionColorAnimation.setDirection(Direction.FORWARDS);
         } else {
            this.selectionX = targetX;
            this.selectionY = targetY;
            this.selectionColorAnimation.setDirection(Direction.FORWARDS);
         }

         this.previousCategory = currentCategory;
      } else if (this.previousCategory != currentCategory) {
         this.previousCategory = currentCategory;
      }

      if (foundTarget && !EXCLUDED_CATEGORIES.contains(currentCategory)) {
         if (MenuScreen.INSTANCE.isMenuDragging()) {
            this.selectionX = targetX;
            this.selectionY = targetY;
         } else {
            this.selectionX = MathUtil.interpolateSmooth(4.0, this.selectionX, targetX);
            this.selectionY = MathUtil.interpolateSmooth(4.0, this.selectionY, targetY);
         }

         float squareSize = 17.0F;
         float iconSize = 11.0F;
         float squareX = this.selectionX + (iconSize - squareSize) / 2.0F;
         float squareY = this.selectionY + (iconSize - squareSize) / 2.0F;
         float colorProgress = this.selectionColorAnimation.getOutput().floatValue();
         int defaultOutlineColor = ColorUtil.getOutline();
         int clientOutlineColor = ColorUtil.getClientColor();
         int outlineColor = ColorUtil.overCol(defaultOutlineColor, clientOutlineColor, colorProgress);
         rectangle.render(
            ShapeProperties.create(context.getMatrices(), squareX, squareY, squareSize, squareSize)
               .round(3.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(outlineColor)
               .color(ColorUtil.getGuiRectColor(0.5F))
               .build()
         );
      }

      for (CategoryComponent component : this.categoryComponents) {
         component.render(context, mouseX, mouseY, delta);
      }
   }

   @Override
   public void tick() {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.tick();
      }

      super.tick();
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         if (categoryComponent.mouseClicked(mouseX, mouseY, button)) {
            return true;
         }
      }

      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.mouseReleased(mouseX, mouseY, button);
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   @Override
   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }

      return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.mouseScrolled(mouseX, mouseY, amount);
      }

      return super.mouseScrolled(mouseX, mouseY, amount);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.keyPressed(keyCode, scanCode, modifiers);
      }

      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char chr, int modifiers) {
      for (CategoryComponent categoryComponent : this.categoryComponents) {
         categoryComponent.charTyped(chr, modifiers);
      }

      return super.charTyped(chr, modifiers);
   }

   public CategoryContainerComponent setSelectionX(float selectionX) {
      this.selectionX = selectionX;
      return this;
   }

   public CategoryContainerComponent setSelectionY(float selectionY) {
      this.selectionY = selectionY;
      return this;
   }

   public CategoryContainerComponent setPreviousCategory(ModuleCategory previousCategory) {
      this.previousCategory = previousCategory;
      return this;
   }
}
