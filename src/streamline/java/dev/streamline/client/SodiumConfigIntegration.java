package dev.streamline.client;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.BooleanOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.OptionPageBuilder;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.frame.AdaptiveVsync;

/**
 * Surfaces Streamline's options inside Sodium's video settings - and therefore Reese's Sodium Options -
 * as two pages (Frame Pacing / Details), with the mod's icon on its entry.
 *
 * <p><b>Self-gating:</b> referenced nowhere in our own code; Sodium's config scanner finds it via the
 * {@link ConfigEntryPointForge} annotation, so nothing loads when Sodium is absent. Every option binds
 * straight to a {@link StreamlineConfig} setter/getter pair - edits apply immediately (all hooks re-read
 * config live) and persist; the storage handler is therefore a no-op, mirroring the other mods in this
 * repo. The adaptive-vsync binding additionally re-pushes the vsync state so a toggle takes effect
 * without reopening the options.</p>
 */
@ConfigEntryPointForge("streamline")
public final class SodiumConfigIntegration implements ConfigEntryPoint {

    private static Component fps(final int value) {
        return Component.literal(value + " FPS");
    }

    private static Component fpsOr(final int value, final String zeroLabel) {
        return value == 0 ? Component.literal(zeroLabel) : fps(value);
    }

    private static Component seconds(final int value) {
        return value % 60 == 0
            ? Component.literal((value / 60) + " min")
            : Component.literal(value + " s");
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath("streamline", path);
    }

    @Override
    public void registerConfigLate(final ConfigBuilder builder) {

        // ============================ page: Frame Pacing ============================
        final IntegerOptionBuilder unfocused = builder
            .createIntegerOption(id("unfocused_fps"))
            .setName(Component.literal("Unfocused FPS"))
            .setTooltip(Component.literal(
                "Frame cap while the window is open but not focused (working in another window). "
                    + "Rendering frames nobody watches is pure GPU heat. Focus lifts the cap on the "
                    + "very next frame; loading screens are never throttled."))
            .setRange(0, 120, 5)
            .setDefaultValue(StreamlineConfig.UNFOCUSED_FPS_DEFAULT)
            .setImpact(OptionImpact.HIGH)
            .setValueFormatter(v -> fpsOr(v, "No limit"))
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setUnfocusedFps, StreamlineConfig::unfocusedFps);

        final IntegerOptionBuilder minimized = builder
            .createIntegerOption(id("minimized_fps"))
            .setName(Component.literal("Minimized FPS"))
            .setTooltip(Component.literal(
                "Frame cap while the window is minimized or hidden - nothing is visible at all. A few "
                    + "FPS keep the client ticking comfortably (packets drain fully every tick, so "
                    + "multiplayer stays perfectly in sync)."))
            .setRange(0, 30, 1)
            .setDefaultValue(StreamlineConfig.MINIMIZED_FPS_DEFAULT)
            .setImpact(OptionImpact.HIGH)
            .setValueFormatter(v -> fpsOr(v, "No limit"))
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setMinimizedFps, StreamlineConfig::minimizedFps);

        final IntegerOptionBuilder idle = builder
            .createIntegerOption(id("idle_fps"))
            .setName(Component.literal("Idle FPS"))
            .setTooltip(Component.literal(
                "Frame cap after the idle delay passes with no input while focused (stepped away from "
                    + "the keyboard). Any key, click, scroll, mouse movement or held movement key counts "
                    + "as activity and lifts the cap instantly."))
            .setRange(0, 120, 5)
            .setDefaultValue(StreamlineConfig.IDLE_FPS_DEFAULT)
            .setImpact(OptionImpact.HIGH)
            .setValueFormatter(v -> fpsOr(v, "Off"))
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setIdleFps, StreamlineConfig::idleFps);

        final IntegerOptionBuilder idleDelay = builder
            .createIntegerOption(id("idle_delay"))
            .setName(Component.literal("Idle Delay"))
            .setTooltip(Component.literal(
                "How long without input before the idle FPS cap engages."))
            .setRange(60, 900, 30)
            .setDefaultValue(StreamlineConfig.IDLE_DELAY_DEFAULT)
            .setImpact(OptionImpact.LOW)
            .setValueFormatter(SodiumConfigIntegration::seconds)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setIdleDelaySeconds, StreamlineConfig::idleDelaySeconds);

        final BooleanOptionBuilder precise = builder
            .createBooleanOption(id("precise_limiter"))
            .setName(Component.literal("Precise Frame Limiter"))
            .setTooltip(Component.literal(
                "Replace vanilla's coarse FPS limiter with wait-then-spin pacing whenever a frame cap is "
                    + "active (the FPS Limit slider, menus, or this governor). Vanilla's wait routinely "
                    + "overshoots by a millisecond+, so capped gameplay has visible cadence wobble; this "
                    + "lands frames within microseconds at the cost of a sliver of one CPU core."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(false)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setPreciseLimiter, StreamlineConfig::preciseLimiter);

        final BooleanOptionBuilder adaptive = builder
            .createBooleanOption(id("adaptive_vsync"))
            .setName(Component.literal("Adaptive VSync"))
            .setTooltip(Component.literal(
                "With VSync on, tear instead of stalling a whole refresh when a frame runs late - the "
                    + "classic 'VSync halves your FPS the moment you dip below 60' fix. Needs driver "
                    + "support (swap_control_tear); silently keeps normal VSync when unsupported. Does "
                    + "nothing while VSync is off."))
            .setImpact(OptionImpact.MEDIUM)
            .setDefaultValue(false)
            .setStorageHandler(() -> {})
            .setBinding(v -> {
                StreamlineConfig.setAdaptiveVsync(v);
                AdaptiveVsync.reapply();
            }, StreamlineConfig::adaptiveVsync);

        // ============================ page: Details ============================
        final BooleanOptionBuilder animMaster = builder
            .createBooleanOption(id("animate_master"))
            .setName(Component.literal("Animate Textures"))
            .setTooltip(Component.literal(
                "Master switch for texture animations. Every animated sprite re-uploads pixels to the "
                    + "GPU each animation frame; OFF freezes them all at their current frame regardless "
                    + "of the category switches below. Sodium already skips off-screen sprites - this "
                    + "also stops the visible ones (pure look tradeoff, zero gameplay impact)."))
            .setImpact(OptionImpact.MEDIUM)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAnimateTextures, StreamlineConfig::animateTextures);

        final BooleanOptionBuilder animWater = builder
            .createBooleanOption(id("animate_water"))
            .setName(Component.literal("Water Animation"))
            .setTooltip(Component.literal("Animate water surfaces (oceans are a lot of animated area)."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAnimateWater, StreamlineConfig::animateWater);

        final BooleanOptionBuilder animLava = builder
            .createBooleanOption(id("animate_lava"))
            .setName(Component.literal("Lava Animation"))
            .setTooltip(Component.literal("Animate lava (the Nether is wall-to-wall animated texture)."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAnimateLava, StreamlineConfig::animateLava);

        final BooleanOptionBuilder animFire = builder
            .createBooleanOption(id("animate_fire"))
            .setName(Component.literal("Fire Animation"))
            .setTooltip(Component.literal("Animate fire, soul fire and campfires."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAnimateFire, StreamlineConfig::animateFire);

        final BooleanOptionBuilder animPortal = builder
            .createBooleanOption(id("animate_portal"))
            .setName(Component.literal("Portal Animation"))
            .setTooltip(Component.literal("Animate the nether portal swirl."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAnimatePortal, StreamlineConfig::animatePortal);

        final BooleanOptionBuilder animOther = builder
            .createBooleanOption(id("animate_other"))
            .setName(Component.literal("Other Animations"))
            .setTooltip(Component.literal(
                "Everything else that animates: magma, sea lanterns, prismarine, kelp, stonecutter saws, "
                    + "command blocks, modded sprites..."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAnimateOther, StreamlineConfig::animateOther);

        final IntegerOptionBuilder ambience = builder
            .createIntegerOption(id("ambience_density"))
            .setName(Component.literal("Block Ambience Density"))
            .setTooltip(Component.literal(
                "Density of ambient block effects: torch flames, water drips, fire crackles, portal "
                    + "whispers. Vanilla samples 667 random positions around you every tick, twice, and "
                    + "asks each block to emit its ambience - this scales that whole loop. Lower = fewer "
                    + "ambient particles/sounds and less tick time."))
            .setRange(10, 100, 10)
            .setDefaultValue(StreamlineConfig.AMBIENCE_DENSITY_DEFAULT)
            .setImpact(OptionImpact.LOW)
            .setValueFormatter(v -> Component.literal(v + "%"))
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setAmbienceDensity, StreamlineConfig::ambienceDensity);

        final BooleanOptionBuilder steadyLightmap = builder
            .createBooleanOption(id("steady_lightmap"))
            .setName(Component.literal("Steady Lightmap"))
            .setTooltip(Component.literal(
                "Vanilla rebuilds and re-uploads the lightmap texture every tick just to animate the "
                    + "random torch flicker. ON pins the flicker at its average (torchlight simply stops "
                    + "wavering) and skips rebuilds whose inputs did not change. Night vision, darkness, "
                    + "gamma, storms and dimension effects still update instantly; dimensions with custom "
                    + "lightmap hooks stay on the vanilla path."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setSteadyLightmap, StreamlineConfig::steadyLightmap);

        final BooleanOptionBuilder vignette = builder
            .createBooleanOption(id("vignette"))
            .setName(Component.literal("Vignette"))
            .setTooltip(Component.literal(
                "The subtle darkened screen border - one fullscreen translucent blend per frame. OFF "
                    + "skips it (also brightens dark corners slightly, which some players prefer anyway)."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setVignette, StreamlineConfig::vignette);

        final BooleanOptionBuilder particleFastPath = builder
            .createBooleanOption(id("particle_fast_path"))
            .setName(Component.literal("Particle Physics Fast Path"))
            .setTooltip(Component.literal(
                "Particles surrounded by air skip vanilla's allocation-heavy block-collision machinery. "
                    + "The pre-scan mirrors vanilla's collision rules exactly, so outcomes are identical - "
                    + "particles near solid blocks take the full vanilla path. A big deal in storms: "
                    + "thousands of rain drops and splashes run collision every tick."))
            .setImpact(OptionImpact.HIGH)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setParticleFastPath, StreamlineConfig::particleFastPath);

        final BooleanOptionBuilder hudBatching = builder
            .createBooleanOption(id("hud_batching"))
            .setName(Component.literal("HUD Draw Batching"))
            .setTooltip(Component.literal(
                "Vanilla flushes the GUI buffer after every single HUD element - each heart, each chat "
                    + "line, each scoreboard row is its own draw flush. This batches each VANILLA HUD "
                    + "layer into one flush (the same mechanism vanilla uses for tooltips). Modded HUD "
                    + "layers are untouched and render exactly as before."))
            .setImpact(OptionImpact.MEDIUM)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setHudBatching, StreamlineConfig::hudBatching);

        final BooleanOptionBuilder fastDebugHud = builder
            .createBooleanOption(id("fast_debug_hud"))
            .setName(Component.literal("Fast F3 Debug HUD"))
            .setTooltip(Component.literal(
                "Rebuild the F3 text columns at most 10x/second instead of every frame. Vanilla runs "
                    + "dozens of String.formats per frame while F3 is open - noticeable FPS cost and "
                    + "constant allocation churn. Values on screen still refresh 10x/second."))
            .setImpact(OptionImpact.MEDIUM)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setFastDebugHud, StreamlineConfig::fastDebugHud);

        final BooleanOptionBuilder glint = builder
            .createBooleanOption(id("enchantment_glint"))
            .setName(Component.literal("Enchantment Glint"))
            .setTooltip(Component.literal(
                "Every enchanted item and armor piece is drawn a second time for the shimmer pass - in "
                    + "crowds of armored mobs/players that is real frame time. OFF skips the glint pass "
                    + "everywhere (world, hands, GUIs); enchanted gear keeps its tooltip and name color."))
            .setImpact(OptionImpact.MEDIUM)
            .setDefaultValue(true)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setEnchantmentGlint, StreamlineConfig::enchantmentGlint);

        final BooleanOptionBuilder fpsMeter = builder
            .createBooleanOption(id("fps_meter"))
            .setName(Component.literal("FPS Meter"))
            .setTooltip(Component.literal(
                "Small top-right readout: FPS, average frame time, 1% low, and the governor cap "
                    + "currently applied (if any)."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(false)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setFpsMeter, StreamlineConfig::fpsMeter);

        final BooleanOptionBuilder overlay = builder
            .createBooleanOption(id("debug_overlay"))
            .setName(Component.literal("Debug Overlay"))
            .setTooltip(Component.literal(
                "Top-left diagnostic block (below Fovea's and Sightline's slots): governor state, pacer "
                    + "and vsync status, per-tick skip counters, and any hooks that failed to arm."))
            .setImpact(OptionImpact.LOW)
            .setDefaultValue(false)
            .setStorageHandler(() -> {})
            .setBinding(StreamlineConfig::setDebugOverlay, StreamlineConfig::debugOverlay);

        // ===================== pages =====================
        final OptionPageBuilder pacingPage = builder.createOptionPage()
            .setName(Component.literal("Frame Pacing"))
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.literal("FPS Governor"))
                .addOption(unfocused)
                .addOption(minimized)
                .addOption(idle)
                .addOption(idleDelay))
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.literal("Pacing & VSync"))
                .addOption(precise)
                .addOption(adaptive));

        final OptionPageBuilder detailsPage = builder.createOptionPage()
            .setName(Component.literal("Details"))
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.literal("Texture Animations"))
                .addOption(animMaster)
                .addOption(animWater)
                .addOption(animLava)
                .addOption(animFire)
                .addOption(animPortal)
                .addOption(animOther))
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.literal("World Ambience"))
                .addOption(ambience)
                .addOption(steadyLightmap)
                .addOption(particleFastPath))
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.literal("HUD & Items"))
                .addOption(hudBatching)
                .addOption(fastDebugHud)
                .addOption(vignette)
                .addOption(glint))
            .addOptionGroup(builder.createOptionGroup()
                .setName(Component.literal("Diagnostics"))
                .addOption(fpsMeter)
                .addOption(overlay));

        builder.registerOwnModOptions()
            .setName("Streamline")
            .setNonTintedIcon(ResourceLocation.fromNamespaceAndPath("streamline", "textures/gui/mod_icon.png"))
            .addPage(pacingPage)
            .addPage(detailsPage);
    }
}
