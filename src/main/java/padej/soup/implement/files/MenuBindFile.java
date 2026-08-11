package padej.soup.implement.files;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;

public class MenuBindFile extends ClientFile {
   private int menuKey = 48;

   public MenuBindFile() {
      super("menuBind");
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
      File file = new File(path, fileName);

      try (FileWriter writer = new FileWriter(file)) {
         writer.write(String.valueOf(this.menuKey));
      } catch (IOException e) {
         throw new FileSaveException("Failed to save menu key to file", e);
      }

      super.saveToFile(path, fileName);
   }

   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      if (!file.exists()) {
         this.menuKey = 48;
      } else {
         try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
               try {
                  this.menuKey = Integer.parseInt(line.trim());
               } catch (NumberFormatException e) {
                  this.menuKey = 48;
               }
            } else {
               this.menuKey = 48;
            }
         } catch (IOException e) {
            throw new FileLoadException("Failed to load menu key from file", e);
         }

         super.loadFromFile(path, fileName);
      }
   }

   public void setMenuKey(int menuKey) {
      this.menuKey = menuKey;
   }

   public int getMenuKey() {
      return this.menuKey;
   }
}
