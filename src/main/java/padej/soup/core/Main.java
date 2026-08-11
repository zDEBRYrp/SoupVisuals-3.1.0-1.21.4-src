package padej.soup.core;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AfterTranslucent;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.Last;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import padej.soup.api.event.EventManager;
import padej.soup.api.feature.draggable.DraggableRepository;
import padej.soup.api.feature.module.ModuleProvider;
import padej.soup.api.feature.module.ModuleRepository;
import padej.soup.api.feature.module.ModuleSwitcher;
import padej.soup.api.file.DirectoryCreator;
import padej.soup.api.file.FileController;
import padej.soup.api.file.FileRepository;
import padej.soup.api.file.exception.FileProcessingException;
import padej.soup.api.repository.box.BoxRepository;
import padej.soup.api.repository.config.ConfigManager;
import padej.soup.api.repository.config.ConfigUtils;
import padej.soup.api.system.activity.ActivityManager;
import padej.soup.api.system.discord.DiscordManager;
import padej.soup.api.system.localization.LocalizationManager;
import padej.soup.api.system.sound.SoundManager;
import padej.soup.base.util.compat.ModCompatibility;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.base.util.other.RoleCache;
import padej.soup.base.util.render.Render3DUtil;
import padej.soup.base.util.render.ScissorManager;
import padej.soup.core.client.ClientInfo;
import padej.soup.core.client.ClientInfoProvider;
import padej.soup.core.listener.ListenerRepository;
import padej.soup.core.server.ServerApi;
import padej.soup.implement.events.render.WorldRenderEvent;
import padej.soup.implement.features.modules.other.EndCrystal;
import padej.soup.implement.menu.MenuScreen;

public class Main implements ModInitializer {
   public static MinecraftClient mc;
   static Main instance;
   private EventManager eventManager = new EventManager();
   private ModuleRepository moduleRepository;
   private ModuleSwitcher moduleSwitcher;
   private ModuleProvider moduleProvider;
   private DraggableRepository draggableRepository;
   private BoxRepository boxRepository;
   private DiscordManager discordManager;
   private ActivityManager activityManager;
   private FileRepository fileRepository;
   private FileController fileController;
   private ScissorManager scissorManager = new ScissorManager();
   private ClientInfoProvider clientInfoProvider;
   private ListenerRepository listenerRepository;
   private ConfigManager configManager;
   private ServerApi serverApi;
   private boolean initialized;
   private int live_time = 3600;

   @Override
   public void onInitialize() {
      instance = this;
      mc = MinecraftClient.getInstance();
      ModCompatibility.printCompatibilityInfo();
      this.initLocalization();
      this.initClientInfoProvider();
      this.initModules();
      this.initDraggable();
      this.initBox();
      this.initConfigManager();
      this.initFileManager();
      this.initListeners();
      this.initDiscordRPC();
      this.initActivityManager();
      this.initServerApi();
      SoundManager.init();
      MenuScreen menuScreen = new MenuScreen();
      menuScreen.initialize();
      this.initialized = true;
      WorldRenderEvents.AFTER_ENTITIES.register(EndCrystal.getInstance()::drawCrystals);
      WorldRenderEvents.AFTER_TRANSLUCENT.register((AfterTranslucent)context -> {
         if (context.advancedTranslucency()) {
            dispatchWorldRender(context);
         }
      });
      WorldRenderEvents.LAST.register((Last)context -> {
         if (!context.advancedTranslucency()) {
            dispatchWorldRender(context);
         }
      });
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         if (this.configManager != null) {
            LoggerUtil.info("Shutting down ConfigManager...");
            this.configManager.shutdown();
         }

         if (this.activityManager != null) {
            LoggerUtil.info("Saving activity data...");
            this.activityManager.saveToFile();
         }

         if (this.serverApi != null) {
            LoggerUtil.info("Stopping ServerApi...");
            this.serverApi.stop();
         }

         LoggerUtil.info("Shutting down RoleCache reconnection scheduler...");
         RoleCache.shutdown();
      }, "SoupAPI-Shutdown"));
   }

   private void initLocalization() {
      LocalizationManager.getInstance();
   }

   private void initDraggable() {
      this.draggableRepository = new DraggableRepository();
      this.draggableRepository.setup();
   }

   private void initBox() {
      this.boxRepository = new BoxRepository(this.eventManager);
   }

   private void initModules() {
      this.moduleRepository = new ModuleRepository();
      this.moduleRepository.setup();
      this.moduleProvider = new ModuleProvider(this.moduleRepository.modules());
      this.moduleSwitcher = new ModuleSwitcher(this.moduleRepository.modules(), this.eventManager);
   }

   private void initDiscordRPC() {
      this.discordManager = new DiscordManager();
      this.discordManager.init();
   }

   private void initServerApi() {
      this.serverApi = new ServerApi();
      this.serverApi.start();
      LoggerUtil.info("ServerApi initialized");
   }

   private void initClientInfoProvider() {
      File clientDirectory = new File(MinecraftClient.getInstance().runDirectory, "\\soupapi\\");
      File filesDirectory = new File(clientDirectory, "\\files\\");
      File moduleFilesDirectory = new File(filesDirectory, "\\config\\");
      this.clientInfoProvider = new ClientInfo(clientDirectory, filesDirectory, moduleFilesDirectory);
   }

   private void initConfigManager() {
      this.configManager = new ConfigManager(this.moduleRepository);
      this.configManager.setDraggableRepository(this.draggableRepository);
      ConfigUtils.initializeConfigManager(this.configManager);
   }

   private void initFileManager() {
      DirectoryCreator directoryCreator = new DirectoryCreator();
      directoryCreator.createDirectories(this.clientInfoProvider.clientDir(), this.clientInfoProvider.filesDir());
      this.fileRepository = new FileRepository();
      this.fileRepository.setup(this);
      this.fileController = new FileController(this.fileRepository.getClientFiles(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());

      try {
         this.fileController.loadFiles();
         this.fileController.startAutoSave();
      } catch (FileProcessingException e) {
         LoggerUtil.error("Error occurred while loading files: " + e.getMessage() + " " + e.getCause());
      }
   }

   private void initListeners() {
      this.listenerRepository = new ListenerRepository();
      this.listenerRepository.setup();
   }

   private void initActivityManager() {
      this.activityManager = new ActivityManager();
      this.activityManager.loadFromFile();
      this.activityManager.startSession();
   }

   private static void dispatchWorldRender(WorldRenderContext context) {
      if (context != null && context.projectionMatrix() != null && context.camera() != null) {
         MatrixStack legacyWorldStack = new MatrixStack();
         Matrix4f cameraRotationMatrix = new Matrix4f().rotation(new Quaternionf(context.camera().getRotation()).conjugate());
         legacyWorldStack.multiplyPositionMatrix(cameraRotationMatrix);
         legacyWorldStack.translate(context.camera().getPos().negate());
         Render3DUtil.setLastProjMat(RenderSystem.getProjectionMatrix());
         Render3DUtil.setLastWorldSpaceMatrix(legacyWorldStack.peek());
         Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
         modelViewStack.pushMatrix();
         modelViewStack.identity();

         try {
            EventManager.callEvent(new WorldRenderEvent(legacyWorldStack, context.tickCounter().getTickDelta(false)));
            Render3DUtil.onWorldRender();
         } finally {
            modelViewStack.popMatrix();
         }
      }
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public ModuleRepository getModuleRepository() {
      return this.moduleRepository;
   }

   public ModuleSwitcher getModuleSwitcher() {
      return this.moduleSwitcher;
   }

   public ModuleProvider getModuleProvider() {
      return this.moduleProvider;
   }

   public DraggableRepository getDraggableRepository() {
      return this.draggableRepository;
   }

   public BoxRepository getBoxRepository() {
      return this.boxRepository;
   }

   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }

   public ActivityManager getActivityManager() {
      return this.activityManager;
   }

   public FileRepository getFileRepository() {
      return this.fileRepository;
   }

   public FileController getFileController() {
      return this.fileController;
   }

   public ScissorManager getScissorManager() {
      return this.scissorManager;
   }

   public ClientInfoProvider getClientInfoProvider() {
      return this.clientInfoProvider;
   }

   public ListenerRepository getListenerRepository() {
      return this.listenerRepository;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public ServerApi getServerApi() {
      return this.serverApi;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public int getLive_time() {
      return this.live_time;
   }

   public void setEventManager(EventManager eventManager) {
      this.eventManager = eventManager;
   }

   public void setModuleRepository(ModuleRepository moduleRepository) {
      this.moduleRepository = moduleRepository;
   }

   public void setModuleSwitcher(ModuleSwitcher moduleSwitcher) {
      this.moduleSwitcher = moduleSwitcher;
   }

   public void setModuleProvider(ModuleProvider moduleProvider) {
      this.moduleProvider = moduleProvider;
   }

   public void setDraggableRepository(DraggableRepository draggableRepository) {
      this.draggableRepository = draggableRepository;
   }

   public void setBoxRepository(BoxRepository boxRepository) {
      this.boxRepository = boxRepository;
   }

   public void setDiscordManager(DiscordManager discordManager) {
      this.discordManager = discordManager;
   }

   public void setActivityManager(ActivityManager activityManager) {
      this.activityManager = activityManager;
   }

   public void setFileRepository(FileRepository fileRepository) {
      this.fileRepository = fileRepository;
   }

   public void setFileController(FileController fileController) {
      this.fileController = fileController;
   }

   public void setScissorManager(ScissorManager scissorManager) {
      this.scissorManager = scissorManager;
   }

   public void setClientInfoProvider(ClientInfoProvider clientInfoProvider) {
      this.clientInfoProvider = clientInfoProvider;
   }

   public void setListenerRepository(ListenerRepository listenerRepository) {
      this.listenerRepository = listenerRepository;
   }

   public void setConfigManager(ConfigManager configManager) {
      this.configManager = configManager;
   }

   public void setServerApi(ServerApi serverApi) {
      this.serverApi = serverApi;
   }

   public void setInitialized(boolean initialized) {
      this.initialized = initialized;
   }

   public void setLive_time(int live_time) {
      this.live_time = live_time;
   }

   public static Main getInstance() {
      return instance;
   }
}
