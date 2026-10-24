package com.sanhiruzu.magitemaocean;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

// Tracks the owning chunk while this mod invokes a placed feature.
// Features may inspect the generation region, but writes belong to one chunk to keep output order-independent.
public final class OceanFeatureWriteScope implements AutoCloseable {
    private static final ThreadLocal<ChunkPos> OWNER = new ThreadLocal<>();
    private final ChunkPos previous;

    private OceanFeatureWriteScope(ChunkPos owner) {
        this.previous = OWNER.get();
        OWNER.set(owner);
    }

    public static OceanFeatureWriteScope forChunk(ChunkPos owner) {
        return new OceanFeatureWriteScope(owner);
    }

    public static boolean permits(BlockPos pos) {
        ChunkPos owner = OWNER.get();
        return owner == null || (pos.getX() >> 4) == owner.x && (pos.getZ() >> 4) == owner.z;
    }

    @Override
    public void close() {
        if (previous == null) OWNER.remove();
        else OWNER.set(previous);
    }
}
