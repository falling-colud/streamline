package dev.streamline.frame.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.streamline.core.StreamlineState;
import dev.streamline.frame.FpsGovernor;

/**
 * Idle detection: any mouse event (move, click, scroll - the GLFW callbacks funnel through these three
 * handlers) stamps the governor's last-input time. HEAD inserts only; nothing about the handling changes.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerActivityMixin {

    @Inject(method = {"onPress", "onScroll", "onMove"}, at = @At("HEAD"))
    private void streamline$noteActivity(final CallbackInfo ci) {
        StreamlineState.armedActivityMouse = true;
        FpsGovernor.noteInput();
    }
}
