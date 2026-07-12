package dev.streamline.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Streamline's CLIENT config ({@code streamline-client.toml}). Every option is surfaced in Sodium's video
 * settings (and Reese's Sodium Options) via {@code dev.streamline.client.SodiumConfigIntegration}.
 *
 * <p>All accessors guard {@link ModConfigSpec#isLoaded()} and fall back to the default, so render code can
 * call them at any time - several hooks (vsync, framerate) run before configs load on the very first frames.
 * Setters persist immediately (Sodium's storage handler is a no-op, repo pattern).</p>
 */
public final class StreamlineConfig {

    public static final ModConfigSpec CLIENT_SPEC;

    private static final String T = "streamline.config.";

    // Defaults shared with the Sodium option builders.
    public static final int UNFOCUSED_FPS_DEFAULT = 15;
    public static final int MINIMIZED_FPS_DEFAULT = 3;
    public static final int IDLE_FPS_DEFAULT = 30;
    public static final int IDLE_DELAY_DEFAULT = 300;
    public static final int AMBIENCE_DENSITY_DEFAULT = 100;

    // --- fps governor ---
    private static final ModConfigSpec.IntValue UNFOCUSED_FPS;
    private static final ModConfigSpec.IntValue MINIMIZED_FPS;
    private static final ModConfigSpec.IntValue IDLE_FPS;
    private static final ModConfigSpec.IntValue IDLE_DELAY_SECONDS;
    private static final ModConfigSpec.BooleanValue PRECISE_LIMITER;
    private static final ModConfigSpec.BooleanValue ADAPTIVE_VSYNC;

    // --- texture animations ---
    private static final ModConfigSpec.BooleanValue ANIMATE_TEXTURES;
    private static final ModConfigSpec.BooleanValue ANIMATE_WATER;
    private static final ModConfigSpec.BooleanValue ANIMATE_LAVA;
    private static final ModConfigSpec.BooleanValue ANIMATE_FIRE;
    private static final ModConfigSpec.BooleanValue ANIMATE_PORTAL;
    private static final ModConfigSpec.BooleanValue ANIMATE_OTHER;

    // --- world details ---
    private static final ModConfigSpec.IntValue AMBIENCE_DENSITY;
    private static final ModConfigSpec.BooleanValue STEADY_LIGHTMAP;
    private static final ModConfigSpec.BooleanValue VIGNETTE;
    private static final ModConfigSpec.BooleanValue ENCHANTMENT_GLINT;
    private static final ModConfigSpec.BooleanValue PARTICLE_FAST_PATH;
    private static final ModConfigSpec.BooleanValue HUD_BATCHING;
    private static final ModConfigSpec.BooleanValue FAST_DEBUG_HUD;

    // --- diagnostics ---
    private static final ModConfigSpec.BooleanValue FPS_METER;
    private static final ModConfigSpec.BooleanValue DEBUG_OVERLAY;

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("FPS governor: cap the frame rate while the window is unfocused, minimized, or the player",
                  "is idle (no mouse/keyboard input and no held movement keys). Rendering frames nobody is",
                  "looking at is pure GPU heat. Caps never apply while resources are (re)loading, and any",
                  "input lifts the idle cap on the very next frame. 0 disables a cap.")
            .translation(T + "section.fps")
            .push("fps");
        UNFOCUSED_FPS = b
            .comment("FPS while the window is open but not focused (alt-tabbed to another window).")
            .translation(T + "fps.unfocused")
            .defineInRange("unfocused", UNFOCUSED_FPS_DEFAULT, 0, 120);
        MINIMIZED_FPS = b
            .comment("FPS while the window is minimized/hidden. Kept above 1 so the client still ticks",
                     "comfortably (packets keep draining; multiplayer stays in sync).")
            .translation(T + "fps.minimized")
            .defineInRange("minimized", MINIMIZED_FPS_DEFAULT, 0, 30);
        IDLE_FPS = b
            .comment("FPS after the idle delay passes with no input while focused. Any key, click, scroll,",
                     "mouse move or held movement key resets it instantly.")
            .translation(T + "fps.idle")
            .defineInRange("idle", IDLE_FPS_DEFAULT, 0, 120);
        IDLE_DELAY_SECONDS = b
            .comment("Seconds without input before the idle FPS cap engages.")
            .translation(T + "fps.idleDelaySeconds")
            .defineInRange("idleDelaySeconds", IDLE_DELAY_DEFAULT, 60, 900);
        PRECISE_LIMITER = b
            .comment("Replace vanilla's coarse FPS limiter with a wait-then-spin pacer for visibly steadier",
                     "frame times whenever a cap is active (vanilla's, or this governor's). Costs a sliver",
                     "of one CPU core right before each frame.")
            .translation(T + "fps.preciseLimiter")
            .define("preciseLimiter", false);
        ADAPTIVE_VSYNC = b
            .comment("With VSync on, allow tearing instead of stalling when a frame misses the refresh",
                     "window (GLFW swap interval -1). Needs driver support (WGL/GLX_EXT_swap_control_tear);",
                     "falls back to normal VSync when unsupported.")
            .translation(T + "fps.adaptiveVsync")
            .define("adaptiveVsync", false);
        b.pop();

        b.comment("Texture animation control: stop ticking + re-uploading animated atlas sprites by category.",
                  "Every animated sprite uploads new pixels to the GPU each animation frame; Sodium's",
                  "'animate only visible textures' already skips off-screen ones - these switches also stop",
                  "the ones you CAN see, freezing them at their current frame (pure look tradeoff).")
            .translation(T + "section.animations")
            .push("animations");
        ANIMATE_TEXTURES = b
            .comment("Master switch: OFF freezes every animated texture regardless of the categories below.")
            .translation(T + "animations.master")
            .define("master", true);
        ANIMATE_WATER = b.translation(T + "animations.water").define("water", true);
        ANIMATE_LAVA = b.translation(T + "animations.lava").define("lava", true);
        ANIMATE_FIRE = b.translation(T + "animations.fire").define("fire", true);
        ANIMATE_PORTAL = b.translation(T + "animations.portal").define("portal", true);
        ANIMATE_OTHER = b
            .comment("Everything else that animates: magma, sea lanterns, prismarine, kelp, stonecutter",
                     "saws, command blocks, modded sprites...")
            .translation(T + "animations.other")
            .define("other", true);
        b.pop();

        b.comment("World detail costs that are per-frame or per-tick busywork.")
            .translation(T + "section.details")
            .push("details");
        AMBIENCE_DENSITY = b
            .comment("Density of ambient block effects (torch flames, water drips, fire crackle sounds...).",
                     "Vanilla samples 667 random positions around you every tick, twice, and asks each block",
                     "to emit its ambience; this scales that sample count. 100 = vanilla.")
            .translation(T + "details.ambienceDensity")
            .defineInRange("ambienceDensity", AMBIENCE_DENSITY_DEFAULT, 10, 100);
        STEADY_LIGHTMAP = b
            .comment("Suppress the random block-light flicker and skip rebuilding + re-uploading the 16x16",
                     "lightmap texture when nothing about it changed (vanilla rebuilds it every tick just",
                     "for the flicker). Night vision, darkness, gamma, storms, dimension effects all still",
                     "update it the moment they change; dimensions with custom lightmap hooks are left on",
                     "the vanilla path. Torchlight simply stops wavering.")
            .translation(T + "details.steadyLightmap")
            .define("steadyLightmap", true);
        VIGNETTE = b
            .comment("Render the vignette (the subtle darkened screen border) - one fullscreen translucent",
                     "quad per frame. OFF skips it.")
            .translation(T + "details.vignette")
            .define("vignette", true);
        ENCHANTMENT_GLINT = b
            .comment("Render the enchantment glint. Every enchanted item/armor piece draws its geometry a",
                     "second time for the shimmer - OFF skips that pass everywhere (world, hands, GUIs).")
            .translation(T + "details.enchantmentGlint")
            .define("enchantmentGlint", true);
        PARTICLE_FAST_PATH = b
            .comment("Skip the expensive block-collision scan for particles surrounded by air (rain, splashes,",
                     "smoke - thousands per tick in storms). The fast pre-scan mirrors vanilla's collision",
                     "rules exactly, so outcomes are bit-identical; any particle near solid blocks takes the",
                     "full vanilla path.")
            .translation(T + "details.particleFastPath")
            .define("particleFastPath", true);
        HUD_BATCHING = b
            .comment("Batch the draw calls of VANILLA HUD layers (hearts, hunger, hotbar text, chat,",
                     "scoreboard...). Vanilla flushes the GUI buffer after every single element; batching per",
                     "layer collapses dozens of flushes into a few. Modded HUD layers render exactly as",
                     "before (only minecraft-namespaced layers are batched).")
            .translation(T + "details.hudBatching")
            .define("hudBatching", true);
        FAST_DEBUG_HUD = b
            .comment("Rebuild the F3 debug text at most every 100 ms instead of every frame. The two info",
                     "columns are rebuilt with heavy string formatting per frame in vanilla - at high FPS",
                     "that is real time and constant allocation churn. Values on screen refresh 10x/second.")
            .translation(T + "details.fastDebugHud")
            .define("fastDebugHud", true);
        b.pop();

        b.comment("Diagnostics.")
            .translation(T + "section.debug")
            .push("debug");
        FPS_METER = b
            .comment("Small top-right readout: FPS, average frame time, 1% low, and the governor cap",
                     "currently applied (if any).")
            .translation(T + "debug.fpsMeter")
            .define("fpsMeter", false);
        DEBUG_OVERLAY = b
            .comment("Top-left diagnostic block (below Fovea's and Sightline's slots): governor state,",
                     "pacer/vsync status, per-tick skip counters, and any hooks that failed to arm.")
            .translation(T + "debug.overlay")
            .define("overlay", false);
        b.pop();

        CLIENT_SPEC = b.build();
    }

    private static boolean get(final ModConfigSpec.BooleanValue v, final boolean dflt) {
        return CLIENT_SPEC.isLoaded() ? v.get() : dflt;
    }

    private static int get(final ModConfigSpec.IntValue v, final int dflt) {
        return CLIENT_SPEC.isLoaded() ? v.get() : dflt;
    }

    // --- fps governor ---
    public static int unfocusedFps()          { return get(UNFOCUSED_FPS, UNFOCUSED_FPS_DEFAULT); }
    public static int minimizedFps()          { return get(MINIMIZED_FPS, MINIMIZED_FPS_DEFAULT); }
    public static int idleFps()               { return get(IDLE_FPS, IDLE_FPS_DEFAULT); }
    public static int idleDelaySeconds()      { return get(IDLE_DELAY_SECONDS, IDLE_DELAY_DEFAULT); }
    public static boolean preciseLimiter()    { return get(PRECISE_LIMITER, false); }
    public static boolean adaptiveVsync()     { return get(ADAPTIVE_VSYNC, false); }

    // --- texture animations ---
    public static boolean animateTextures()   { return get(ANIMATE_TEXTURES, true); }
    public static boolean animateWater()      { return get(ANIMATE_WATER, true); }
    public static boolean animateLava()       { return get(ANIMATE_LAVA, true); }
    public static boolean animateFire()       { return get(ANIMATE_FIRE, true); }
    public static boolean animatePortal()     { return get(ANIMATE_PORTAL, true); }
    public static boolean animateOther()      { return get(ANIMATE_OTHER, true); }

    // --- world details ---
    public static int ambienceDensity()       { return get(AMBIENCE_DENSITY, AMBIENCE_DENSITY_DEFAULT); }
    public static boolean steadyLightmap()    { return get(STEADY_LIGHTMAP, true); }
    public static boolean vignette()          { return get(VIGNETTE, true); }
    public static boolean enchantmentGlint()  { return get(ENCHANTMENT_GLINT, true); }
    public static boolean particleFastPath()  { return get(PARTICLE_FAST_PATH, true); }
    public static boolean hudBatching()       { return get(HUD_BATCHING, true); }
    public static boolean fastDebugHud()      { return get(FAST_DEBUG_HUD, true); }

    // --- diagnostics ---
    public static boolean fpsMeter()          { return get(FPS_METER, false); }
    public static boolean debugOverlay()      { return get(DEBUG_OVERLAY, false); }

    // --- setters (Sodium option bindings; ConfigValue.set persists) ---
    public static void setUnfocusedFps(final int v)         { UNFOCUSED_FPS.set(v); }
    public static void setMinimizedFps(final int v)         { MINIMIZED_FPS.set(v); }
    public static void setIdleFps(final int v)              { IDLE_FPS.set(v); }
    public static void setIdleDelaySeconds(final int v)     { IDLE_DELAY_SECONDS.set(v); }
    public static void setPreciseLimiter(final boolean v)   { PRECISE_LIMITER.set(v); }
    public static void setAdaptiveVsync(final boolean v)    { ADAPTIVE_VSYNC.set(v); }
    public static void setAnimateTextures(final boolean v)  { ANIMATE_TEXTURES.set(v); }
    public static void setAnimateWater(final boolean v)     { ANIMATE_WATER.set(v); }
    public static void setAnimateLava(final boolean v)      { ANIMATE_LAVA.set(v); }
    public static void setAnimateFire(final boolean v)      { ANIMATE_FIRE.set(v); }
    public static void setAnimatePortal(final boolean v)    { ANIMATE_PORTAL.set(v); }
    public static void setAnimateOther(final boolean v)     { ANIMATE_OTHER.set(v); }
    public static void setAmbienceDensity(final int v)      { AMBIENCE_DENSITY.set(v); }
    public static void setSteadyLightmap(final boolean v)   { STEADY_LIGHTMAP.set(v); }
    public static void setVignette(final boolean v)         { VIGNETTE.set(v); }
    public static void setEnchantmentGlint(final boolean v) { ENCHANTMENT_GLINT.set(v); }
    public static void setParticleFastPath(final boolean v) { PARTICLE_FAST_PATH.set(v); }
    public static void setHudBatching(final boolean v)      { HUD_BATCHING.set(v); }
    public static void setFastDebugHud(final boolean v)     { FAST_DEBUG_HUD.set(v); }
    public static void setFpsMeter(final boolean v)         { FPS_METER.set(v); }
    public static void setDebugOverlay(final boolean v)     { DEBUG_OVERLAY.set(v); }

    private StreamlineConfig() {}
}
