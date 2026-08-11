package padej.soup.api.file.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;

public class CurrentLanguageFile extends ClientFile {
   private String currentLanguage = "en_us";

   public CurrentLanguageFile() {
      super("currentLang");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      this.saveToFile(path, this.getName());
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      this.loadFromFile(path, this.getName());
   }

   @Override
   public void saveToFile(File path, String fileName) throws FileSaveException {
      if (this.currentLanguage == null || this.currentLanguage.trim().isEmpty()) {
         this.currentLanguage = "en_us";
      }

      File file = new File(path, fileName);

      try (FileWriter writer = new FileWriter(file)) {
         writer.write(this.currentLanguage.trim());
      } catch (IOException e) {
         throw new FileSaveException("Failed to save current language to file", e);
      }

      super.saveToFile(path, fileName);
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         this.currentLanguage = "en_us";
      } else {
         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
               this.currentLanguage = line.trim();
            } else {
               this.currentLanguage = "en_us";
            }
         } catch (IOException e) {
            throw new FileLoadException("Failed to load current language from file", e);
         }

         super.loadFromFile(path, fileName);
      }
   }

   public boolean hasCurrentLanguage() {
      return this.currentLanguage != null && !this.currentLanguage.trim().isEmpty();
   }

   public void setCurrentLanguage(String currentLanguage) {
      this.currentLanguage = currentLanguage;
   }

   public String getCurrentLanguage() {
      return this.currentLanguage;
   }
}
