package padej.soup.api.file.impl;

import java.io.File;
import padej.soup.api.file.ClientFile;
import padej.soup.api.file.exception.FileLoadException;
import padej.soup.api.file.exception.FileSaveException;
import padej.soup.api.repository.config.Config;
import padej.soup.api.repository.config.ConfigManager;
import padej.soup.api.repository.config.ConfigUtils;
import padej.soup.base.util.logger.LoggerUtil;

public class ConfigFile extends ClientFile {
   public ConfigFile() {
      super("configs");
   }

   @Override
   public void saveToFile(File path) throws FileSaveException {
      ConfigUtils.saveActiveConfig();
   }

   @Override
   public void loadFromFile(File path) throws FileLoadException {
      ConfigUtils.clear();
      ConfigUtils.loadConfigsFromDirectory();
   }

   public void saveConfig(String configName) throws FileSaveException {
      try {
         if (ConfigUtils.getConfigManager() != null) {
            ConfigUtils.getConfigManager().save(configName);
            File configFile = new File(ConfigManager.CONFIGS_FOLDER, configName + ".soup");
            boolean isNewConfig = !ConfigUtils.isConfig(configName);
            if (isNewConfig) {
               String description = "Custom configuration";
               Config config = new Config(configName, description, configName + ".soup", configFile.lastModified());
               ConfigUtils.addConfig(config);
            } else {
               Config existingConfig = ConfigUtils.getConfig(configName);
               if (existingConfig != null) {
                  existingConfig.setLastModified(configFile.lastModified());
               }
            }
         }
      } catch (Exception e) {
         throw new FileSaveException("Failed to save config: " + configName, e);
      }
   }

   public void loadConfig(String configName) throws FileLoadException {
      try {
         Config config = ConfigUtils.getConfig(configName);
         if (config == null) {
            throw new FileLoadException("Config not found: " + configName);
         }

         File configFile = new File(ConfigManager.CONFIGS_FOLDER, config.getFileName());
         if (!configFile.exists()) {
            throw new FileLoadException("Config file not found: " + config.getFileName());
         }

         if (!configFile.canRead()) {
            throw new FileLoadException("Config file is not readable: " + config.getFileName());
         }

         if (configFile.length() == 0L) {
            throw new FileLoadException("Config file is empty: " + config.getFileName());
         }

         if (ConfigUtils.getConfigManager() != null) {
            ConfigUtils.getConfigManager().load(configName);
            LoggerUtil.info("Loaded config: " + configName);
         }
      } catch (FileLoadException e) {
         throw e;
      } catch (Exception e) {
         throw new FileLoadException("Failed to load config: " + configName, e);
      }
   }

   public void deleteConfig(String configName) throws FileSaveException {
      try {
         ConfigUtils.deleteConfig(configName);
      } catch (Exception e) {
         throw new FileSaveException("Failed to delete config: " + configName, e);
      }
   }

   public void copyConfig(String sourceConfigName, String newConfigName) throws FileSaveException {
      try {
         Config sourceConfig = ConfigUtils.getConfig(sourceConfigName);
         if (sourceConfig == null) {
            throw new FileSaveException("Source config not found: " + sourceConfigName);
         }

         if (ConfigUtils.isConfig(newConfigName)) {
            throw new FileSaveException("Config with name already exists: " + newConfigName);
         }

         File sourceFile = new File(ConfigManager.CONFIGS_FOLDER, sourceConfig.getFileName());
         if (!sourceFile.exists()) {
            throw new FileSaveException("Source config file not found: " + sourceConfig.getFileName());
         }

         if (ConfigUtils.getConfigManager() != null) {
            ConfigUtils.getConfigManager().load(sourceConfigName);
            ConfigUtils.getConfigManager().save(newConfigName);
            File newFile = new File(ConfigManager.CONFIGS_FOLDER, newConfigName + ".soup");
            String description = "Custom configuration";
            Config newConfig = new Config(newConfigName, description, newConfigName + ".soup", newFile.lastModified());
            ConfigUtils.addConfig(newConfig);
         }
      } catch (Exception e) {
         throw new FileSaveException("Failed to copy config from " + sourceConfigName + " to " + newConfigName, e);
      }
   }

   public boolean isConfigValid(String configName) {
      try {
         Config config = ConfigUtils.getConfig(configName);
         if (config == null) {
            return false;
         }

         File configFile = new File(ConfigUtils.getConfigDirectory(), config.getFileName());
         return configFile.exists() && configFile.canRead() && configFile.length() != 0L;
      } catch (Exception e) {
         return false;
      }
   }
}
