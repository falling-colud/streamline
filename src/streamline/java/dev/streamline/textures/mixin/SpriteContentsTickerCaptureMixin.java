package dev.streamline.textures.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.streamline.core.StreamlineState;
import dev.streamline.textures.AnimationControl;

/**
 * Ticker classification: {@code SpriteContents$Ticker} is an inner class with no name of its own, so the
 * ticker&rarr;category mapping is captured here, at the one factory method every animated sprite's ticker
 * comes out of.
 */
@Mixin(SpriteContents.class)
public abstract class SpriteContentsTickerCaptureMixin {

    @Shadow
    public abstract ResourceLocation name();

    @Inject(method = "createTicker()Lnet/minecraft/client/renderer/texture/SpriteTicker;", at = @At("RETURN"))
    private void streamline$captureTicker(final CallbackInfoReturnable<SpriteTicker> cir) {
        StreamlineState.armedTickerCapture = true;
        final SpriteTicker ticker = cir.getReturnValue();
        if (ticker != null)
            AnimationControl.register(ticker, this.name());
    }
}
