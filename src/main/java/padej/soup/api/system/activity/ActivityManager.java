package padej.soup.api.system.activity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import padej.soup.core.Main;

public class ActivityManager {
   private static final String CALENDAR_FILE = "calendar.json";
   private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
   private Map<String, ActivityData> activities = new HashMap<>();
   private long sessionStartTime = 0L;
   private boolean isInGame = false;
   private ScheduledExecutorService autoSaveExecutor;
   private volatile boolean shouldStop = false;

   public void startSession() {
      if (!this.isInGame) {
         this.sessionStartTime = System.currentTimeMillis();
         this.isInGame = true;
         this.startAutoSave();
      }
   }

   public void endSession() {
      if (this.isInGame && this.sessionStartTime > 0L) {
         long endTime = System.currentTimeMillis();
         String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
         ActivityData dayData = this.activities.computeIfAbsent(today, ActivityData::new);
         dayData.addSession(this.sessionStartTime, endTime);
         this.saveToFile();
         this.isInGame = false;
         this.sessionStartTime = 0L;
         this.stopAutoSave();
      }
   }

   public ActivityData getActivityForDate(String date) {
      return this.activities.getOrDefault(date, new ActivityData(date));
   }

   public void saveToFile() {
      try {
         File filesDir = Main.getInstance().getClientInfoProvider().filesDir();
         if (!filesDir.exists()) {
            filesDir.mkdirs();
         }

         File file = new File(filesDir, "calendar.json");

         try (FileWriter writer = new FileWriter(file)) {
            this.gson.toJson(this.activities, writer);
         }
      } catch (IOException e) {
         System.err.println("Failed to save activity data: " + e.getMessage());
      }
   }

   public void loadFromFile() {
      try {
         File filesDir = Main.getInstance().getClientInfoProvider().filesDir();
         File file = new File(filesDir, "calendar.json");
         if (!file.exists()) {
            File oldFile = new File("config", "calendar.json");
            if (!oldFile.exists()) {
               return;
            }

            file = oldFile;
         }

         try (FileReader reader = new FileReader(file)) {
            Type type = (new TypeToken<Map<String, ActivityData>>() {}).getType();
            Map<String, ActivityData> loaded = (Map<String, ActivityData>)this.gson.fromJson(reader, type);
            if (loaded != null) {
               this.activities = loaded;
            }
         }
      } catch (IOException e) {
         System.err.println("Failed to load activity data: " + e.getMessage());
      }
   }

   public long getTotalPlayTime() {
      return this.activities.values().stream().mapToLong(ActivityData::getTotalPlayTime).sum();
   }

   public String getFormattedTotalPlayTime() {
      long totalMs = this.getTotalPlayTime();
      long hours = totalMs / 3600000L;
      long minutes = totalMs % 3600000L / 60000L;
      return String.format("%dh %dm", hours, minutes);
   }

   private void startAutoSave() {
      if (this.autoSaveExecutor == null || this.autoSaveExecutor.isShutdown()) {
         this.autoSaveExecutor = Executors.newScheduledThreadPool(1);
         this.shouldStop = false;
         this.autoSaveExecutor.scheduleAtFixedRate(() -> {
            if (!this.shouldStop && this.isInGame && this.sessionStartTime > 0L) {
               long currentTime = System.currentTimeMillis();
               String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
               ActivityData dayData = this.activities.computeIfAbsent(today, ActivityData::new);
               dayData.addSession(this.sessionStartTime, currentTime);
               this.saveToFile();
               this.sessionStartTime = currentTime;
            }
         }, 15L, 15L, TimeUnit.SECONDS);
      }
   }

   private void stopAutoSave() {
      this.shouldStop = true;
      if (this.autoSaveExecutor != null && !this.autoSaveExecutor.isShutdown()) {
         this.autoSaveExecutor.shutdown();

         try {
            if (!this.autoSaveExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
               this.autoSaveExecutor.shutdownNow();
            }
         } catch (InterruptedException e) {
            this.autoSaveExecutor.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }

   public Map<String, ActivityData> getActivities() {
      return this.activities;
   }

   public long getSessionStartTime() {
      return this.sessionStartTime;
   }
}
