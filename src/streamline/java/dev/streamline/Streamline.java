package dev.streamline;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

import dev.streamline.core.StreamlineConfig;

/**
 * <b>Streamline</b> &mdash; frame delivery performance for Sodium/Iris packs, Sightline's companion.
 * Where Sightline decides <i>what</i> is worth rendering, Streamline decides <i>when and how hard</i>
 * to render at all:
 *
 * <ul>
 *   <li><b>FPS governor</b> &mdash; frame caps for unfocused, minimized and idle (no input) window states;
 *       frames nobody watches stop burning the GPU.</li>
 *   <li><b>Precise frame limiter</b> &mdash; wait-then-spin pacing for visibly steadier capped frame times.</li>
 *   <li><b>Adaptive VSync</b> &mdash; tear instead of stall when a frame misses the refresh window.</li>
 *   <li><b>Texture animation control</b> &mdash; freeze animated sprites per category (water, lava, fire,
 *       portal, other); each animated sprite re-uploads pixels to the GPU every animation frame.</li>
 *   <li><b>Steady lightmap</b> &mdash; skip the per-tick lightmap rebuild + upload vanilla performs just to
 *       animate torch flicker.</li>
 *   <li><b>Ambience density</b> &mdash; scale the 667-samples-per-tick block ambience loop.</li>
 *   <li><b>Detail toggles</b> &mdash; enchantment glint pass, vignette.</li>
 * </ul>
 *
 * <p>Client-only. All options live in Sodium's video settings (Reese's Sodium Options compatible); every
 * hook is vanilla-targeted and composes with Sodium's, Iris's, Fovea's and Sightline's mixins (no shared
 * redirect call sites; texture gates cancel at HEAD alongside Sodium's visibility tracking).</p>
 */
@Mod(Streamline.MOD_ID)
public final class Streamline {

    public static final String MOD_ID = "streamline";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Streamline(final IEventBus modBus, final ModContainer container) {
        if (FMLEnvironment.dist == Dist.CLIENT)
            container.registerConfig(ModConfig.Type.CLIENT, StreamlineConfig.CLIENT_SPEC);
        LOGGER.info("[Streamline] loaded.");
    }
}
