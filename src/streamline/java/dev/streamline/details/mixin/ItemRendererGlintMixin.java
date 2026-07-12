package dev.streamline.details.mixin;

import net.minecraft.client.renderer.entity.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;

/**
 * Enchantment glint toggle: every foil-wrapped buffer request goes through these three statics, whose
 * boolean decides between "plain buffer" and "glint pass + plain buffer" (each glinted item is drawn
 * twice). Forcing the flag false at HEAD picks the plain path with vanilla's own logic - no render-type
 * juggling, and mods calling these helpers are covered too. The compass/clock swirl is a different
 * method ({@code getCompassFoilBuffer}) and deliberately untouched: that shimmer is the item's face,
 * not a decoration.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererGlintMixin {

    @ModifyVariable(
        method = "getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true)
    private static boolean streamline$stripGlint(final boolean glint) {
        return streamline$filter(glint);
    }

    @ModifyVariable(
        method = "getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true)
    private static boolean streamline$stripGlintDirect(final boolean withGlint) {
        return streamline$filter(withGlint);
    }

    @ModifyVariable(
        method = "getArmorFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Z)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
        at = @At("HEAD"),
        ordinal = 0,
        argsOnly = true)
    private static boolean streamline$stripArmorGlint(final boolean hasFoil) {
        return streamline$filter(hasFoil);
    }

    private static boolean streamline$filter(final boolean requested) {
        StreamlineState.armedGlint = true;
        if (requested && !StreamlineConfig.enchantmentGlint()) {
            StreamlineState.glintPassesSkipped++;
            return false;
        }
        return requested;
    }
}
