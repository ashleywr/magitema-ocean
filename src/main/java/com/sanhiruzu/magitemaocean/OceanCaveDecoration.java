package com.sanhiruzu.magitemaocean;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.levelgen.Heightmap;

/** Runs after profile features to add sparse decoration inside sealed ocean caves.
 * Owns only dripstone, lush-cave and deep-dark habitat blocks because vanilla
 * biome decoration is filtered by this custom generator.
 */
final class OceanCaveDecoration {
    private static final ResourceKey<Biome> DRIPSTONE = biome("dripstone_caves");
    private static final ResourceKey<Biome> LUSH = biome("lush_caves");
    private static final ResourceKey<Biome> DEEP_DARK = biome("deep_dark");

    private OceanCaveDecoration() {}

    static void decorate(WorldGenLevel level, int chunkX, int chunkZ, RandomSource random) {
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = baseX + random.nextInt(16);
            int z = baseZ + random.nextInt(16);
            int seabed = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            int top = seabed - 49;
            int bottom = Math.max(level.getMinBuildHeight() + 4, seabed - 192);
            // Biomes resolve at four-block quart resolution; matching that stride caps
            // cave inspection at at most 1,152 block/biome probes per chunk instead of ~3,100.
            for (int y = top; y >= bottom; y -= 4) {
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.getBlockState(pos).isAir()) continue;
                var biome = level.getBiome(pos);
                if (biome.is(LUSH) && decorateLush(level, pos, random)) break;
                if (biome.is(DRIPSTONE) && random.nextInt(3) == 0 && decorateDripstone(level, pos, random)) break;
                if (biome.is(DEEP_DARK) && decorateDeepDark(level, pos)) break;
            }
        }
    }

    private static boolean decorateLush(WorldGenLevel level, BlockPos pos, RandomSource random) {
        for (int depth = 1; depth <= 6; depth++) {
            BlockPos floor = pos.below(depth);
            BlockPos floorAir = floor.above();
            if (!replaceableRock(level.getBlockState(floor)) || !level.getBlockState(floorAir).isAir()
                    || !level.getBiome(floorAir).is(LUSH)) continue;
            level.setBlock(floor, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
            BlockState carpet = Blocks.MOSS_CARPET.defaultBlockState();
            if (carpet.canSurvive(level, floorAir)) level.setBlock(floorAir, carpet, 2);
            if (random.nextBoolean()) {
                Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos neighbor = floor.relative(direction);
                if (replaceableRock(level.getBlockState(neighbor))) level.setBlock(neighbor, Blocks.MOSS_BLOCK.defaultBlockState(), 2);
            }
            return true;
        }
        if (!level.getBlockState(pos.above()).isSolid()) return false;
        BlockState blossom = Blocks.SPORE_BLOSSOM.defaultBlockState();
        if (!blossom.canSurvive(level, pos)) return false;
        level.setBlock(pos, blossom, 2);
        return true;
    }

    private static boolean decorateDripstone(WorldGenLevel level, BlockPos pos, RandomSource random) {
        Direction direction = random.nextBoolean() ? Direction.DOWN : Direction.UP;
        BlockPos support = direction == Direction.DOWN ? pos.above() : pos.below();
        if (!level.getBlockState(support).isSolid()) return false;
        BlockState tip = Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(PointedDripstoneBlock.TIP_DIRECTION, direction)
                .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP);
        if (!tip.canSurvive(level, pos)) return false;
        level.setBlock(pos, tip, 2);
        return true;
    }

    private static boolean decorateDeepDark(WorldGenLevel level, BlockPos pos) {
        BlockPos floor = pos.below();
        if (replaceableRock(level.getBlockState(floor))) {
            level.setBlock(floor, Blocks.SCULK.defaultBlockState(), 2);
            return true;
        }
        BlockPos ceiling = pos.above();
        if (!replaceableRock(level.getBlockState(ceiling))) return false;
        level.setBlock(ceiling, Blocks.SCULK.defaultBlockState(), 2);
        return true;
    }

    private static boolean replaceableRock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("minecraft", name));
    }
}
