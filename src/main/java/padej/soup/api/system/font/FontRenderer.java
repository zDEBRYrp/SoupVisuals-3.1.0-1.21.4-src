package padej.soup.api.system.font;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import padej.soup.api.event.EventManager;
import padej.soup.api.system.font.entry.DrawEntry;
import padej.soup.api.system.font.glyph.Glyph;
import padej.soup.api.system.font.glyph.GlyphMap;
import padej.soup.base.QuickImports;
import padej.soup.base.util.color.ColorUtil;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.math.MathUtil;
import padej.soup.base.util.other.StringUtil;
import padej.soup.implement.events.render.TextFactoryEvent;

public class FontRenderer implements QuickImports {
   private static final int STRING_BUILDER_POOL_SIZE = 16;
   private static final Queue<StringBuilder> STRING_BUILDER_POOL = new ArrayDeque<>(16);
   public static double ANIMATION_TIME;
   private final Object2ObjectMap<Identifier, ObjectList<DrawEntry>> GLYPH_PAGE_CACHE = new Object2ObjectOpenHashMap();
   private final ObjectList<GlyphMap> maps = new ObjectArrayList();
   private final Map<Character, Glyph> glyphCache = new HashMap<>();
   private static final Map<FontRenderer.ColorInterpKey, Integer> COLOR_INTERP_CACHE;
   private Font font;

   public FontRenderer(Font font, float sizePx) {
      this.init(font, sizePx);
   }

   private void init(Font font, float sizePx) {
      this.font = font.deriveFont(sizePx * 2.0F);
   }

   private GlyphMap generateMap(char from, char to) {
      GlyphMap glyphMap = new GlyphMap(from, to, this.font, randomIdentifier(), 5);
      this.maps.add(glyphMap);
      return glyphMap;
   }

   private Glyph locateGlyph(char glyph) {
      Glyph cached = this.glyphCache.get(glyph);
      if (cached != null) {
         return cached;
      }

      ObjectListIterator base = this.maps.iterator();

      while (base.hasNext()) {
         GlyphMap map = (GlyphMap)base.next();
         if (map.contains(glyph)) {
            Glyph found = map.getGlyph(glyph);
            this.glyphCache.put(glyph, found);
            return found;
         }
      }

      char basex = (char)MathUtil.floorNearestMulN(glyph, 128);
      Glyph result = this.generateMap(basex, (char)(basex + 128)).getGlyph(glyph);
      this.glyphCache.put(glyph, result);
      return result;
   }

   private static StringBuilder acquireStringBuilder() {
      StringBuilder sb = STRING_BUILDER_POOL.poll();
      if (sb == null) {
         sb = new StringBuilder(256);
      } else {
         sb.setLength(0);
      }

      return sb;
   }

   private static void releaseStringBuilder(StringBuilder sb) {
      if (STRING_BUILDER_POOL.size() < 16) {
         sb.setLength(0);
         STRING_BUILDER_POOL.offer(sb);
      }
   }

   public void drawText(MatrixStack matrix, Text text, double x, double y) {
      StringBuilder sb = acquireStringBuilder();

      try {
         this.findStyle(sb, text);
         this.drawString(matrix, sb.toString(), x, y, ColorUtil.getText());
      } finally {
         releaseStringBuilder(sb);
      }
   }

   public void findStyle(StringBuilder sb, Text component) {
      Style style = component.getStyle();
      if (component.getSiblings().isEmpty()) {
         if (style.getColor() != null) {
            sb.append(ColorUtil.formatting(style.getColor().getRgb()));
         }

         sb.append(component.getString()).append(Formatting.RESET);
      } else {
         component.getWithStyle(style).forEach(text -> this.findStyle(sb, text));
      }
   }

   public void drawStringWithScroll(MatrixStack matrix, String text, double x, double y, float width, int color) {
      String separation = "  |  ";
      float textWidth = this.getStringWidth(text + separation);
      if (textWidth - width < 10.0F) {
         this.drawString(matrix, text, x, y, color);
      } else {
         this.drawString(matrix, text + separation + text, x - MathUtil.textScrolling(textWidth), y, color);
      }
   }

   public void drawItalicString(MatrixStack matrix, String text, double x, double y, int color) {
      matrix.push();
      float shear = -0.15F;
      Matrix4f mat = matrix.peek().getPositionMatrix();
      mat.m10(mat.m10() + shear);
      mat.m30(mat.m30() - shear * (float)y);
      this.drawString(matrix, text, x, y, color);
      matrix.pop();
   }

   public void drawString(MatrixStack matrix, String text, double x, double y, int color) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      char[] chars = text.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      int lineStart = 0;
      StringBuilder stringColor = new StringBuilder();
      boolean colorFormat = false;
      boolean textColor = false;
      int clr = color;

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (c == 167) {
            colorFormat = true;
         } else if (colorFormat) {
            colorFormat = false;
            char c1 = Character.toUpperCase(c);
            if (ColorUtil.colorCodes.containsKey(c1)) {
               clr = new Color(ColorUtil.colorCodes.get(c1)).getRGB();
            } else if (c1 == 'R') {
               clr = color;
            }
         } else if (c == 9167) {
            if (textColor) {
               try {
                  String colorString = stringColor.toString();
                  if (colorString.matches("\\d+")) {
                     clr = new Color(Integer.parseInt(colorString)).getRGB();
                  }
               } catch (IllegalArgumentException e) {
                  LoggerUtil.error("Failed to draw string: " + e.getMessage());
               }

               stringColor.setLength(0);
            }

            textColor = !textColor;
         } else if (textColor) {
            stringColor.append(c);
         } else if (c == '\n') {
            yOffset += this.getStringHeight(text.substring(lineStart, i)) - 2.0F;
            xOffset = 0.0F;
            lineStart = i + 1;
         } else {
            Glyph glyph = this.locateGlyph(c);
            if (glyph != null) {
               if (glyph.value() != ' ') {
                  Identifier i1 = glyph.owner().bindToTexture;
                  DrawEntry entry = new DrawEntry(xOffset, yOffset, clr, glyph);
                  ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList())).add(entry);
               }

               xOffset += glyph.width();
            }
         }
      }

      if (!this.GLYPH_PAGE_CACHE.isEmpty()) {
         this.drawGlyphs(matrix, x, y);
      }

      this.clearGlyphCache();
   }

   public void drawGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      char[] chars = text.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      int lineStart = 0;
      int textLength = text.length();

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (c == '\n') {
            yOffset += this.getStringHeight(text.substring(lineStart, i)) - 2.0F;
            xOffset = 0.0F;
            lineStart = i + 1;
         } else {
            Glyph glyph = this.locateGlyph(c);
            if (glyph != null) {
               if (glyph.value() != ' ') {
                  float t = (float)i / (textLength - 1);
                  int color = this.interpolateColor(colorStart, colorEnd, t);
                  Identifier i1 = glyph.owner().bindToTexture;
                  DrawEntry entry = new DrawEntry(xOffset, yOffset, color, glyph);
                  ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList())).add(entry);
               }

               xOffset += glyph.width();
            }
         }
      }

      if (!this.GLYPH_PAGE_CACHE.isEmpty()) {
         this.drawGlyphs(matrix, x, y);
      }

      this.clearGlyphCache();
   }

   @Deprecated
   public void drawAnimatedGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd, double waveLength, double speed) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd, waveLength, speed, 0.3F);
   }

   @Deprecated
   public void drawAnimatedGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd);
   }

   public void drawWaveGradientString(
      MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd, double waveLength, double speed, float minBrightness
   ) {
      double safeWaveLength = waveLength <= 0.0 ? Math.PI * 2 : waveLength;
      double durationSec = speed <= 0.0 ? 2.4 : 7.2 / speed;
      int symbolCount = this.countWaveSymbols(text);
      double totalCycleSpread = safeWaveLength / (Math.PI * 2);
      double delayPerCharSec = symbolCount <= 1 ? 0.0 : durationSec * totalCycleSpread / (symbolCount - 1);
      this.drawWaveGradientString(matrix, text, x, y, new int[]{colorStart, colorEnd, colorStart}, durationSec, delayPerCharSec, minBrightness);
   }

   public void drawWaveGradientString(
      MatrixStack matrix, String text, double x, double y, int[] keyframeColors, double durationSec, double delayPerCharSec, float minBrightness
   ) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      ANIMATION_TIME = System.currentTimeMillis() / 1000.0;
      int[] palette = this.sanitizeKeyframeColors(keyframeColors);
      int[] normalizedPalette = this.normalizePaletteBrightness(palette, this.clamp01(minBrightness));
      double safeDuration = Math.max(0.05, durationSec);
      double safeDelay = Math.max(0.0, delayPerCharSec);
      char[] chars = text.toCharArray();
      float xOffset = 0.0F;
      float yOffset = 0.0F;
      int lineStart = 0;
      int waveIndex = 0;

      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];
         if (c == '\n') {
            yOffset += this.getStringHeight(text.substring(lineStart, i)) - 2.0F;
            xOffset = 0.0F;
            lineStart = i + 1;
            waveIndex = 0;
         } else {
            Glyph glyph = this.locateGlyph(c);
            if (glyph != null) {
               if (glyph.value() != ' ') {
                  double progress = this.computeWaveProgress(ANIMATION_TIME, safeDuration, safeDelay, waveIndex);
                  int color = this.sampleKeyframeColor(normalizedPalette, progress);
                  Identifier i1 = glyph.owner().bindToTexture;
                  DrawEntry entry = new DrawEntry(xOffset, yOffset, color, glyph);
                  ((ObjectList)this.GLYPH_PAGE_CACHE.computeIfAbsent(i1, integer -> new ObjectArrayList())).add(entry);
               }

               xOffset += glyph.width();
            }

            waveIndex++;
         }
      }

      if (!this.GLYPH_PAGE_CACHE.isEmpty()) {
         this.drawGlyphs(matrix, x, y);
      }

      this.clearGlyphCache();
   }

   public void drawWaveGradientString(MatrixStack matrix, String text, double x, double y, int[] keyframeColors, double durationSec, double delayPerCharSec) {
      this.drawWaveGradientString(matrix, text, x, y, keyframeColors, durationSec, delayPerCharSec, 0.3F);
   }

   public void drawWaveGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd, double waveLength, double speed) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd, waveLength, speed, 0.3F);
   }

   public void drawWaveGradientString(MatrixStack matrix, String text, double x, double y, int colorStart, int colorEnd) {
      this.drawWaveGradientString(matrix, text, x, y, colorStart, colorEnd, Math.PI * 2, 3.0, 0.3F);
   }

   private void clearGlyphCache() {
      ObjectIterator var1 = this.GLYPH_PAGE_CACHE.values().iterator();

      while (var1.hasNext()) {
         ObjectList<DrawEntry> list = (ObjectList<DrawEntry>)var1.next();
         list.clear();
      }

      this.GLYPH_PAGE_CACHE.clear();
   }

   private void drawGlyphs(MatrixStack matrix, double x, double y) {
      matrix.push();
      matrix.translate(x, y - 3.0, 0.0);
      matrix.scale(0.5F, 0.5F, 1.0F);
      Matrix4f matrix4f = matrix.peek().getPositionMatrix();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      ObjectIterator var7 = this.GLYPH_PAGE_CACHE.keySet().iterator();

      while (var7.hasNext()) {
         Identifier identifier = (Identifier)var7.next();
         RenderSystem.setShaderTexture(0, identifier);
         BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         ObjectListIterator var10 = ((ObjectList)this.GLYPH_PAGE_CACHE.get(identifier)).iterator();

         while (var10.hasNext()) {
            DrawEntry drawEntry = (DrawEntry)var10.next();
            float x1 = drawEntry.atX();
            float y1 = drawEntry.atY();
            Glyph glyph = drawEntry.toDraw();
            GlyphMap glyphMap = glyph.owner();
            float width = glyph.width();
            float height = glyph.height();
            float u1 = (float)glyph.u() / glyphMap.width;
            float v1 = (float)glyph.v() / glyphMap.height;
            float u2 = (float)(glyph.u() + glyph.width()) / glyphMap.width;
            float v2 = (float)(glyph.v() + glyph.height()) / glyphMap.height;
            int color = drawEntry.color();
            buffer.vertex(matrix4f, x1 + 0.0F, y1 + height, 0.0F).texture(u1, v2).color(color);
            buffer.vertex(matrix4f, x1 + width, y1 + height, 0.0F).texture(u2, v2).color(color);
            buffer.vertex(matrix4f, x1 + width, y1 + 0.0F, 0.0F).texture(u2, v1).color(color);
            buffer.vertex(matrix4f, x1 + 0.0F, y1 + 0.0F, 0.0F).texture(u1, v1).color(color);
         }

         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }

      RenderSystem.disableBlend();
      matrix.pop();
   }

   public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
      this.drawString(stack, s, (int)(x - this.getStringWidth(s) / 2.0F), (float)y, color);
   }

   public float getStringWidth(Text text) {
      return text != null ? this.getStringWidth(text.getString()) : 0.0F;
   }

   public float getStringWidth(String text) {
      TextFactoryEvent event = new TextFactoryEvent(text);
      EventManager.callEvent(event);
      text = event.getText();
      float currentLine = 0.0F;
      float maxPreviousLines = 0.0F;
      boolean ignore = false;

      for (char c : text.toCharArray()) {
         if (ignore) {
            ignore = false;
         } else if (c == 167) {
            ignore = true;
         } else if (c == '\n') {
            maxPreviousLines = Math.max(currentLine, maxPreviousLines);
            currentLine = 0.0F;
         } else {
            Glyph glyph = this.locateGlyph(c);
            currentLine += glyph == null ? 0.0F : glyph.width();
         }
      }

      return Math.max(currentLine, maxPreviousLines) / 2.0F;
   }

   public float getStringHeight(Text text) {
      return this.getStringHeight(text.getString());
   }

   public float getStringHeight(String text) {
      float currentLine = 0.0F;
      float previous = 0.0F;

      for (char c : (text.isEmpty() ? " " : text).toCharArray()) {
         if (c == '\n') {
            currentLine = currentLine == 0.0F ? this.locateGlyph(' ').height() : currentLine;
            previous += currentLine;
            currentLine = 0.0F;
         } else {
            Glyph glyph = this.locateGlyph(c);
            currentLine = Math.max(glyph == null ? 0.0F : glyph.height(), currentLine);
         }
      }

      return currentLine + previous;
   }

   private int interpolateColor(int colorStart, int colorEnd, float t) {
      int quantizedT = (int)(Math.max(0.0F, Math.min(1.0F, t)) * 512.0F);
      FontRenderer.ColorInterpKey key = new FontRenderer.ColorInterpKey(colorStart, colorEnd, quantizedT);
      Integer cached = COLOR_INTERP_CACHE.get(key);
      if (cached != null) {
         return cached;
      }

      float startAlpha = (colorStart >> 24 & 0xFF) / 255.0F;
      float startRed = (colorStart >> 16 & 0xFF) / 255.0F;
      float startGreen = (colorStart >> 8 & 0xFF) / 255.0F;
      float startBlue = (colorStart & 0xFF) / 255.0F;
      float endAlpha = (colorEnd >> 24 & 0xFF) / 255.0F;
      float endRed = (colorEnd >> 16 & 0xFF) / 255.0F;
      float endGreen = (colorEnd >> 8 & 0xFF) / 255.0F;
      float endBlue = (colorEnd & 0xFF) / 255.0F;
      float alpha = startAlpha + t * (endAlpha - startAlpha);
      float red = startRed + t * (endRed - startRed);
      float green = startGreen + t * (endGreen - startGreen);
      float blue = startBlue + t * (endBlue - startBlue);
      int result = (int)(alpha * 255.0F) << 24 | (int)(red * 255.0F) << 16 | (int)(green * 255.0F) << 8 | (int)(blue * 255.0F);
      if (COLOR_INTERP_CACHE.size() < 10000) {
         COLOR_INTERP_CACHE.put(key, result);
      }

      return result;
   }

   private int normalizeBrightness(int color, float targetBrightness) {
      int alpha = color >> 24 & 0xFF;
      int red = color >> 16 & 0xFF;
      int green = color >> 8 & 0xFF;
      int blue = color & 0xFF;
      float currentBrightness = (0.299F * red + 0.587F * green + 0.114F * blue) / 255.0F;
      if (currentBrightness < targetBrightness) {
         float scale = targetBrightness * 1.01F / Math.max(currentBrightness, 0.01F);
         red = Math.min(255, (int)Math.ceil(red * scale));
         green = Math.min(255, (int)Math.ceil(green * scale));
         blue = Math.min(255, (int)Math.ceil(blue * scale));
      }

      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private int interpolateColorWithBrightness(int colorStart, int colorEnd, float t, float minBrightness) {
      int normalizedStart = this.normalizeBrightness(colorStart, minBrightness);
      int normalizedEnd = this.normalizeBrightness(colorEnd, minBrightness);
      return this.interpolateColor(normalizedStart, normalizedEnd, t);
   }

   private int[] sanitizeKeyframeColors(int[] keyframeColors) {
      return keyframeColors != null && keyframeColors.length != 0 ? keyframeColors : new int[]{-1};
   }

   private int[] normalizePaletteBrightness(int[] keyframeColors, float minBrightness) {
      int[] result = new int[keyframeColors.length];

      for (int i = 0; i < keyframeColors.length; i++) {
         result[i] = this.normalizeBrightness(keyframeColors[i], minBrightness);
      }

      return result;
   }

   private double computeWaveProgress(double currentTimeSec, double durationSec, double delayPerCharSec, int charIndex) {
      double shiftedTime = currentTimeSec - charIndex * delayPerCharSec;
      double cyclePos = shiftedTime / durationSec;
      double progress = cyclePos - Math.floor(cyclePos);
      return progress < 0.0 ? progress + 1.0 : progress;
   }

   private int sampleKeyframeColor(int[] keyframeColors, double progress) {
      if (keyframeColors.length == 1) {
         return keyframeColors[0];
      }

      double clamped = Math.max(0.0, Math.min(1.0, progress));
      int segmentCount = keyframeColors.length - 1;
      double scaled = clamped * segmentCount;
      int segment = (int)Math.floor(scaled);
      if (segment >= segmentCount) {
         return keyframeColors[segmentCount];
      }

      float localT = (float)(scaled - segment);
      float easedT = this.easeInOut(localT);
      return this.interpolateColor(keyframeColors[segment], keyframeColors[segment + 1], easedT);
   }

   private float easeInOut(float t) {
      float clamped = Math.max(0.0F, Math.min(1.0F, t));
      return (float)(0.5 - 0.5 * Math.cos(Math.PI * clamped));
   }

   private float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private int countWaveSymbols(String text) {
      if (text != null && !text.isEmpty()) {
         int count = 0;

         for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\n') {
               count++;
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   @Contract(value = "-> new", pure = true)
   @NotNull
   public static Identifier randomIdentifier() {
      return Identifier.of("soupapi", "temp/" + StringUtil.randomString(32));
   }

   public FontRenderer setFont(Font font) {
      this.font = font;
      return this;
   }

   public Font getFont() {
      return this.font;
   }

   static {
      for (int i = 0; i < 16; i++) {
         STRING_BUILDER_POOL.offer(new StringBuilder(256));
      }

      ANIMATION_TIME = 0.0;
      COLOR_INTERP_CACHE = new HashMap<>();
   }

   private record ColorInterpKey(int colorStart, int colorEnd, int quantizedT) {
   }
}
