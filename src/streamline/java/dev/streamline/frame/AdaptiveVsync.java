package dev.streamline.frame;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Adaptive VSync: with the option on and VSync enabled, swap interval -1 is requested instead of 1 -
 * the driver syncs to the refresh rate while frames are on time but tears instead of stalling a whole
 * refresh when one runs late (the classic "vsync halves your FPS the moment you dip below 60" problem).
 *
 * <p>Support ({@code WGL/GLX_EXT_swap_control_tear}) is probed once on the render thread and cached;
 * unsupported drivers silently keep normal VSync. Applied from the tail of {@code Window.updateVsync},
 * so every vanilla path (startup, options screen, fullscreen toggle) re-applies it naturally.</p>
 */
public final class AdaptiveVsync {

    private static Boolean tearSupported;

    /** Render thread only (GLFW extension probing needs the current GL context). */
    public static boolean tearSupported() {
        if (tearSupported == null) {
            boolean supported;
            try {
                supported = GLFW.glfwExtensionSupported("WGL_EXT_swap_control_tear")
                    || GLFW.glfwExtensionSupported("GLX_EXT_swap_control_tear");
            } catch (final Throwable t) {
                supported = false;
            }
            tearSupported = supported;
        }
        return tearSupported;
    }

    /**
     * Re-push the vanilla vsync state through {@code Window.updateVsync} so a toggle of the adaptive
     * option takes effect immediately (the mixin tail re-evaluates). Called from the Sodium option
     * binding, which runs on the render thread.
     */
    public static void reapply() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null)
            mc.getWindow().updateVsync(mc.options.enableVsync().get());
    }

    private AdaptiveVsync() {}
}
