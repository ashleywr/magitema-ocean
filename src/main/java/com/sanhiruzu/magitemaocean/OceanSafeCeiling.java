package com.sanhiruzu.magitemaocean;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Computes the lowest nearby seabed used to guard the dry cave roof.
 * The cache_2d marker in the preset stores this value once per horizontal sample column.
 */
public record OceanSafeCeiling(DensityFunction floor) implements DensityFunction {
    public static final KeyDispatchDataCodec<OceanSafeCeiling> CODEC = KeyDispatchDataCodec.of(
        RecordCodecBuilder.mapCodec(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("floor").forGetter(OceanSafeCeiling::floor)
        ).apply(i, OceanSafeCeiling::new))
    );

    @Override
    public double compute(FunctionContext context) {
        return OceanTerrain.safeCeiling(floor, context.blockX(), context.blockZ());
    }

    @Override
    public void fillArray(double[] values, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new OceanSafeCeiling(floor.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return floor.minValue() - OceanTerrain.ROOF - OceanTerrain.WALL_RADIUS;
    }

    @Override
    public double maxValue() {
        return floor.maxValue() - OceanTerrain.ROOF - OceanTerrain.WALL_RADIUS;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
