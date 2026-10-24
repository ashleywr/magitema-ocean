package com.sanhiruzu.magitemaocean;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.block.Blocks;
import java.util.Arrays;
public final class OceanWorld {
    private static final int FLUID_COLUMN_CACHE_SIZE = 512;
    private static final long EMPTY_COLUMN = Long.MIN_VALUE;
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
    public static DensityFunction floorFunction(RandomState state) {
        DensityFunction floor = unwrap(state.router().continents());
        if (!(floor instanceof OceanFloor)) throw new IllegalArgumentException("Ocean floor requested for a non-ocean generator");
        return floor;
    }
    public static int floorBlockAt(DensityFunction floor, int x, int z) {
        return (int)Math.ceil(OceanTerrain.floorAt(floor, x, z)) - 1;
    }
    public static boolean exploration(ChunkGenerator g) {
        return g instanceof NoiseBasedChunkGenerator n && unwrap(n.generatorSettings().value().noiseRouter().continents()) instanceof OceanFloor f && f.islands();
    }
    public static Aquifer.FluidPicker fluids(RandomState state) {
        DensityFunction floor = unwrap(state.router().continents());
        long[] columnKeys = new long[FLUID_COLUMN_CACHE_SIZE];
        Arrays.fill(columnKeys, EMPTY_COLUMN);
        double[] columnFloors = new double[FLUID_COLUMN_CACHE_SIZE];
        var water = new Aquifer.FluidStatus(63, Blocks.WATER.defaultBlockState());
        var air = new Aquifer.FluidStatus(-1000, Blocks.AIR.defaultBlockState());
        var lava = new Aquifer.FluidStatus(-684, Blocks.LAVA.defaultBlockState());
        return (x,y,z) -> {
            // Density caves never reach this band; lava reserved for future sealed features.
            if (y < -684) return lava;
            long key = ((long)x << 32) | (z & 0xffffffffL);
            int hash = (int)(key ^ (key >>> 32));
            hash *= 0x9E3779B9;
            int slot = (hash ^ (hash >>> 16)) & (FLUID_COLUMN_CACHE_SIZE - 1);
            if (columnKeys[slot] != key) {
                columnFloors[slot] = OceanTerrain.floorAt(floor,x,z);
                columnKeys[slot] = key;
            }
            double f = columnFloors[slot];
            return y > f-16 ? water : air;
        };
    }
}
