package padej.soup.implement.features.draggables;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.system.font.FontRenderer;
import padej.soup.api.system.font.Fonts;
import padej.soup.api.system.shape.ShapeProperties;
import padej.soup.base.util.color.ColorUtil;

public class ScoreBoard extends AbstractDraggable {
   private static final Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR = Comparator.comparing(ScoreboardEntry::value)
      .reversed()
      .thenComparing(ScoreboardEntry::owner, String.CASE_INSENSITIVE_ORDER);
   private final List<ScoreboardEntry> scoreboardEntries = new ArrayList<>();
   private ScoreboardObjective objective;

   public ScoreBoard() {
      super("ScoreBoard", 10, 100, 100, 120, true);
   }

   @Override
   public boolean visible() {
      return !this.scoreboardEntries.isEmpty();
   }

   @Override
   public void tick() {
      if (mc.world == null) {
         this.objective = null;
         this.scoreboardEntries.clear();
      } else {
         this.objective = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (this.objective == null) {
            this.scoreboardEntries.clear();
         } else {
            this.scoreboardEntries.clear();
            this.scoreboardEntries.addAll(mc.world.getScoreboard().getScoreboardEntries(this.objective));
            this.scoreboardEntries.sort(SCOREBOARD_ENTRY_COMPARATOR);
         }
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      MatrixStack matrix = context.getMatrices();
      FontRenderer font = Fonts.getSize(16);
      MutableText text = Text.empty();
      Text mainText = this.objective != null ? this.objective.getDisplayName() : Text.empty();
      if (mc.world != null) {
         for (ScoreboardEntry entry : this.scoreboardEntries) {
            text.append(Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(entry.owner()), entry.name())).append("\n");
         }
      }

      padej.soup.implement.features.modules.hud.ScoreBoard scoreboardModule = padej.soup.implement.features.modules.hud.ScoreBoard.getInstance();
      float headerHeight = 14.0F;
      int padding = 3;
      boolean showHeader = scoreboardModule.getShowHeader().isValue();
      boolean darkenHeader = scoreboardModule.getDarkenHeader().isValue();
      int width = (int)Math.max(font.getStringWidth(text) + padding * 2 + 1.0F, 100.0F);
      float contentHeight = font.getStringHeight(text) / 2.16F + padding;
      float totalHeight = showHeader ? headerHeight + contentHeight : contentHeight;
      if (showHeader) {
         int headerColor = darkenHeader ? ColorUtil.getRectDarker(0.9F) : ColorUtil.getRect(0.7F);
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), headerHeight)
               .round(4.0F, 0.0F, 4.0F, 0.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(headerColor)
               .build()
         );
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY() + headerHeight, this.getWidth(), contentHeight)
               .quality(40.0F)
               .round(0.0F, 4.0F, 0.0F, 4.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
         font.drawText(matrix, mainText, (int)(this.getX() + (this.getWidth() - font.getStringWidth(mainText)) / 2.0F), this.getY() + padding + 1.5F);
      } else {
         blur.render(
            ShapeProperties.create(matrix, this.getX(), this.getY(), this.getWidth(), contentHeight)
               .quality(40.0F)
               .round(4.0F)
               .thickness(2.0F)
               .softness(1.0F)
               .outlineColor(ColorUtil.getOutline())
               .color(ColorUtil.getRect(0.7F))
               .build()
         );
      }

      int offsetText = showHeader ? (int)(headerHeight + padding) : padding;
      font.drawText(matrix, text, this.getX() + padding, this.getY() + offsetText);
      if (this.getX() > mc.getWindow().getScaledWidth() / 2) {
         this.setX(this.getX() + this.getWidth() - width);
      }

      this.setWidth(width);
      this.setHeight((int)totalHeight);
   }
}
