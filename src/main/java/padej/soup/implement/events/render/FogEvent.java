package padej.soup.implement.events.render;

import padej.soup.api.event.events.callables.EventCancellable;

public class FogEvent extends EventCancellable {
   private float distance;
   private int color;

   public float getDistance() {
      return this.distance;
   }

   public int getColor() {
      return this.color;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }

   public void setColor(int color) {
      this.color = color;
   }
}
