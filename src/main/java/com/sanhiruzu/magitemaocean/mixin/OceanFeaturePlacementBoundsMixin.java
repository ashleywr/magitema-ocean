package com.sanhiruzu.magitemaocean.mixin;

import com.sanhiruzu.magitemaocean.OceanFeatureWriteScope;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/** Constrains wide placed-feature starts to a safe interior of their owner chunk.
 * The inset prevents oversized trees and reef patches from writing into neighboring chunks. */
@Mixin(InSquarePlacement.class)
public abstract class OceanFeaturePlacementBoundsMixin {
    @Inject(method = "getPositions", at = @At("RETURN"), cancellable = true)
    private void magitemaOcean$keepSensitiveFeatureStartsInsideChunk(PlacementContext context,
                                                                     RandomSource random,
                                                                     BlockPos origin,
                                                                     CallbackInfoReturnable<Stream<BlockPos>> cir) {
        if (OceanFeatureWriteScope.candidateMargin() > 0) {
            cir.setReturnValue(cir.getReturnValue().map(OceanFeatureWriteScope::constrainCandidateToOwnerInterior));
        }
    }
}
