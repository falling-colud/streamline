package dev.streamline.frame.mixin;

import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.streamline.core.StreamlineState;
import dev.streamline.frame.FpsGovernor;

/**
 * Idle detection: any key press or typed character stamps the governor's last-input time (held keys emit
 * no repeat events - the governor polls held gameplay bindings separately). HEAD inserts only.
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerActivityMixin {

    @Inject(method = {"keyPress", "charTyped"}, at = @At("HEAD"))
    private void streamline$noteActivity(final CallbackInfo ci) {
        StreamlineState.armedActivityKeyboard = true;
        FpsGovernor.noteInput();
    }
}
