package com.sanhiruzu.magitemaocean;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

// Tracks the owning chunk while this mod invokes a placed feature.
// Features may inspect the generation region, but writes belong to one chunk to keep output order-independent.
public final class OceanFeatureWriteScope implements AutoCloseable {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private final Scope previous;

    private OceanFeatureWriteScope(ChunkPos owner, int candidateMargin) {
        this.previous = CURRENT.get();
        CURRENT.set(new Scope(owner, candidateMargin));
    }

    public static OceanFeatureWriteScope forChunk(ChunkPos owner, int candidateMargin) {
        return new OceanFeatureWriteScope(owner, candidateMargin);
    }

    public static boolean permits(BlockPos pos) {
        Scope scope = CURRENT.get();
        return scope == null || (pos.getX() >> 4) == scope.owner.x && (pos.getZ() >> 4) == scope.owner.z;
    }

    public static int candidateMargin() {
        Scope scope = CURRENT.get();
        return scope == null ? 0 : scope.candidateMargin;
    }

    public static BlockPos constrainCandidateToOwnerInterior(BlockPos pos) {
        Scope scope = CURRENT.get();
        if (scope == null || scope.candidateMargin == 0) return pos;

        int minX = scope.owner.getMinBlockX() + scope.candidateMargin;
        int minZ = scope.owner.getMinBlockZ() + scope.candidateMargin;
        int maxX = scope.owner.getMaxBlockX() - scope.candidateMargin;
        int maxZ = scope.owner.getMaxBlockZ() - scope.candidateMargin;
        return new BlockPos(Math.max(minX, Math.min(maxX, pos.getX())), pos.getY(),
                Math.max(minZ, Math.min(maxZ, pos.getZ())));
    }

    @Override
    public void close() {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    private record Scope(ChunkPos owner, int candidateMargin) {}
}
