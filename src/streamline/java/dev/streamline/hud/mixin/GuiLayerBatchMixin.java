package dev.streamline.hud.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;

/**
 * HUD draw batching: {@code GuiGraphics} flushes its buffer after EVERY unmanaged element - each heart,
 * each line of chat, each scoreboard row becomes its own draw flush. Wrapping a layer's render in
 * {@code drawManaged} defers those flushes to one per layer, exactly the batching vanilla uses for
 * tooltips. Scoped tight for compatibility: only layers in the {@code minecraft} namespace are batched
 * (their draw code ships with this game version and is verified batch-safe - blits draw immediately
 * regardless, item renders self-flush, deferred fills/text keep insertion order); modded layers and the
 * {@code RenderGuiLayerEvent} hooks around each layer render exactly as before. Wrapping the call inside
 * {@code renderInner} (not the layer object) keeps those events outside the managed scope.
 */
@Mixin(GuiLayerManager.class)
public abstract class GuiLayerBatchMixin {

    @WrapOperation(
        method = "renderInner",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/LayeredDraw$Layer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    @SuppressWarnings("deprecation")
    private void streamline$batchVanillaLayer(final LayeredDraw.Layer layer, final GuiGraphics guiGraphics,
                                              final DeltaTracker deltaTracker, final Operation<Void> original,
                                              @Local final GuiLayerManager.NamedLayer namedLayer) {
        StreamlineState.armedHudBatching = true;
        if (StreamlineConfig.hudBatching() && "minecraft".equals(namedLayer.name().getNamespace())) {
            guiGraphics.drawManaged(() -> original.call(layer, guiGraphics, deltaTracker));
        } else {
            original.call(layer, guiGraphics, deltaTracker);
        }
    }
}
