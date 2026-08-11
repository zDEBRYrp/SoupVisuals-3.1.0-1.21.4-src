package padej.soup.api.feature.module;

import padej.soup.api.system.localization.LocalizationManager;

public enum ModuleCategory {
   VISUALS("category.visuals"),
   WORLD("category.world"),
   PARTICLES("category.particles"),
   CLIENT("category.client"),
   OTHER("category.other"),
   HUD("category.hud"),
   MCTIERS("category.mctiers"),
   PERSONAL_INFO("category.personal_info"),
   HOME("category.home"),
   SEARCH("category.search");

   private final String localizationKey;

   ModuleCategory(String localizationKey) {
      this.localizationKey = localizationKey;
   }

   public String getLocalizedName() {
      String localized = LocalizationManager.getInstance().get(this.localizationKey);
      return localized.equals(this.localizationKey) ? this.localizationKey : localized;
   }

   public String getReadableName() {
      return this.getLocalizedName();
   }

   public String getIdentifier() {
      return this.name().toLowerCase();
   }

   public String getLocalizationKey() {
      return this.localizationKey;
   }
}
