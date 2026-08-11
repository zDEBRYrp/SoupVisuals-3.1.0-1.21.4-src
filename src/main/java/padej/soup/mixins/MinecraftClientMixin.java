package padej.soup.mixins;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import padej.protect.ProtIgnore;
import padej.soup.api.event.EventManager;
import padej.soup.api.feature.draggable.AbstractDraggable;
import padej.soup.api.file.exception.FileProcessingException;
import padej.soup.api.system.font.Fonts;
import padej.soup.base.QuickImports;
import padej.soup.base.util.logger.LoggerUtil;
import padej.soup.core.Main;
import padej.soup.implement.events.container.SetScreenEvent;

@ProtIgnore
@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin implements QuickImports {
   @Shadow
   @Nullable
   public ClientPlayerEntity player;
   @Shadow
   @Nullable
   public Screen currentScreen;

   @Inject(at = @At("TAIL"), method = "<init>")
   private void onInit(RunArgs args, CallbackInfo ci) {
      Fonts.init();
   }

   @Inject(at = @At("HEAD"), method = "stop")
   private void stop(CallbackInfo ci) {
      if (Main.getInstance().isInitialized()) {
         if (Main.getInstance().getActivityManager() != null) {
            Main.getInstance().getActivityManager().endSession();
         }

         try {
            Main.getInstance().getFileController().saveFiles();
         } catch (FileProcessingException e) {
            LoggerUtil.error("Error occurred while saving files: " + e.getMessage() + " " + e.getCause());
         } finally {
            Main.getInstance().getFileController().stopAutoSave();
         }
      }
   }

   @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
   public void setScreenHook(Screen screen, CallbackInfo ci) {
      SetScreenEvent event = new SetScreenEvent(screen);
      EventManager.callEvent(event);

      for (AbstractDraggable draggable : Main.getInstance().getDraggableRepository().draggable()) {
         draggable.setScreen(event);
      }

      Screen eventScreen = event.getScreen();
      if (screen != eventScreen) {
         mc.setScreen(eventScreen);
         ci.cancel();
      }
   }
}
