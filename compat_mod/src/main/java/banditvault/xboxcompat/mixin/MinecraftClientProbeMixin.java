package banditvault.xboxcompat.mixin;

import banditvault.xboxcompat.XboxCompatLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientProbeMixin {
    private static long banditvault$tickCount = 0L;
    private static boolean banditvault$uncaughtHandlerInstalled = false;

    @Shadow
    public Screen screen;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void banditvault$logClientConstructed(GameConfig args, CallbackInfo ci) {
        XboxCompatLog.log("MinecraftClient constructed");
        banditvault$installUncaughtExceptionHandler();
    }

    @Inject(method = "run", at = @At("HEAD"))
    private void banditvault$logMainLoopEntered(CallbackInfo ci) {
        XboxCompatLog.log("MinecraftClient main loop entered");
    }

    @Inject(method = "run", at = @At("TAIL"))
    private void banditvault$logMainLoopExited(CallbackInfo ci) {
        XboxCompatLog.log("MinecraftClient main loop exited");
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void banditvault$logSetScreenHead(Screen screen, CallbackInfo ci) {
        XboxCompatLog.log("setScreen head -> " + banditvault$screenName(screen));
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void banditvault$logSetScreenTail(Screen screen, CallbackInfo ci) {
        XboxCompatLog.log("setScreen tail -> current=" + banditvault$screenName(this.screen));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void banditvault$logClientTick(CallbackInfo ci) {
        banditvault$tickCount++;
        if (banditvault$tickCount <= 5 || banditvault$tickCount % 120 == 0) {
            XboxCompatLog.log("client tick=" + banditvault$tickCount
                + " screen=" + banditvault$screenName(this.screen));
        }
    }

    private static String banditvault$screenName(Screen screen) {
        return screen == null ? "null" : screen.getClass().getName();
    }

    private static synchronized void banditvault$installUncaughtExceptionHandler() {
        if (banditvault$uncaughtHandlerInstalled) {
            return;
        }

        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            XboxCompatLog.logException(
                "Uncaught exception on thread=" + thread.getName(),
                throwable);
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        });
        banditvault$uncaughtHandlerInstalled = true;
        XboxCompatLog.log("Installed default uncaught exception handler");
    }
}
