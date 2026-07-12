package dev.streamline.hud;

import java.util.List;

import net.minecraft.Util;

/**
 * Cache for the two F3 debug-text columns. Vanilla rebuilds both lists every frame with dozens of
 * {@code String.format} calls and hundreds of temporary strings; at high FPS that is real frame time and
 * constant allocation churn while the screen content changes at most a few times a second anyway. The
 * hooks serve the cached lists while fresh (100 ms) and refill on expiry - the on-screen values simply
 * refresh 10x/second. Render-thread only.
 */
public final class DebugHudCache {

    public static final long INTERVAL_MS = 100;

    private static List<String> game;
    private static long gameAtMs;
    private static List<String> system;
    private static long systemAtMs;

    public static List<String> freshGame() {
        return game != null && Util.getMillis() - gameAtMs < INTERVAL_MS ? game : null;
    }

    public static List<String> freshSystem() {
        return system != null && Util.getMillis() - systemAtMs < INTERVAL_MS ? system : null;
    }

    public static void storeGame(final List<String> lines) {
        game = lines;
        gameAtMs = Util.getMillis();
    }

    public static void storeSystem(final List<String> lines) {
        system = lines;
        systemAtMs = Util.getMillis();
    }

    private DebugHudCache() {}
}
