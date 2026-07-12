package dev.streamline.frame.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;
import dev.streamline.frame.FramePacer;

/**
 * Precise frame limiter hook: wraps the one {@code RenderSystem.limitDisplayFPS} call in the render loop
 * ({@code @WrapOperation}, so another mod wrapping the same call chains instead of conflicting). Vanilla
 * only makes the call when a cap below 260 is active - exactly when pacing matters.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftFrameLimiterMixin {

    @WrapOperation(
        method = "runTick(Z)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;limitDisplayFPS(I)V"))
    private void streamline$pace(final int fps, final Operation<Void> original) {
        StreamlineState.armedFrameLimiter = true;
        if (StreamlineConfig.preciseLimiter()) {
            FramePacer.pace(fps);
        } else {
            FramePacer.reset();
            original.call(fps);
        }
    }
}
