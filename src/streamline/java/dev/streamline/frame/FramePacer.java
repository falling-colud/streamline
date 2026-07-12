package dev.streamline.frame;

import org.lwjgl.glfw.GLFW;

/**
 * Precise frame limiter. Vanilla's {@code RenderSystem.limitDisplayFPS} loops
 * {@code glfwWaitEventsTimeout} until the target time - on Windows that wait routinely overshoots by a
 * millisecond or more, so "60 FPS" is really an uneven 57-60 with visible cadence wobble. This pacer
 * does the same event-pumping wait only up to a couple of milliseconds <i>before</i> the target, then
 * spins the remainder ({@code Thread.onSpinWait}), landing frames within microseconds of the cadence.
 *
 * <p>The spin costs a sliver of one core per frame, which is why it is opt-in. Events still get pumped:
 * the coarse phase uses the exact same GLFW wait vanilla uses, and {@code RenderSystem.flipFrame} polls
 * every frame regardless. Cadence anchors to the previous <i>target</i> (not the wake-up time), so jitter
 * does not accumulate; falling behind by more than a period resets the anchor instead of sprinting.</p>
 */
public final class FramePacer {

    /** Stop the timed wait this early and spin the rest (Windows timer slop is ~1-2 ms). */
    private static final long SPIN_WINDOW_NANOS = 2_000_000L;

    private static long nextTargetNanos;

    /** Render thread only (vanilla calls the limiter right after the buffer swap). */
    public static void pace(final int fps) {
        final long period = 1_000_000_000L / Math.max(1, fps);
        final long now = System.nanoTime();

        long target = nextTargetNanos + period;
        if (nextTargetNanos == 0 || now > target + period) {
            // First frame, or we fell more than a whole period behind: re-anchor, render immediately.
            nextTargetNanos = now;
            return;
        }

        long remaining;
        while ((remaining = target - System.nanoTime()) > SPIN_WINDOW_NANOS) {
            GLFW.glfwWaitEventsTimeout((remaining - SPIN_WINDOW_NANOS) / 1.0e9);
        }
        while (System.nanoTime() < target) {
            Thread.onSpinWait();
        }
        nextTargetNanos = target;
    }

    /** Called while the pacer is disabled so re-enabling starts from a fresh anchor. */
    public static void reset() {
        nextTargetNanos = 0;
    }

    private FramePacer() {}
}
