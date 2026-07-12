package dev.streamline.frame;

import java.util.Arrays;

/**
 * Frame interval statistics for the FPS meter, fed once per frame from the {@code getFramerateLimit}
 * hook (the most reliable per-frame heartbeat: vanilla calls it every loop iteration, menus included).
 * Render-thread only; the overlays read from the same thread.
 */
public final class FrameStats {

    private static final int WINDOW = 240;
    private static final long RECOMPUTE_INTERVAL_MS = 250;

    private static final float[] intervalsMs = new float[WINDOW];
    private static int index;
    private static int filled;
    private static long lastFrameNanos;

    private static long lastComputeMs;
    private static double avgMs;
    private static double onePercentLowFps;

    static void onFrame() {
        final long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            final float ms = (now - lastFrameNanos) / 1.0e6f;
            if (ms < 1000.0f) {
                intervalsMs[index] = ms;
                index = (index + 1) % WINDOW;
                if (filled < WINDOW)
                    filled++;
            } else {
                // A stall (world load, shader compile) is not a frame cadence; restart the window.
                filled = 0;
                index = 0;
            }
        }
        lastFrameNanos = now;
    }

    /** Average frame time over the window, in milliseconds (0 until enough frames arrived). */
    public static double averageMs() {
        maybeRecompute();
        return avgMs;
    }

    /** The classic "1% low": the FPS corresponding to the worst 1% of recent frame times. */
    public static double onePercentLowFps() {
        maybeRecompute();
        return onePercentLowFps;
    }

    private static void maybeRecompute() {
        final long nowMs = System.currentTimeMillis();
        if (nowMs - lastComputeMs < RECOMPUTE_INTERVAL_MS)
            return;
        lastComputeMs = nowMs;
        if (filled < 30) {
            avgMs = 0;
            onePercentLowFps = 0;
            return;
        }
        final float[] copy = Arrays.copyOf(intervalsMs, filled);
        double sum = 0;
        for (final float v : copy)
            sum += v;
        avgMs = sum / filled;
        Arrays.sort(copy);
        final float p99 = copy[Math.min(filled - 1, (int) Math.ceil(filled * 0.99) - 1)];
        onePercentLowFps = p99 > 0 ? 1000.0 / p99 : 0;
    }

    private FrameStats() {}
}
