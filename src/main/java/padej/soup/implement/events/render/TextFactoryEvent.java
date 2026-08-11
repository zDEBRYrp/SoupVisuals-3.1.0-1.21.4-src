package padej.soup.implement.events.render;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import padej.soup.api.event.events.Event;

public class TextFactoryEvent implements Event {
   private String text;

   public void replaceText(String protect, String replaced) {
      if (this.text != null && !this.text.isEmpty() && protect != null && replaced != null) {
         if (this.text.equalsIgnoreCase(protect)) {
            this.text = replaced;
         } else {
            String pattern = "(?i)\\b" + Pattern.quote(protect) + "\\b";
            this.text = this.text.replaceAll(pattern, Matcher.quoteReplacement(replaced));
            this.text = this.text.replace("⏏" + protect, "⏏" + replaced);
            this.text = this.text.replace(protect + "§", replaced + "§");
         }
      }
   }

   public void setText(String text) {
      this.text = text;
   }

   public String getText() {
      return this.text;
   }

   public TextFactoryEvent(String text) {
      this.text = text;
   }
}
