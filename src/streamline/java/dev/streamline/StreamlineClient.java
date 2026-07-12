package dev.streamline;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import dev.streamline.client.DebugOverlay;
import dev.streamline.client.FpsMeterOverlay;
import dev.streamline.core.StreamlineState;

/**
 * Client wiring: the per-tick counter roll, the FPS meter and debug overlays, and the fallback config screen
 * (Mods &rarr; Streamline &rarr; Config). The primary options UI is Sodium's video settings - see
 * {@link dev.streamline.client.SodiumConfigIntegration} - which Reese's Sodium Options restyles.
 */
@Mod(value = Streamline.MOD_ID, dist = Dist.CLIENT)
public final class StreamlineClient {

    public StreamlineClient(final IEventBus modBus, final ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(FpsMeterOverlay::onRenderGui);
        NeoForge.EVENT_BUS.addListener(DebugOverlay::onRenderGui);
        NeoForge.EVENT_BUS.addListener(StreamlineClient::onClientTick);
    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        StreamlineState.rollCounters();
    }
}
