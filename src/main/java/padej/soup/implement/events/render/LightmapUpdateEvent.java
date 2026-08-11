package padej.soup.implement.events.render;

import net.minecraft.client.render.LightmapTextureManager;
import padej.soup.api.event.events.Event;

public record LightmapUpdateEvent(LightmapTextureManager lightmapTextureManager) implements Event {
}
