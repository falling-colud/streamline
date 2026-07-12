package dev.streamline.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;
import dev.streamline.details.LightmapMemo;
import dev.streamline.frame.AdaptiveVsync;
import dev.streamline.frame.FpsGovernor;

/**
 * Top-left diagnostic overlay (config {@code debug.overlay}): governor state, pacer/vsync status,
 * per-tick skip counters, and - because every injector runs {@code require 0} - the list of any hooks
 * that never armed, so a silent mixin mismatch is visible instead of mysterious. Drawn below Fovea's
 * (y=4) and Sightline's (y=84) overlay slots so all three can be read together.
 */
public final class DebugOverlay {

    public static void onRenderGui(final RenderGuiEvent.Post event) {
        if (!StreamlineConfig.debugOverlay())
            return;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getDebugOverlay().showDebugScreen())
            return;

        final List<String> lines = new ArrayList<>(6);
        lines.add("[Streamline]");
        final int cap = FpsGovernor.appliedCapFps();
        lines.add(cap > 0
            ? String.format("governor: cap %d fps (%s)", cap,
                FpsGovernor.currentMode().name().toLowerCase(Locale.ROOT))
            : "governor: uncapped");
        lines.add(String.format("pacer %s | adaptive vsync %s",
            StreamlineConfig.preciseLimiter() ? "ON" : "off",
            !StreamlineConfig.adaptiveVsync() ? "off"
                : AdaptiveVsync.tearSupported() ? "ON" : "UNSUPPORTED by driver"));
        lines.add(String.format("sprites frozen %d | glint passes skipped %d  (per tick)",
            StreamlineState.lastFrozenSpriteUploads, StreamlineState.lastGlintPassesSkipped));
        lines.add(LightmapMemo.overlayLine());
        lines.add(String.format("particle collisions  fast %d | full %d  (per tick)",
            StreamlineState.lastParticleCollisionsFast, StreamlineState.lastParticleCollisionsFull));

        final String unarmed = unarmedHooks();
        if (!unarmed.isEmpty())
            lines.add("hooks NOT armed: " + unarmed);

        final GuiGraphics g = event.getGuiGraphics();
        // Below Fovea's and Sightline's overlay slots.
        int y = 150;
        for (final String line : lines) {
            g.drawString(mc.font, line, 4, y, 0xE0E0E0);
            y += 10;
        }
    }

    private static String unarmedHooks() {
        final StringBuilder sb = new StringBuilder();
        append(sb, !StreamlineState.armedFramerateHook, "framerate");
        append(sb, !StreamlineState.armedFrameLimiter, "frameLimiter(needs a cap below 260)");
        append(sb, !StreamlineState.armedActivityMouse, "mouseActivity(needs a mouse event)");
        append(sb, !StreamlineState.armedActivityKeyboard, "keyboardActivity(needs a key press)");
        append(sb, !StreamlineState.armedVsyncHook, "vsync");
        append(sb, !StreamlineState.armedTickerCapture, "tickerCapture");
        append(sb, !StreamlineState.armedTickerGate, "tickerGate");
        append(sb, !StreamlineState.armedAmbience, "ambience");
        append(sb, !StreamlineState.armedLightmap, "lightmap");
        append(sb, !StreamlineState.armedVignette, "vignette");
        append(sb, !StreamlineState.armedGlint, "glint(needs an item on screen)");
        append(sb, !StreamlineState.armedParticlePhysics, "particlePhysics(needs a physics particle)");
        append(sb, !StreamlineState.armedHudBatching, "hudBatching");
        append(sb, !StreamlineState.armedDebugHudCache, "debugHudCache(needs F3 opened once)");
        return sb.toString();
    }

    private static void append(final StringBuilder sb, final boolean missing, final String name) {
        if (missing) {
            if (sb.length() > 0)
                sb.append(", ");
            sb.append(name);
        }
    }

    private DebugOverlay() {}
}
