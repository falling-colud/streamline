package dev.streamline.frame.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.streamline.core.StreamlineState;
import dev.streamline.frame.FpsGovernor;

/**
 * FPS governor hook: {@code getFramerateLimit()} is the single choke point vanilla's limiter reads once
 * per frame (menus included), so clamping its return value composes with vanilla's own out-of-level
 * 60 FPS cap and with anything else that adjusts the window limit. Also the per-frame heartbeat for the
 * FPS meter's frame-time window.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftFramerateLimitMixin {

    @ModifyReturnValue(method = "getFramerateLimit", at = @At("RETURN"))
    private int streamline$governFps(final int original) {
        StreamlineState.armedFramerateHook = true;
        return FpsGovernor.onFrameAndClamp(original);
    }
}
