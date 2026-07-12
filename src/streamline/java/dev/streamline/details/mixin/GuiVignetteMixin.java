package dev.streamline.details.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;

/**
 * Vignette toggle: one fullscreen translucent blend per frame. Precise cancel of just this overlay
 * (cancelling NeoForge's whole CAMERA_OVERLAYS layer would also drop the pumpkin/powder-snow views).
 */
@Mixin(Gui.class)
public abstract class GuiVignetteMixin {

    @Inject(
        method = "renderVignette(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/Entity;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void streamline$skipVignette(final GuiGraphics guiGraphics, final Entity entity, final CallbackInfo ci) {
        StreamlineState.armedVignette = true;
        if (!StreamlineConfig.vignette())
            ci.cancel();
    }
}
