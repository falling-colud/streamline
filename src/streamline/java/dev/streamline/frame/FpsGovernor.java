package dev.streamline.frame;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import dev.streamline.core.StreamlineConfig;

/**
 * Decides the frame cap for the current window state. Consulted once per frame from the
 * {@code Minecraft.getFramerateLimit()} hook (the single choke point vanilla's own limiter reads):
 *
 * <ul>
 *   <li><b>Minimized/hidden</b> &mdash; nothing is visible at all; a few FPS keep the client ticking
 *       (packet queues drain fully every tick, so multiplayer stays in sync even at 3 FPS).</li>
 *   <li><b>Unfocused</b> &mdash; the window is visible but the user is working elsewhere.</li>
 *   <li><b>Idle</b> &mdash; focused but no input for the configured delay. GLFW only reports key
 *       <i>presses</i>, so held movement keys (running straight, holding attack) are polled each frame
 *       and count as activity.</li>
 * </ul>
 *
 * <p>Caps never apply while a resource-reload overlay is up: the reload drains tasks on the render
 * thread, so throttling frames there would throttle the reload itself. The result is a plain
 * {@code min(vanilla, cap)} - it composes with vanilla's menu cap and any other mod's limit, and Fovea's
 * dynamic resolution reads {@link #appliedCapFps()} (reflectively, optional) to know that capped frame
 * times say nothing about render cost.</p>
 */
public final class FpsGovernor {

    /** What the governor is doing this frame, for the overlays. */
    public enum Mode { NONE, UNFOCUSED, MINIMIZED, IDLE }

    private static volatile long lastInputMs = System.currentTimeMillis();
    private static volatile int appliedCap = -1;
    private static volatile Mode mode = Mode.NONE;

    /** Called from the GLFW input callbacks (render thread) and the held-key poll. */
    public static void noteInput() {
        lastInputMs = System.currentTimeMillis();
    }

    /** The cap the governor applied this frame, or -1 when it is not limiting (read by Fovea's DRS). */
    public static int appliedCapFps() {
        return appliedCap;
    }

    public static Mode currentMode() {
        return mode;
    }

    /**
     * Per-frame heartbeat + clamp; {@code vanilla} is whatever vanilla decided (window limit, or 60 on
     * out-of-level menus).
     */
    public static int onFrameAndClamp(final int vanilla) {
        FrameStats.onFrame();

        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getOverlay() != null) {
            // Resource (re)loads run partly on the render thread - never slow them down.
            appliedCap = -1;
            mode = Mode.NONE;
            return vanilla;
        }

        final long handle = mc.getWindow().getWindow();
        final boolean minimized = GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
            || GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_VISIBLE) == GLFW.GLFW_FALSE;

        int cap = 0;
        Mode newMode = Mode.NONE;
        if (minimized) {
            cap = StreamlineConfig.minimizedFps();
            newMode = Mode.MINIMIZED;
        } else if (!mc.isWindowActive()) {
            cap = StreamlineConfig.unfocusedFps();
            newMode = Mode.UNFOCUSED;
        } else {
            if (anyBoundKeyHeld(mc))
                noteInput();
            final int idleFps = StreamlineConfig.idleFps();
            if (idleFps > 0
                && System.currentTimeMillis() - lastInputMs >= StreamlineConfig.idleDelaySeconds() * 1000L) {
                cap = idleFps;
                newMode = Mode.IDLE;
            }
        }

        if (cap > 0 && cap < vanilla) {
            appliedCap = cap;
            mode = newMode;
            return cap;
        }
        appliedCap = -1;
        mode = Mode.NONE;
        return vanilla;
    }

    /**
     * Held keys emit no GLFW events after the initial press; treat any held gameplay binding as activity
     * so cruising in a straight line or holding attack never counts as idle.
     */
    private static boolean anyBoundKeyHeld(final Minecraft mc) {
        if (mc.options == null)
            return false;
        return isDown(mc.options.keyUp) || isDown(mc.options.keyDown)
            || isDown(mc.options.keyLeft) || isDown(mc.options.keyRight)
            || isDown(mc.options.keyJump) || isDown(mc.options.keyShift)
            || isDown(mc.options.keySprint)
            || isDown(mc.options.keyAttack) || isDown(mc.options.keyUse);
    }

    private static boolean isDown(final KeyMapping key) {
        return key != null && key.isDown();
    }

    private FpsGovernor() {}
}
