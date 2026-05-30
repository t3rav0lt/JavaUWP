package banditvault.xboxcompat.mixin;

import banditvault.xboxcompat.XboxCompatLog;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelLoadTracker.class)
public abstract class WorldLoadProgressTrackerMixin {
    @Unique
    private long banditvault$chunkEventCount = 0L;

    @Unique
    private boolean banditvault$readyLogged = false;

    @Shadow
    public abstract float serverProgress();

    @Inject(method = "setServerChunkStatusView", at = @At("TAIL"))
    private void banditvault$logTrackerListener(ChunkLoadStatusView listener, CallbackInfo ci) {
        XboxCompatLog.log("world-load tracker listener=" + banditvault$className(listener));
    }

    @Inject(method = "start", at = @At("TAIL"))
    private void banditvault$logStageStarted(LevelLoadListener.Stage stage, int total, CallbackInfo ci) {
        XboxCompatLog.log("world-load stage start stage=" + banditvault$stageName(stage)
            + " total=" + total);
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void banditvault$logStageProgress(LevelLoadListener.Stage stage, int current, int total, CallbackInfo ci) {
        if (current <= 1 || current == total || current % 128 == 0) {
            XboxCompatLog.log("world-load stage progress stage=" + banditvault$stageName(stage)
                + " current=" + current + "/" + total
                + " overall=" + this.serverProgress());
        }
    }

    @Inject(method = "finish", at = @At("TAIL"))
    private void banditvault$logStageFinished(LevelLoadListener.Stage stage, CallbackInfo ci) {
        XboxCompatLog.log("world-load stage finish stage=" + banditvault$stageName(stage)
            + " overall=" + this.serverProgress());
    }

    @Inject(method = "updateFocus", at = @At("TAIL"))
    private void banditvault$logChunkEvent(ResourceKey<Level> dimension, ChunkPos chunkPos, CallbackInfo ci) {
        this.banditvault$chunkEventCount++;
        if (this.banditvault$chunkEventCount <= 16 || this.banditvault$chunkEventCount % 128 == 0) {
            XboxCompatLog.log("world-load chunk event count=" + this.banditvault$chunkEventCount
                + " dim=" + dimension
                + " chunk=" + chunkPos);
        }
    }

    @Inject(method = "isLevelReady", at = @At("TAIL"))
    private void banditvault$logReady(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && !this.banditvault$readyLogged) {
            this.banditvault$readyLogged = true;
            XboxCompatLog.log("world-load tracker ready chunkEvents=" + this.banditvault$chunkEventCount
                + " overall=" + this.serverProgress());
        }
    }

    @Unique
    private static String banditvault$stageName(LevelLoadListener.Stage stage) {
        return stage == null ? "null" : stage.name() + "#" + stage.ordinal();
    }

    @Unique
    private static String banditvault$className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
