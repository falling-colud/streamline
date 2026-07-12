package dev.streamline.particles;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Allocation-free "is it all air" pre-scan for particle physics. Vanilla answers "which blocks does this
 * particle hit" with the full {@code BlockCollisions} machinery - an iterator, an immutable-list builder
 * and voxel-shape work, allocated per particle per tick. For airborne particles (rain falling, smoke
 * rising - the overwhelming majority every tick of a storm) the answer is always "nothing".
 *
 * <p>This scan walks the exact cell set vanilla's {@code BlockCollisions} cursor walks - the swept box
 * inflated by 1.0E-7, extended by one cell per axis, corners skipped, the face ring only counting blocks
 * with {@code hasLargeCollisionShape()}, the edge ring only moving pistons, unloaded chunks contributing
 * nothing - and reports whether ANY candidate collider exists. No candidates means vanilla would have
 * returned the motion unchanged, so skipping the full path is bit-identical. Any candidate at all (even
 * one whose shape would not actually intersect) falls back to the full vanilla path.</p>
 *
 * <p>Render-thread only (particles tick there), so one reusable cursor position is safe.</p>
 */
public final class ParticleCollisionFastPath {

    /** Fast-moving particles sweep big boxes; beyond this many cells let vanilla handle it. */
    private static final int MAX_SCAN_CELLS = 64;

    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();

    /** @return {@code true} if the swept box provably has no block colliders (safe to skip collision). */
    public static boolean noColliders(final Level level, final AABB sweptBox) {
        final int x0 = Mth.floor(sweptBox.minX - 1.0E-7) - 1;
        final int x1 = Mth.floor(sweptBox.maxX + 1.0E-7) + 1;
        final int y0 = Mth.floor(sweptBox.minY - 1.0E-7) - 1;
        final int y1 = Mth.floor(sweptBox.maxY + 1.0E-7) + 1;
        final int z0 = Mth.floor(sweptBox.minZ - 1.0E-7) - 1;
        final int z1 = Mth.floor(sweptBox.maxZ + 1.0E-7) + 1;

        final long cells = (long) (x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        if (cells > MAX_SCAN_CELLS)
            return false;

        BlockGetter chunk = null;
        int chunkX = Integer.MIN_VALUE;
        int chunkZ = Integer.MIN_VALUE;

        for (int x = x0; x <= x1; x++) {
            final boolean xEdge = x == x0 || x == x1;
            for (int z = z0; z <= z1; z++) {
                final boolean zEdge = z == z0 || z == z1;
                final int cx = x >> 4;
                final int cz = z >> 4;
                if (cx != chunkX || cz != chunkZ) {
                    chunk = level.getChunkForCollisions(cx, cz);
                    chunkX = cx;
                    chunkZ = cz;
                }
                if (chunk == null)
                    continue; // vanilla: unloaded chunk yields no colliders
                for (int y = y0; y <= y1; y++) {
                    final int edges = (xEdge ? 1 : 0) + (y == y0 || y == y1 ? 1 : 0) + (zEdge ? 1 : 0);
                    if (edges == 3)
                        continue; // corners are skipped outright by the vanilla cursor
                    final BlockState state = chunk.getBlockState(CURSOR.set(x, y, z));
                    if (state.isAir())
                        continue;
                    if (edges == 1 && !state.hasLargeCollisionShape())
                        continue;
                    if (edges == 2 && !state.is(Blocks.MOVING_PISTON))
                        continue;
                    if (state.getCollisionShape(level, CURSOR).isEmpty())
                        continue;
                    return false; // a candidate collider - take the full vanilla path
                }
            }
        }
        return true;
    }

    private ParticleCollisionFastPath() {}
}
