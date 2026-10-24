package com.sanhiruzu.magitemaocean;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Builds a small seabed vent from NauTec's registered hydrothermal block.
 * It keeps the resource feature useful without allowing its footprint to cross chunk ownership. */
public final class OceanVentClusterFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceLocation NAUTEC_VENT = ResourceLocation.fromNamespaceAndPath("nautec", "hydrothermal_vent");

    public OceanVentClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        Block ventBlock = level.registryAccess().registryOrThrow(Registries.BLOCK).getOptional(NAUTEC_VENT).orElse(null);
        if (ventBlock == null) return false;

        int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, origin.getX(), origin.getZ());
        BlockPos ventPos = new BlockPos(origin.getX(), floorY, origin.getZ());
        if (!level.getFluidState(ventPos).is(FluidTags.WATER)
                || !level.getFluidState(ventPos.above()).is(FluidTags.WATER)
                || !level.getBlockState(ventPos.below()).isSolidRender(level, ventPos.below())) return false;

        BlockState vent = ventBlock.defaultBlockState();
        if (vent.hasProperty(BlockStateProperties.WATERLOGGED)) {
            vent = vent.setValue(BlockStateProperties.WATERLOGGED, true);
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int distanceSquared = dx * dx + dz * dz;
                if (distanceSquared > 4) continue;
                pos.set(ventPos.getX() + dx, ventPos.getY(), ventPos.getZ() + dz);
                if (!level.getFluidState(pos).is(FluidTags.WATER)) continue;
                BlockPos supportPos = pos.below();
                BlockState support = level.getBlockState(supportPos);
                if (!support.isSolidRender(level, supportPos) || support.is(Blocks.BEDROCK)) continue;

                if (dx == 0 && dz == 0) {
                    level.setBlock(pos, vent, 2);
                    placed = true;
                } else if (distanceSquared <= 2) {
                    level.setBlock(supportPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                } else {
                    level.setBlock(supportPos, Blocks.BASALT.defaultBlockState(), 2);
                }
            }
        }
        return placed;
    }
}
