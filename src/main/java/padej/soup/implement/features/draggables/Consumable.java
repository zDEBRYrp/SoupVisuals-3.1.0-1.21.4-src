package padej.soup.implement.features.draggables;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.entity.PlayerIntersectionUtil;

public class Consumable extends AbstractDraggable {
   private static final Map<String, Item> ITEM_MAP = new LinkedHashMap<>();
   private final Map<String, Integer> itemCounts = new LinkedHashMap<>();

   public Consumable() {
      super("Consumable", 400, 200, 140, 80, true);
   }

   @Override
   public boolean visible() {
      return mc.player != null || PlayerIntersectionUtil.isChat(mc.currentScreen);
   }

   @Override
   public void tick() {
      if (mc.player != null) {
         this.itemCounts.clear();
         padej.soup.implement.features.modules.hud.Consumable consumableModule = padej.soup.implement.features.modules.hud.Consumable.getInstance();

         for (String itemType : consumableModule.getItemTypes().getSelected()) {
            Item item = ITEM_MAP.get(itemType);
            if (item != null) {
               int count = this.countItem(item);
               if (count > 0) {
                  this.itemCounts.put(itemType, count);
               }
            }
         }
      }
   }

   private int countItem(Item item) {
      return mc.player == null
         ? 0
         : IntStream.range(0, mc.player.getInventory().size())
            .mapToObj(i -> mc.player.getInventory().getStack(i))
            .filter(stack -> !stack.isEmpty() && stack.getItem() == item)
            .mapToInt(ItemStack::getCount)
            .sum();
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      padej.soup.implement.features.modules.hud.Consumable consumableModule = padej.soup.implement.features.modules.hud.Consumable.getInstance();
      boolean showBackground = consumableModule.getShowBackground().isValue();
      boolean showCount = consumableModule.getShowCount().isValue();
      String layout = consumableModule.getLayout().getSelected();
      float scale = consumableModule.getScale().getValue();
      float padding = 3.0F;
      float baseItemSize = 16.0F;
      float itemSize = baseItemSize * scale + 2.0F;
      int visibleItems = this.itemCounts.size();
      if (visibleItems != 0) {
         int cols = 1;
         int rows = 1;
         float width = 0.0F;
         float height = 0.0F;
         switch (layout) {
            case "Line":
               cols = visibleItems;
               width = cols * itemSize + padding * 2.0F;
               height = itemSize + padding * 2.0F;
               break;
            case "Column":
               cols = 1;
               rows = visibleItems;
               width = itemSize + padding * 2.0F;
               height = rows * itemSize + padding * 2.0F;
               break;
            case "Table":
               cols = (int)Math.ceil(Math.sqrt(visibleItems));
               rows = (int)Math.ceil((double)visibleItems / cols);
               width = cols * itemSize + padding * 2.0F;
               height = rows * itemSize + padding * 2.0F;
         }

         this.setWidth((int)width);
         this.setHeight((int)height);
         if (showBackground) {
            blur.render(
               ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), this.getHeight())
                  .quality(25.0F)
                  .round(4.0F)
                  .softness(1.0F)
                  .thickness(2.0F)
                  .outlineColor(ColorUtil.getOutline())
                  .color(ColorUtil.getRect(0.7F))
                  .build()
            );
         }

         float startY = this.getY() + padding;
         float startX = this.getX() + padding;
         int index = 0;

         for (Entry<String, Integer> entry : this.itemCounts.entrySet()) {
            String itemType = entry.getKey();
            int count = entry.getValue();
            int row = 0;
            int col = 0;
            switch (layout) {
               case "Line":
                  col = index;
                  row = 0;
                  break;
               case "Column":
                  col = 0;
                  row = index;
                  break;
               case "Table":
                  row = index / cols;
                  col = index % cols;
            }

            float x = startX + col * itemSize;
            float y = startY + row * itemSize;
            Item item = ITEM_MAP.get(itemType);
            if (item != null) {
               ItemStack stack = new ItemStack(item, count);
               matrix.push();
               matrix.translate(x, y, 0.0F);
               matrix.scale(scale, scale, 1.0F);
               context.drawItem(stack, 0, 0);
               if (showCount) {
                  context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
               }

               matrix.pop();
            }

            index++;
         }
      }
   }

   static {
      ITEM_MAP.put("Snowball", Items.SNOWBALL);
      ITEM_MAP.put("Egg", Items.EGG);
      ITEM_MAP.put("Wind Charge", Items.WIND_CHARGE);
      ITEM_MAP.put("Golden Apple", Items.GOLDEN_APPLE);
      ITEM_MAP.put("Enchanted Golden Apple", Items.ENCHANTED_GOLDEN_APPLE);
      ITEM_MAP.put("Arrow", Items.ARROW);
      ITEM_MAP.put("Spectral Arrow", Items.SPECTRAL_ARROW);
      ITEM_MAP.put("Tipped Arrow", Items.TIPPED_ARROW);
      ITEM_MAP.put("Totem", Items.TOTEM_OF_UNDYING);
      ITEM_MAP.put("Chorus Fruit", Items.CHORUS_FRUIT);
      ITEM_MAP.put("Ender Pearl", Items.ENDER_PEARL);
      ITEM_MAP.put("Firework Rocket", Items.FIREWORK_ROCKET);
      ITEM_MAP.put("Experience Bottle", Items.EXPERIENCE_BOTTLE);
   }
}
