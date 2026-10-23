package com.sanhiruzu.magitemaocean;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Original smooth geography. Inputs are seeded by Minecraft RandomState. */
public record OceanFloor(DensityFunction geography, DensityFunction ridges,
                         DensityFunction channels, DensityFunction detail, boolean islands) implements DensityFunction {
    public static final KeyDispatchDataCodec<OceanFloor> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("geography").forGetter(OceanFloor::geography),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridges").forGetter(OceanFloor::ridges),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("channels").forGetter(OceanFloor::channels),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail").forGetter(OceanFloor::detail),
        Codec.BOOL.fieldOf("islands").forGetter(OceanFloor::islands)
    ).apply(i, OceanFloor::new)));

    public static double smooth(double lo, double hi, double x) {
        double t = Math.clamp((x-lo)/(hi-lo), 0, 1);
        return t*t*(3-2*t);
    }
    @Override public double compute(FunctionContext c) {
        var p = new SinglePointContext(c.blockX(), 0, c.blockZ());
        double g = geography.compute(p), r = ridges.compute(p), t = channels.compute(p), d = detail.compute(p);
        double shelf = smooth(-0.02, 0.36, g);
        double ridge = Math.pow(1-smooth(0, 0.25, Math.abs(r)), 2);
        double trench = 1-smooth(0.025, 0.13, Math.abs(t));
        double floor = -305 + 330*shelf + 105*ridge - 225*trench*(1-shelf) + 14*d;
        floor = Math.clamp(floor, -548, 38);
        if (!islands) floor = Math.min(floor, -18);
        if (islands) {
            // Isolated summits require two independent positive fields, so no continents.
            double island = smooth(0.48, 0.66, g) * smooth(0.28, 0.5, r);
            floor = floor*(1-island) + 88*island;
        }
        return floor;
    }
    @Override public void fillArray(double[] a, ContextProvider p) { p.fillAllDirectly(a, this); }
    @Override public DensityFunction mapAll(Visitor v) { return v.apply(new OceanFloor(geography.mapAll(v), ridges.mapAll(v), channels.mapAll(v), detail.mapAll(v), islands)); }
    @Override public double minValue() { return -548; }
    @Override public double maxValue() { return islands ? 88 : -18; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
