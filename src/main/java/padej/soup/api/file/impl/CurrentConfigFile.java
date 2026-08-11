package padej.soup.api.file.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;

public class CurrentConfigFile extends ClientFile {
   private String currentConfigName = null;

   public CurrentConfigFile() {
      super("currentCfg");
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
      if (this.currentConfigName != null && !this.currentConfigName.trim().isEmpty()) {
         File file = new File(path, fileName);
         File tempFile = new File(path, fileName + ".tmp");

         try {
            FileWriter writer = new FileWriter(tempFile);

            try {
               writer.write(this.currentConfigName.trim());
            } catch (Throwable var14) {
               try {
                  writer.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }

               throw var14;
            }

            writer.close();
            if (file.exists()) {
               file.delete();
            }

            if (!tempFile.renameTo(file)) {
               throw new FileSaveException("Failed to rename temp file");
            }
         } catch (IOException e) {
            throw new FileSaveException("Failed to save current config name to file", e);
         } finally {
            if (tempFile.exists()) {
               tempFile.delete();
            }
         }

         super.saveToFile(path, fileName);
      }
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         this.currentConfigName = null;
      } else {
         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
               this.currentConfigName = line.trim();
            } else {
               this.currentConfigName = null;
            }
         } catch (IOException e) {
            throw new FileLoadException("Failed to load current config name from file", e);
         }

         super.loadFromFile(path, fileName);
      }
   }

   public boolean hasCurrentConfig() {
      return this.currentConfigName != null && !this.currentConfigName.trim().isEmpty();
   }

   public void setCurrentConfigName(String currentConfigName) {
      this.currentConfigName = currentConfigName;
   }

   public String getCurrentConfigName() {
      return this.currentConfigName;
   }
}
