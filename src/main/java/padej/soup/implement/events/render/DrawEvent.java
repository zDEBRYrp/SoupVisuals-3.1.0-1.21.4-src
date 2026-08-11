package padej.soup.implement.events.render;

import net.minecraft.client.gui.DrawContext;
import padej.soup.api.event.events.Event;
import padej.soup.api.system.draw.DrawEngine;

public class DrawEvent implements Event {
   private DrawContext drawContext;
   private DrawEngine drawEngine;
   private float partialTicks;

   public DrawContext getDrawContext() {
      return this.drawContext;
   }

   public DrawEngine getDrawEngine() {
      return this.drawEngine;
   }

   public float getPartialTicks() {
      return this.partialTicks;
   }

   public DrawEvent(DrawContext drawContext, DrawEngine drawEngine, float partialTicks) {
      this.drawContext = drawContext;
      this.drawEngine = drawEngine;
      this.partialTicks = partialTicks;
   }
}
