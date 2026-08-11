package padej.soup.api.system.localization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import padej.soup.base.util.logger.LoggerUtil;

public class LocalizationManager {
   private static LocalizationManager instance;
   private String currentLanguage = "en_us";
   private final Map<String, Map<String, String>> languages = new HashMap<>();
   private final Set<String> accessedKeys = new HashSet<>();
   private final Set<String> missingKeys = new HashSet<>();

   public static LocalizationManager getInstance() {
      if (instance == null) {
         instance = new LocalizationManager();
      }

      return instance;
   }

   private LocalizationManager() {
      this.loadLanguage("en_us");
      this.loadLanguage("ru_ru");
   }

   private void loadLanguage(String languageCode) {
      Map<String, String> translations = new HashMap<>();
      String path = "/lang/" + languageCode + ".ini";

      try (InputStream inputStream = this.getClass().getResourceAsStream(path)) {
         if (inputStream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
               this.parseIni(reader, translations);
               this.languages.put(languageCode, translations);
               LoggerUtil.info("Loaded language: " + languageCode + " (" + translations.size() + " entries)");
            }
         } else {
            LoggerUtil.info("Language file not found: " + path);
         }
      } catch (IOException e) {
         LoggerUtil.error("Failed to load language: " + languageCode);
         LoggerUtil.error("Failed to load language: " + e.getLocalizedMessage());
      }
   }

   private void parseIni(BufferedReader reader, Map<String, String> translations) throws IOException {
      String line;
      while ((line = reader.readLine()) != null) {
         line = line.trim();
         if (!line.isEmpty() && !line.startsWith("#") && !line.startsWith(";") && line.contains("=")) {
            int equalIndex = line.indexOf(61);
            String key = line.substring(0, equalIndex).trim();
            String value = line.substring(equalIndex + 1).trim();
            translations.put(key, value);
         }
      }
   }

   public void setLanguage(String languageCode) {
      String normalizedCode = languageCode.toLowerCase();
      if (this.languages.containsKey(normalizedCode)) {
         if (!this.currentLanguage.equals(normalizedCode)) {
            this.currentLanguage = normalizedCode;
            LoggerUtil.info("Language changed to: " + normalizedCode);
         } else {
            this.currentLanguage = normalizedCode;
         }
      } else {
         LoggerUtil.warn("Language not found: " + normalizedCode);
      }
   }

   public String get(String key) {
      this.accessedKeys.add(key);
      Map<String, String> currentLang = this.languages.get(this.currentLanguage);
      if (currentLang != null && currentLang.containsKey(key)) {
         return currentLang.get(key);
      }

      Map<String, String> fallback = this.languages.get("en_us");
      if (fallback != null && fallback.containsKey(key)) {
         return fallback.get(key);
      }

      if (!this.missingKeys.contains(key)) {
         StringBuilder missingLangs = new StringBuilder();

         for (String lang : this.languages.keySet()) {
            Map<String, String> langMap = this.languages.get(lang);
            if (langMap != null && !langMap.containsKey(key)) {
               if (missingLangs.length() > 0) {
                  missingLangs.append(", ");
               }

               missingLangs.append(lang);
            }
         }

         LoggerUtil.warn("No translation for: " + key + " | langs: " + missingLangs);
         this.missingKeys.add(key);
      }

      return key;
   }

   public String getFormatted(String key, Object... args) {
      String text = this.get(key);
      return String.format(text, args);
   }

   public boolean hasTranslation(String key) {
      Map<String, String> currentLang = this.languages.get(this.currentLanguage);
      return currentLang != null && currentLang.containsKey(key);
   }

   public void reload() {
      this.languages.clear();
      this.accessedKeys.clear();
      this.missingKeys.clear();
      this.loadLanguage("en_us");
      this.loadLanguage("ru_ru");
      LoggerUtil.info("All languages reloaded");
   }

   public void checkUnusedKeys() {
      Map<String, String> enLang = this.languages.get("en_us");
      if (enLang != null) {
         Set<String> unusedKeys = new HashSet<>();

         for (String key : enLang.keySet()) {
            if (!this.accessedKeys.contains(key)) {
               unusedKeys.add(key);
            }
         }

         if (!unusedKeys.isEmpty()) {
            LoggerUtil.warn("Found " + unusedKeys.size() + " unused translation keys:");

            for (String key : unusedKeys) {
               LoggerUtil.warn("  Unused key: " + key);
            }
         } else {
            LoggerUtil.info("No unused translation keys found");
         }
      }
   }

   public void printStatistics() {
      Map<String, String> enLang = this.languages.get("en_us");
      if (enLang != null) {
         int totalKeys = enLang.size();
         int accessedCount = this.accessedKeys.size();
         int missingCount = this.missingKeys.size();
         int unusedCount = totalKeys - accessedCount;
         LoggerUtil.info("=== Translation Statistics ===");
         LoggerUtil.info("Total keys in en_us: " + totalKeys);
         LoggerUtil.info("Accessed keys: " + accessedCount);
         LoggerUtil.info("Missing keys: " + missingCount);
         LoggerUtil.info("Unused keys: " + unusedCount);
         LoggerUtil.info("==============================");
      }
   }

   public String getCurrentLanguage() {
      return this.currentLanguage;
   }
}
