package com.sanhiruzu.magitemaocean;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.block.Blocks;
public final class OceanWorld {
    private OceanWorld() {}
    public static boolean isOcean(ChunkGenerator g) {
        return g instanceof NoiseBasedChunkGenerator n
            && (n.getBiomeSource() instanceof MarineBiomes || isOcean(n.generatorSettings().value()));
    }
    public static DensityFunction unwrap(DensityFunction function) {
        while (function instanceof DensityFunctions.HolderHolder holder) function = holder.function().value();
        return function;
    }
    public static boolean isOcean(NoiseGeneratorSettings s) { return unwrap(s.noiseRouter().continents()) instanceof OceanFloor; }
    public static boolean exploration(ChunkGenerator g) {
        return g instanceof NoiseBasedChunkGenerator n && unwrap(n.generatorSettings().value().noiseRouter().continents()) instanceof OceanFloor f && f.islands();
    }
    public static Aquifer.FluidPicker fluids(RandomState state) {
        DensityFunction floor = state.router().continents();
        var water = new Aquifer.FluidStatus(63, Blocks.WATER.defaultBlockState());
        var air = new Aquifer.FluidStatus(-1000, Blocks.AIR.defaultBlockState());
        var lava = new Aquifer.FluidStatus(-684, Blocks.LAVA.defaultBlockState());
        return (x,y,z) -> {
            double f = OceanTerrain.floorAt(floor,x,z);
            // Density caves never reach this band; lava reserved for future sealed features.
            if (y < -684) return lava;
            return y > f-16 ? water : air;
        };
    }
}
