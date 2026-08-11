package padej.soup.api.file;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.api.file.impl.ModuleFile;
import padej.soup.base.util.logger.LoggerUtil;

public class FileController {
   private final List<ClientFile> clientFiles;
   private final File directory;
   private final File moduleConfigDirectory;
   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   public FileController(List<ClientFile> clientFiles, File directory, File moduleConfigDirectory) {
      this.clientFiles = clientFiles;
      this.directory = directory;
      this.moduleConfigDirectory = moduleConfigDirectory;
   }

   public void startAutoSave() {
      this.scheduler.scheduleAtFixedRate(() -> {
         try {
            this.saveFiles();
         } catch (FileSaveException e) {
            LoggerUtil.error("Failed to auto-save files: {}", e.getMessage());
         }
      }, 1L, 1L, TimeUnit.MINUTES);
   }

   public void stopAutoSave() {
      this.scheduler.shutdown();

      try {
         if (!this.scheduler.awaitTermination(1L, TimeUnit.MINUTES)) {
            this.scheduler.shutdownNow();
         }
      } catch (InterruptedException e) {
         this.scheduler.shutdownNow();
      }
   }

   public void saveFiles() throws FileSaveException {
      if (!this.clientFiles.isEmpty()) {
         for (ClientFile clientFile : this.clientFiles) {
            try {
               clientFile.saveToFile(this.directory);
            } catch (FileSaveException e) {
               throw new FileSaveException("Failed to save file: " + clientFile.getName(), e);
            }
         }
      }
   }

   public void loadFiles() throws FileLoadException {
      if (this.clientFiles.isEmpty()) {
         LoggerUtil.warn("No files to load from directory: {}", this.directory.getPath());
      } else {
         for (ClientFile clientFile : this.clientFiles) {
            try {
               clientFile.loadFromFile(this.directory);
            } catch (FileLoadException e) {
               throw new FileLoadException("Failed to load file: " + clientFile.getName(), e);
            }
         }
      }
   }

   public void saveFile(String fileName) throws FileSaveException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.saveToFile(this.moduleConfigDirectory, fileName);
            } catch (FileSaveException e) {
               throw new FileSaveException("Failed to save file: " + fileName, e);
            }
         }
      }
   }

   public void loadFile(String fileName) throws FileLoadException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.loadFromFile(this.moduleConfigDirectory, fileName);
            } catch (FileLoadException e) {
               throw new FileLoadException("Failed to load file: " + fileName, e);
            }
         }
      }
   }
}
