package com.sanhiruzu.magitemaocean;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

// Resolves profile feature scopes against the biome registry for generation and custom decoration.
// A feature listed in any biome-specific scope is restricted to matching scoped biomes.
public final class OceanFeatureScopes {
    private OceanFeatureScopes() {}

    public static boolean allowsInBiome(WorldGenLevel level, String profile,
                                        Holder<PlacedFeature> feature, Holder<Biome> candidateBiome) {
        var placedFeatures = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        TagKey<Biome> surfaceBiomes = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID, "surface_biomes"));
        boolean scoped = false;
        boolean matches = false;

        for (Holder<Biome> biome : biomes.getTagOrEmpty(surfaceBiomes)) {
            String path = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
            if (path.isEmpty()) continue;

            TagKey<PlacedFeature> scope = TagKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID, profile + "_" + path));
            for (Holder<PlacedFeature> scopedFeature : placedFeatures.getTagOrEmpty(scope)) {
                if (scopedFeature.equals(feature)) {
                    scoped = true;
                    if (biome.equals(candidateBiome)) matches = true;
                    break;
                }
            }
        }
        return !scoped || matches;
    }
}
