package dev.streamline.hud.mixin;

import java.util.List;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;
import dev.streamline.hud.DebugHudCache;

/**
 * Fast F3: serve the cached info columns while fresh (HEAD cancel), refill on natural completion
 * (RETURN - a HEAD cache hit returns before the original tail, so hits never re-store). NeoForge's own
 * debug lines are built inside these methods and are cached along with everything else. The renderer
 * itself (text drawing) is untouched.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayCacheMixin {

    @Inject(method = "getGameInformation", at = @At("HEAD"), cancellable = true)
    private void streamline$cachedGameInfo(final CallbackInfoReturnable<List<String>> cir) {
        StreamlineState.armedDebugHudCache = true;
        if (!StreamlineConfig.fastDebugHud())
            return;
        final List<String> fresh = DebugHudCache.freshGame();
        if (fresh != null)
            cir.setReturnValue(fresh);
    }

    @Inject(method = "getGameInformation", at = @At("RETURN"))
    private void streamline$storeGameInfo(final CallbackInfoReturnable<List<String>> cir) {
        if (StreamlineConfig.fastDebugHud())
            DebugHudCache.storeGame(cir.getReturnValue());
    }

    @Inject(method = "getSystemInformation", at = @At("HEAD"), cancellable = true)
    private void streamline$cachedSystemInfo(final CallbackInfoReturnable<List<String>> cir) {
        if (!StreamlineConfig.fastDebugHud())
            return;
        final List<String> fresh = DebugHudCache.freshSystem();
        if (fresh != null)
            cir.setReturnValue(fresh);
    }

    @Inject(method = "getSystemInformation", at = @At("RETURN"))
    private void streamline$storeSystemInfo(final CallbackInfoReturnable<List<String>> cir) {
        if (StreamlineConfig.fastDebugHud())
            DebugHudCache.storeSystem(cir.getReturnValue());
    }
}
