package dev.streamline.textures;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.resources.ResourceLocation;

import dev.streamline.core.StreamlineConfig;

/**
 * Texture animation control. Sprite tickers carry no name, so each one is classified at creation
 * ({@code SpriteContents.createTicker} hook) by its sprite's path and remembered weakly (atlas reloads
 * discard tickers; the map follows). The tick gate then answers per ticker from live config.
 *
 * <p>Freezing is upload-side only - a gated ticker's {@code tickAndUpload} is cancelled whole, so the
 * frame counter stops and the sprite holds its current frame; re-enabling resumes from there. Unknown
 * tickers (anything registered before the capture hook armed, or exotic modded tickers) fail open.</p>
 */
public final class AnimationControl {

    public enum Category { WATER, LAVA, FIRE, PORTAL, OTHER }

    private static final Map<Object, Category> TICKERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void register(final Object ticker, final ResourceLocation spriteName) {
        TICKERS.put(ticker, classify(spriteName));
    }

    /** @return {@code true} if this ticker may advance + upload this tick. */
    public static boolean shouldTick(final Object ticker) {
        if (!StreamlineConfig.animateTextures())
            return false;
        final Category category = TICKERS.get(ticker);
        if (category == null)
            return true;
        return switch (category) {
            case WATER -> StreamlineConfig.animateWater();
            case LAVA -> StreamlineConfig.animateLava();
            case FIRE -> StreamlineConfig.animateFire();
            case PORTAL -> StreamlineConfig.animatePortal();
            case OTHER -> StreamlineConfig.animateOther();
        };
    }

    /**
     * Path-based (namespace-agnostic, so modded water/lava/fire variants named conventionally land in
     * the matching category; everything else is OTHER).
     */
    static Category classify(final ResourceLocation spriteName) {
        final String path = spriteName.getPath();
        if (path.startsWith("block/water_"))
            return Category.WATER;
        if (path.startsWith("block/lava_"))
            return Category.LAVA;
        if (path.startsWith("block/fire_") || path.startsWith("block/soul_fire_")
            || path.contains("campfire_fire") || path.contains("campfire_log_lit"))
            return Category.FIRE;
        if (path.contains("nether_portal"))
            return Category.PORTAL;
        return Category.OTHER;
    }

    private AnimationControl() {}
}
