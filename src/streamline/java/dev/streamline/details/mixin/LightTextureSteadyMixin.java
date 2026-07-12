package dev.streamline.details.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;
import dev.streamline.details.LightmapMemo;

/**
 * Steady lightmap: vanilla re-randomizes the block-light flicker every tick and marks the lightmap dirty
 * for it, so all 256 cells get recomputed and re-uploaded to the GPU 20 times a second even when nothing
 * is happening. With the option on, {@code tick()} pins the flicker to zero (the flicker's own average)
 * while still marking dirty, and the {@code updateLightTexture} head consults {@link LightmapMemo}: if
 * every input matches the last rebuild bit-for-bit, the dirty flag is cleared and the rebuild skipped.
 * Any real change - time of day, effects, gamma, storm flash, dimension - rebuilds exactly as vanilla.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureSteadyMixin {

    @Shadow
    private boolean updateLightTexture;
    @Shadow
    private float blockLightRedFlicker;
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private GameRenderer renderer;

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true)
    private void streamline$steadyFlicker(final CallbackInfo ci) {
        StreamlineState.armedLightmap = true;
        if (StreamlineConfig.steadyLightmap()) {
            this.blockLightRedFlicker = 0.0F;
            this.updateLightTexture = true;
            ci.cancel();
        }
    }

    @Inject(method = "updateLightTexture(F)V", at = @At("HEAD"), cancellable = true)
    private void streamline$skipUnchanged(final float partialTicks, final CallbackInfo ci) {
        if (!this.updateLightTexture)
            return; // vanilla body no-ops anyway
        if (!StreamlineConfig.steadyLightmap()) {
            LightmapMemo.invalidate();
            return;
        }
        if (LightmapMemo.unchanged(this.minecraft, this.renderer, partialTicks, this.blockLightRedFlicker)) {
            this.updateLightTexture = false;
            StreamlineState.lightmapSkips++;
            ci.cancel();
        } else {
            StreamlineState.lightmapRebuilds++;
        }
    }
}
