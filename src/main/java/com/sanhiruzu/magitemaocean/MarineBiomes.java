package com.sanhiruzu.magitemaocean;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.stream.Stream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Biome regions follow the exact seeded floor field used by the terrain generator. */
public final class MarineBiomes extends BiomeSource {
    private static final int MARINE_BIOMES = 10;
    private static final int ISLAND_BIOMES = 4;
    private static final int CAVE_BIOMES = MARINE_BIOMES + ISLAND_BIOMES;
    private static final int HADAL_TRENCH = 0;
    private static final int ABYSSAL_BASIN = 1;
    private static final int BASALT_RIDGE = 2;
    private static final int VOLCANIC_SLOPE = 3;
    private static final int SEAMOUNT_SLOPE = 4;
    private static final int ROCKY_SEABED = 5;
    private static final int SAND_SHELF = 6;
    private static final int KELP_GROVE = 7;
    private static final int CORAL_GARDEN = 8;
    private static final int SHELTERED_LAGOON = 9;
    private static final int SANDY_ISLAND = 10;
    private static final int JUNGLE_ISLAND = 11;
    private static final int EXPOSED_CLIFF = 12;
    private static final int VOLCANIC_ISLAND = 13;

    public static final MapCodec<MarineBiomes> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Biome.CODEC.listOf().fieldOf("biomes").forGetter(b -> b.biomes)
    ).apply(i, MarineBiomes::new));

    private final List<Holder<Biome>> biomes;
    private final ConcurrentMap<SampleKey, SurfaceChoice> surfaceChoices = new ConcurrentHashMap<>();
    private final ConcurrentMap<SampleKey, Double> floorSamples = new ConcurrentHashMap<>();

    public MarineBiomes(List<Holder<Biome>> biomes) {
        if (biomes.size() != CAVE_BIOMES + 2) {
            throw new IllegalArgumentException("Marine biome palette requires ten marine, four island and two cave entries");
        }
        this.biomes = List.copyOf(biomes);
    }

    @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return biomes.stream(); }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        var center = context(x, y, z);
        SurfaceChoice choice = surfaceChoice(x, z, sampler);
        double floor = choice.floor();
        double burial = floor - y * 4.0;

        // The two cave biomes occupy rock well below the actual floor, independent of absolute Y.
        if (burial > 48) {
            return biomes.get(CAVE_BIOMES + (sampler.humidity().compute(center) > 0.1 ? 1 : 0));
        }

        return biomes.get(choice.biome());
    }

    private SurfaceChoice surfaceChoice(int x, int z, Climate.Sampler sampler) {
        SampleKey key = new SampleKey(sampler, x, z);
        SurfaceChoice cached = surfaceChoices.get(key);
        if (cached != null) return cached;

        int y = 0;
        double floor = floorAt(sampler, x, y, z);
        double north = floorAt(sampler, x, y, z - 4);
        double south = floorAt(sampler, x, y, z + 4);
        double west = floorAt(sampler, x - 4, y, z);
        double east = floorAt(sampler, x + 4, y, z);
        double slope = Math.max(Math.max(Math.abs(north - south), Math.abs(west - east)) * 0.5,
            Math.max(Math.max(Math.abs(north - floor), Math.abs(south - floor)),
                     Math.max(Math.abs(west - floor), Math.abs(east - floor))));
        double bowl = (north + south + west + east) * 0.25 - floor;
        var center = context(x, y, z);
        double warmth = sampler.temperature().compute(center);
        double humidity = sampler.humidity().compute(center);

        // Land exists only in the Exploration floor field. Relief and climate distinguish
        // humid jungle, high volcanic summits, bare cliffs and sandy atolls.
        int biome;
        if (floor > 63) {
            if (floor >= 78) biome = VOLCANIC_ISLAND;
            else if (slope >= 18) biome = EXPOSED_CLIFF;
            else if (warmth > 0.08 && humidity > 0.1) biome = JUNGLE_ISLAND;
            else biome = SANDY_ISLAND;
        }
        else if (floor <= -485) biome = HADAL_TRENCH;
        else if (floor <= -385) biome = ABYSSAL_BASIN;
        else if (floor <= -155) {
            if (slope >= 42 && floor > -335) biome = BASALT_RIDGE;
            else if (slope >= 42) biome = VOLCANIC_SLOPE;
            else if (slope >= 14) biome = SEAMOUNT_SLOPE;
            else biome = ROCKY_SEABED;
        } else if (floor >= -82 && warmth > 0.08 && slope < 34) {
            biome = CORAL_GARDEN;
        } else if (floor >= -120 && bowl >= 1.5 && slope < 14) {
            biome = SHELTERED_LAGOON;
        } else if (floor >= -175 && humidity > -0.05) {
            biome = KELP_GROVE;
        } else {
            biome = SAND_SHELF;
        }
        SurfaceChoice computed = new SurfaceChoice(floor, biome);
        return putBounded(surfaceChoices, key, computed, 8192);
    }

    private double floorAt(Climate.Sampler sampler, int x, int y, int z) {
        SampleKey key = new SampleKey(sampler, x, z);
        Double cached = floorSamples.get(key);
        if (cached != null) return cached;
        double computed = sampler.continentalness().compute(context(x, y, z));
        return putBounded(floorSamples, key, computed, 32768);
    }

    private static <T> T putBounded(ConcurrentMap<SampleKey, T> cache, SampleKey key, T value, int maxSize) {
        T existing = cache.putIfAbsent(key, value);
        if (cache.size() > maxSize) {
            // Evict as a batch; read-heavy lookups stay lock-free between rare clears.
            synchronized (cache) {
                if (cache.size() > maxSize) cache.clear();
            }
        }
        return existing == null ? value : existing;
    }

    private static DensityFunction.SinglePointContext context(int x, int y, int z) {
        return new DensityFunction.SinglePointContext(x * 4, y * 4, z * 4);
    }

    private record SurfaceChoice(double floor, int biome) {}

    /** Sample keys use sampler identity because each sampler owns a seed/settings-specific noise graph. */
    private static final class SampleKey {
        private final Climate.Sampler sampler;
        private final int x;
        private final int z;

        private SampleKey(Climate.Sampler sampler, int x, int z) {
            this.sampler = sampler;
            this.x = x;
            this.z = z;
        }

        @Override public boolean equals(Object other) {
            return other instanceof SampleKey key && sampler == key.sampler && x == key.x && z == key.z;
        }

        @Override public int hashCode() {
            int result = System.identityHashCode(sampler);
            result = 31 * result + x;
            return 31 * result + z;
        }
    }
}
