package padej.soup.implement.features.draggables.watermark;

import net.minecraft.client.util.math.MatrixStack;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.other.RoleCache;

public class RoleComponent implements WatermarkComponent, QuickImports {
   @Override
   public float render(MatrixStack matrix, float x, float y, float height, FontRenderer font) {
      String username = mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername();
      String role = RoleCache.getUserRole(username);
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      float yIconOffset = 4.0F;
      float xIconOffset = -1.0F;
      if (this.shouldShowIcons()) {
         String iconTexture = this.getRoleIconTexture(role);
         image.setTexture(iconTexture)
            .render(ShapeProperties.create(matrix, x + xIconOffset, y + yIconOffset, iconSize, iconSize).color(this.getIconColor()).build());
      }

      font.drawString(matrix, role, x + iconOffset, y + 6.5F, ColorUtil.getText());
      return iconOffset + font.getStringWidth(role);
   }

   private String getRoleIconTexture(String role) {
      return switch (role) {
         case "YOUTUBE" -> "textures/role/yt.png";
         case "DEVELOPER" -> "textures/role/dev.png";
         case "TESTER" -> "textures/role/tester.png";
         case "PASTER" -> "textures/role/rat.png";
         case "CROW" -> "textures/role/bird.png";
         case "USER" -> "textures/role/user.png";
         default -> "textures/role.png";
      };
   }

   @Override
   public float getWidth(FontRenderer font, float height) {
      String username = mc.player != null ? mc.player.getName().getString() : mc.getSession().getUsername();
      String role = RoleCache.getUserRole(username);
      float iconSize = 8.0F;
      float iconOffset = this.shouldShowIcons() ? iconSize + 2.0F : 0.0F;
      return iconOffset + font.getStringWidth(role);
   }

   @Override
   public String getName() {
      return "Role";
   }
}
