package dev.streamline.details.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;

/**
 * Block ambience density: vanilla's {@code animateTick} loops 667 times per tick, each iteration sampling
 * two random positions (ranges 16 and 32) and asking the block + fluid there to emit ambience (torch
 * flames, drips, crackles). Scaling the loop bound scales the whole cost linearly. Floor of 1 iteration
 * (config floor is 10%) keeps creative marker particles (light blocks, barriers) alive.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelAmbienceMixin {

    @ModifyConstant(method = "animateTick(III)V", constant = @Constant(intValue = 667))
    private int streamline$scaleAmbience(final int original) {
        StreamlineState.armedAmbience = true;
        return Math.max(1, original * StreamlineConfig.ambienceDensity() / 100);
    }
}
