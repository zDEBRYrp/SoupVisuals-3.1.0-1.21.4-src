package padej.soup.implement.events.packet;

import net.minecraft.network.packet.Packet;
import padej.soup.api.event.events.callables.EventCancellable;

public class PacketEvent extends EventCancellable {
   private Packet<?> packet;
   private PacketEvent.Type type;

   public boolean isSend() {
      return this.type.equals(PacketEvent.Type.SEND);
   }

   public Packet<?> getPacket() {
      return this.packet;
   }

   public PacketEvent.Type getType() {
      return this.type;
   }

   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   public void setType(PacketEvent.Type type) {
      this.type = type;
   }

   public PacketEvent(Packet<?> packet, PacketEvent.Type type) {
      this.packet = packet;
      this.type = type;
   }

   public enum Type {
      SEND,
      RECEIVE;
   }
}
