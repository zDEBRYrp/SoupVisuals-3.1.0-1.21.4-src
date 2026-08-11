package padej.soup.api.event.events;

public abstract class EventStoppable implements Event {
   private boolean stopped;

   protected EventStoppable() {
   }

   public boolean isStopped() {
      return this.stopped;
   }
}
