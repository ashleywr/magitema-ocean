package com.sanhiruzu.magitemaocean.mixin;

import com.sanhiruzu.magitemaocean.OceanFeatureWriteScope;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Applies chunk ownership to writes made by our explicitly invoked placed features.
// This prevents neighboring feature patches from racing to overwrite a chunk's decoration.
@Mixin(WorldGenRegion.class)
public abstract class OceanFeatureWriteBoundaryMixin {
    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void magitemaOcean$keepFeatureWritesInOwnerChunk(BlockPos pos, BlockState state,
                                                              int flags, int recursionLeft,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (!OceanFeatureWriteScope.permits(pos)) cir.setReturnValue(false);
    }
}
