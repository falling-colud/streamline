package dev.streamline.core;

/**
 * Cross-cutting runtime state: hook armed flags (every injector runs {@code require 0}, so the debug overlay
 * can show a silent mixin mismatch instead of a mystery) and per-tick counters for the overlay.
 */
public final class StreamlineState {

    // --- armed flags (set the first time each hook actually runs) ---
    public static volatile boolean armedFramerateHook;
    public static volatile boolean armedFrameLimiter;
    public static volatile boolean armedActivityMouse;
    public static volatile boolean armedActivityKeyboard;
    public static volatile boolean armedVsyncHook;
    public static volatile boolean armedTickerCapture;
    public static volatile boolean armedTickerGate;
    public static volatile boolean armedAmbience;
    public static volatile boolean armedLightmap;
    public static volatile boolean armedVignette;
    public static volatile boolean armedGlint;
    public static volatile boolean armedParticlePhysics;
    public static volatile boolean armedHudBatching;
    public static volatile boolean armedDebugHudCache;

    // --- live counters (render thread), rolled once per client tick for the overlay ---
    public static int frozenSpriteUploads;
    public static int lightmapRebuilds;
    public static int lightmapSkips;
    public static int glintPassesSkipped;
    public static int particleCollisionsFast;
    public static int particleCollisionsFull;

    public static int lastFrozenSpriteUploads;
    public static int lastLightmapRebuilds;
    public static int lastLightmapSkips;
    public static int lastGlintPassesSkipped;
    public static int lastParticleCollisionsFast;
    public static int lastParticleCollisionsFull;

    /** Called once per client tick (render thread). */
    public static void rollCounters() {
        lastFrozenSpriteUploads = frozenSpriteUploads;
        lastLightmapRebuilds = lightmapRebuilds;
        lastLightmapSkips = lightmapSkips;
        lastGlintPassesSkipped = glintPassesSkipped;
        lastParticleCollisionsFast = particleCollisionsFast;
        lastParticleCollisionsFull = particleCollisionsFull;
        frozenSpriteUploads = 0;
        lightmapRebuilds = 0;
        lightmapSkips = 0;
        glintPassesSkipped = 0;
        particleCollisionsFast = 0;
        particleCollisionsFull = 0;
    }

    private StreamlineState() {}
}
