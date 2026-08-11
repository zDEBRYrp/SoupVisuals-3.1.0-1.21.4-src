package padej.soup.implement.features.modules.client;

import padej.soup.api.feature.module.Module;
import padej.soup.api.feature.module.ModuleCategory;
import padej.soup.api.feature.module.setting.implement.SelectSetting;
import padej.soup.api.file.impl.CurrentLanguageFile;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.Instance;
import padej.soup.core.Main;

public class Language extends Module {
   private final SelectSetting languageSetting = new SelectSetting("setting.language.language.name", "setting.language.language.desc")
      .value("Русский", "English")
      .selected("English");

   public static Language getInstance() {
      return Instance.get(Language.class);
   }

   public Language() {
      super("module.language.name", ModuleCategory.CLIENT, false);
      this.setup(this.languageSetting);
      this.loadLanguageFromFile();
      this.languageSetting.onChange(value -> {
         this.updateLanguage();
         this.saveLanguageToFile();
      });
      this.updateLanguage();
   }

   private void loadLanguageFromFile() {
      try {
         CurrentLanguageFile languageFile = this.getCurrentLanguageFile();
         if (languageFile != null && languageFile.hasCurrentLanguage()) {
            String langCode = languageFile.getCurrentLanguage();

            String displayName = switch (langCode) {
               case "ru_ru" -> "Русский";
               default -> "English";
            };
            this.languageSetting.setSelected(displayName);
         }
      } catch (Exception e) {
         LoggerUtil.error("Failed to load language from file: " + e.getMessage());
      }
   }

   private void saveLanguageToFile() {
      try {
         CurrentLanguageFile languageFile = this.getCurrentLanguageFile();
         if (languageFile != null) {
            String selected = this.languageSetting.getSelected();

            String langCode = switch (selected) {
               case "Русский" -> "ru_ru";
               default -> "en_us";
            };
            languageFile.setCurrentLanguage(langCode);
         }
      } catch (Exception e) {
         LoggerUtil.error("Failed to save language to file: " + e.getMessage());
      }
   }

   private CurrentLanguageFile getCurrentLanguageFile() {
      try {
         return Main.getInstance()
            .getFileRepository()
            .getClientFiles()
            .stream()
            .filter(file -> file instanceof CurrentLanguageFile)
            .map(file -> (CurrentLanguageFile)file)
            .findFirst()
            .orElse(null);
      } catch (Exception e) {
         return null;
      }
   }

   private void updateLanguage() {
      String selected = this.languageSetting.getSelected();

      String langCode = switch (selected) {
         case "Русский" -> "ru_ru";
         default -> "en_us";
      };
      LocalizationManager.getInstance().setLanguage(langCode);
   }

   public SelectSetting getLanguageSetting() {
      return this.languageSetting;
   }
}
