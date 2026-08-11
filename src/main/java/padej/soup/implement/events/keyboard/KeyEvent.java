package padej.soup.implement.events.keyboard;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil.Type;
import padej.soup.api.event.events.Event;
import padej.soup.base.QuickImports;

public record KeyEvent(Screen screen, Type type, int key, int action) implements Event, QuickImports {
   public boolean isKeyDown(int key) {
      return this.isKeyDown(key, mc.currentScreen == null);
   }

   public boolean isKeyDown(int key, boolean screen) {
      return this.key == key && this.action == 1 && screen;
   }

   public boolean isKeyReleased(int key) {
      return this.isKeyReleased(key, mc.currentScreen == null);
   }

   public boolean isKeyReleased(int key, boolean screen) {
      return this.key == key && this.action == 0 && screen;
   }
}
