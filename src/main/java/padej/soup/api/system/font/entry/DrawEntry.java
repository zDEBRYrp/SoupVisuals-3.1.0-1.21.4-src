package padej.soup.api.system.font.entry;

import padej.soup.api.system.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
