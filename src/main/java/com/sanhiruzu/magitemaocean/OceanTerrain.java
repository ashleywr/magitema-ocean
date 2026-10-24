package com.sanhiruzu.magitemaocean;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Protected dry caves. No chunk/world access, mutable RNG or global cache. */
public record OceanTerrain(DensityFunction floor, DensityFunction safeCeiling,
                           DensityFunction caves, DensityFunction tunnels) implements DensityFunction {
    public static final int ROOF = 32;
    public static final int WALL_RADIUS = 24;
    public static final KeyDispatchDataCodec<OceanTerrain> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("floor").forGetter(OceanTerrain::floor),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("safe_ceiling").forGetter(OceanTerrain::safeCeiling),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("caves").forGetter(OceanTerrain::caves),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("tunnels").forGetter(OceanTerrain::tunnels)
    ).apply(i, OceanTerrain::new)));

    public static double floorAt(DensityFunction floor, int x, int z) { return floor.compute(new SinglePointContext(x, 0, z)); }
    public static double safeCeiling(DensityFunction floor, int x, int z) {
        double min = Double.POSITIVE_INFINITY;
        // Center, cardinal points and diagonals detect steep walls/trench mouths while
        // the extra 24-block allowance covers relief between samples.
        min = Math.min(min, floorAt(floor,x,z));
        for(int dx : new int[]{-48,-24,24,48}) {
            min=Math.min(min,floorAt(floor,x+dx,z));
            min=Math.min(min,floorAt(floor,x,z+dx));
        }
        for(int dx : new int[]{-36,36}) for(int dz : new int[]{-36,36})
            min=Math.min(min,floorAt(floor,x+dx,z+dz));
        return min-ROOF-24;
    }
    @Override public double compute(FunctionContext c) {
        // The preset wraps this y-independent field in cache_2d. Keep the original
        // NoiseChunk context here so that cache can reuse one floor value per column.
        double f = floor.compute(c);
        double rock = f-c.blockY();
        if (rock < 24 || c.blockY() < -660) return rock;
        double cavity = Math.max(0.105-Math.abs(tunnels.compute(c)), caves.compute(c)-0.38);
        if (cavity <= 0) return rock;
        double cover = safeCeiling.compute(c)-c.blockY();
        // 16 blocks of blending isolates caves from interpolation at the guard boundary.
        double opening = Math.min(cavity*48, Math.min(cover/4, (c.blockY()+660)/4.0));
        return Math.min(rock, -opening);
    }
    @Override public void fillArray(double[] a, ContextProvider p) { p.fillAllDirectly(a, this); }
    @Override public DensityFunction mapAll(Visitor v) { return v.apply(new OceanTerrain(floor.mapAll(v), safeCeiling.mapAll(v), caves.mapAll(v), tunnels.mapAll(v))); }
    @Override public double minValue() { return -1024; }
    @Override public double maxValue() { return 1024; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
