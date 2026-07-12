package dev.streamline.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.frame.FpsGovernor;
import dev.streamline.frame.FrameStats;

/**
 * Compact top-right FPS meter (config {@code debug.fpsMeter}): FPS, average frame time, 1% low, and -
 * when the governor is limiting - the active cap and why. Top-right on purpose: Fovea's and Sightline's
 * diagnostic blocks own the top-left.
 */
public final class FpsMeterOverlay {

    public static void onRenderGui(final RenderGuiEvent.Post event) {
        if (!StreamlineConfig.fpsMeter())
            return;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getDebugOverlay().showDebugScreen())
            return;

        final StringBuilder line = new StringBuilder(48);
        line.append(mc.getFps()).append(" fps");
        final double avgMs = FrameStats.averageMs();
        if (avgMs > 0)
            line.append(String.format(" | %.1f ms | 1%% low %.0f", avgMs, FrameStats.onePercentLowFps()));
        final int cap = FpsGovernor.appliedCapFps();
        if (cap > 0)
            line.append(" | cap ").append(cap)
                .append(" (").append(FpsGovernor.currentMode().name().toLowerCase(Locale.ROOT)).append(')');

        final GuiGraphics g = event.getGuiGraphics();
        final String text = line.toString();
        g.drawString(mc.font, text, g.guiWidth() - mc.font.width(text) - 4, 4, 0xE0E0E0);
    }

    private FpsMeterOverlay() {}
}
