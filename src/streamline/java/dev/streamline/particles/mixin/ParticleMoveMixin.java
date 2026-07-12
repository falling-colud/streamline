package dev.streamline.particles.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import dev.streamline.core.StreamlineConfig;
import dev.streamline.core.StreamlineState;
import dev.streamline.particles.ParticleCollisionFastPath;

/**
 * Particle physics fast path: wraps the one {@code Entity.collideBoundingBox} call in
 * {@code Particle.move} ({@code @WrapOperation} - composes if anything else ever wraps it). When the
 * pre-scan proves the swept volume has no colliders, the motion is returned unchanged - exactly what
 * vanilla computes for an empty collider list, minus the iterator/list/shape machinery per particle per
 * tick. Sightline governs particle SPAWNING and Fovea culls particle RENDERING; this is the third leg,
 * the per-tick physics cost of the particles that exist.
 */
@Mixin(Particle.class)
public abstract class ParticleMoveMixin {

    @WrapOperation(
        method = "move(DDD)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 streamline$fastCollide(final Entity entity, final Vec3 vec, final AABB collisionBox,
                                        final Level level, final List<?> potentialHits,
                                        final Operation<Vec3> original) {
        StreamlineState.armedParticlePhysics = true;
        if (StreamlineConfig.particleFastPath() && entity == null && potentialHits.isEmpty()
            && ParticleCollisionFastPath.noColliders(level, collisionBox.expandTowards(vec))) {
            StreamlineState.particleCollisionsFast++;
            return vec;
        }
        StreamlineState.particleCollisionsFull++;
        return original.call(entity, vec, collisionBox, level, potentialHits);
    }
}
