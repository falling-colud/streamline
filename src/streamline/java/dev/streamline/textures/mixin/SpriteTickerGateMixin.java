package dev.streamline.textures.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.streamline.core.StreamlineState;
import dev.streamline.textures.AnimationControl;

/**
 * Texture animation gate: cancelling {@code tickAndUpload} at HEAD freezes the sprite completely - no
 * frame advance, no interpolation, no GPU upload. Sodium's "animate only visible textures" mixin cancels
 * at the same point for off-screen sprites; two HEAD cancels compose (either may veto).
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker")
public abstract class SpriteTickerGateMixin {

    @Inject(method = "tickAndUpload(II)V", at = @At("HEAD"), cancellable = true)
    private void streamline$gateAnimation(final int x, final int y, final CallbackInfo ci) {
        StreamlineState.armedTickerGate = true;
        if (!AnimationControl.shouldTick(this)) {
            StreamlineState.frozenSpriteUploads++;
            ci.cancel();
        }
    }
}
