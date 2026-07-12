package dev.streamline.details;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.client.extensions.IDimensionSpecialEffectsExtension;
import org.joml.Vector3f;

import dev.streamline.core.StreamlineState;

/**
 * Decides whether the 16x16 lightmap rebuild + GPU upload can be skipped this tick because every input
 * the rebuild reads is bit-identical to the last rebuild. With the flicker suppressed (steady lightmap),
 * a calm scene - constant time of day segment, no expiring effects, no storm flash - skips essentially
 * every rebuild; any input moving (dawn/dusk sky darken, night vision flash-out, darkness pulse, gamma
 * edits, boss-fog world darken...) forces the full vanilla path that tick.
 *
 * <p>The fingerprint mirrors {@link LightTexture#updateLightTexture} input-for-input, including the
 * per-tick-varying terms (darkness pulse cosine, night vision flash), so time-varying states simply never
 * match. Iris-safe for the same reason: Iris only captures the darkness factor inside the rebuild, and
 * whenever darkness is active the fingerprint changes every tick, so the rebuild (and Iris's capture)
 * always runs. Dimensions whose {@code DimensionSpecialEffects} override NeoForge's
 * {@code adjustLightmapColors} hook are never skipped - that hook may depend on inputs we cannot see.</p>
 */
public final class LightmapMemo {

    private static final Map<Class<?>, Boolean> CUSTOM_LIGHTMAP_HOOK = new ConcurrentHashMap<>();

    private static final int FINGERPRINT_FLOATS = 9;
    private static final float[] last = new float[FINGERPRINT_FLOATS];
    private static final float[] current = new float[FINGERPRINT_FLOATS];
    private static boolean valid;
    private static Object lastDimensionType;
    private static boolean lastForceBright;

    /** Called when the feature is off (or state resets) so re-enabling starts fresh. */
    public static void invalidate() {
        valid = false;
    }

    /**
     * @return {@code true} if a rebuild right now would produce the exact pixels of the previous one.
     *         Always stores the current fingerprint, so a {@code false} answer (rebuild happens) primes
     *         the next comparison.
     */
    public static boolean unchanged(final Minecraft mc, final GameRenderer renderer,
                                    final float partialTicks, final float blockLightFlicker) {
        final ClientLevel level = mc.level;
        final LocalPlayer player = mc.player;
        if (level == null || player == null) {
            valid = false;
            return false;
        }
        final DimensionSpecialEffects effects = level.effects();
        if (hasCustomLightmapHook(effects)) {
            valid = false;
            return false;
        }

        // Mirror of the values LightTexture.updateLightTexture reads, in its order.
        final float skyDarken = level.getSkyDarken(1.0F);
        final float skyFlash = level.getSkyFlashTime() > 0 ? 1.0F : 0.0F;
        final float darknessOption = mc.options.darknessEffectScale().get().floatValue();
        final float darknessGamma = darknessGamma(player, partialTicks) * darknessOption;
        final float darknessPulse = darknessPulse(player, darknessGamma, partialTicks) * darknessOption;
        final float waterVision = player.getWaterVision();
        final float nightVisionScale;
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            nightVisionScale = GameRenderer.getNightVisionScale(player, partialTicks);
        } else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
            nightVisionScale = waterVision;
        } else {
            nightVisionScale = 0.0F;
        }
        final float gamma = mc.options.gamma().get().floatValue();
        final float darkenWorld = renderer.getDarkenWorldAmount(partialTicks);

        current[0] = skyDarken;
        current[1] = skyFlash;
        current[2] = darknessGamma;
        current[3] = darknessPulse;
        current[4] = nightVisionScale;
        current[5] = gamma;
        current[6] = darkenWorld;
        current[7] = blockLightFlicker;
        current[8] = darknessOption;

        final boolean same = valid
            && lastDimensionType == level.dimensionType()
            && lastForceBright == effects.forceBrightLightmap()
            && equalsExact(last, current);

        if (!same) {
            System.arraycopy(current, 0, last, 0, FINGERPRINT_FLOATS);
            lastDimensionType = level.dimensionType();
            lastForceBright = effects.forceBrightLightmap();
            valid = true;
        }
        return same;
    }

    private static boolean equalsExact(final float[] a, final float[] b) {
        for (int i = 0; i < FINGERPRINT_FLOATS; i++)
            if (a[i] != b[i])
                return false;
        return true;
    }

    // Replicas of LightTexture's two private helpers (same math, public inputs).
    private static float darknessGamma(final LocalPlayer player, final float partialTicks) {
        final MobEffectInstance effect = player.getEffect(MobEffects.DARKNESS);
        return effect != null ? effect.getBlendFactor(player, partialTicks) : 0.0F;
    }

    private static float darknessPulse(final LocalPlayer player, final float gamma, final float partialTicks) {
        final float f = 0.45F * gamma;
        return Math.max(0.0F, Mth.cos(((float) player.tickCount - partialTicks) * (float) Math.PI * 0.025F) * f);
    }

    /**
     * A dimension-effects class that overrides NeoForge's {@code adjustLightmapColors} may feed inputs we
     * cannot fingerprint (per-class answer cached; reflection failure counts as "custom" = never skip).
     */
    private static boolean hasCustomLightmapHook(final DimensionSpecialEffects effects) {
        return CUSTOM_LIGHTMAP_HOOK.computeIfAbsent(effects.getClass(), cls -> {
            try {
                final Class<?> declarer = cls.getMethod("adjustLightmapColors",
                    ClientLevel.class, float.class, float.class, float.class, float.class,
                    int.class, int.class, Vector3f.class).getDeclaringClass();
                return declarer != IDimensionSpecialEffectsExtension.class
                    && declarer != DimensionSpecialEffects.class;
            } catch (final Throwable t) {
                return true;
            }
        });
    }

    /** Overlay helper: rebuild-vs-skip ratio this session would be noise; expose per-tick counts instead. */
    public static String overlayLine() {
        return String.format("lightmap  rebuilt %d | skipped %d (this tick)",
            StreamlineState.lastLightmapRebuilds, StreamlineState.lastLightmapSkips);
    }

    private LightmapMemo() {}
}
