package dev.streamline.frame.mixin;

import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;
import dev.streamline.frame.AdaptiveVsync;

/**
 * Adaptive VSync hook: after vanilla sets swap interval 0/1, upgrade an interval of 1 to -1 (late-frame
 * tearing) when the option is on and the driver supports it. Tail position means every vanilla vsync
 * path - startup, options screen, fullscreen toggle - re-applies our preference for free.
 */
@Mixin(Window.class)
public abstract class WindowAdaptiveVsyncMixin {

    @Inject(method = "updateVsync(Z)V", at = @At("TAIL"))
    private void streamline$adaptiveVsync(final boolean vsync, final CallbackInfo ci) {
        StreamlineState.armedVsyncHook = true;
        if (vsync && StreamlineConfig.adaptiveVsync() && AdaptiveVsync.tearSupported())
            GLFW.glfwSwapInterval(-1);
    }
}
