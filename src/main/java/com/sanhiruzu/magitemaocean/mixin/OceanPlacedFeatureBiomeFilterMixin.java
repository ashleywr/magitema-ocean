package com.sanhiruzu.magitemaocean.mixin;

import com.sanhiruzu.magitemaocean.MagitemaOcean;
import com.sanhiruzu.magitemaocean.OceanWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Let explicitly tagged placed features pass only in their scoped marine or island biomes. */
@Mixin(BiomeFilter.class)
public abstract class OceanPlacedFeatureBiomeFilterMixin {
    @Inject(method = "shouldPlace", at = @At("HEAD"), cancellable = true)
    private void magitemaOcean$allowTaggedMarineFeature(PlacementContext context, RandomSource random,
                                                         BlockPos position, CallbackInfoReturnable<Boolean> cir) {
        ChunkGenerator generator = context.generator();
        if (!OceanWorld.isOcean(generator)) return;

        String profile = OceanWorld.exploration(generator) ? "exploration" : "oceanblock";
        TagKey<PlacedFeature> allowed = TagKey.create(Registries.PLACED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID, profile));
        var registry = context.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var feature = context.topFeature().flatMap(registry::getResourceKey)
                .flatMap(registry::getHolder).orElse(null);
        if (feature == null || !feature.is(allowed)) return;

        String biomePath = context.getLevel().getBiome(position).unwrapKey()
                .map(key -> key.location().getPath()).orElse("");
        TagKey<net.minecraft.world.level.biome.Biome> surfaceBiomes = TagKey.create(Registries.BIOME,
                ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID, "surface_biomes"));
        boolean scoped = false;
        boolean matches = false;
        var biomes = context.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (Holder<net.minecraft.world.level.biome.Biome> biome : biomes.getTagOrEmpty(surfaceBiomes)) {
            String path = biome.unwrapKey().map(key -> key.location().getPath()).orElse("");
            if (path.isEmpty()) continue;
            TagKey<PlacedFeature> scope = TagKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID, profile + "_" + path));
            boolean includesFeature = false;
            for (Holder<PlacedFeature> scopedFeature : registry.getTagOrEmpty(scope)) {
                if (scopedFeature.equals(feature)) {
                    includesFeature = true;
                    break;
                }
            }
            if (includesFeature) {
                scoped = true;
                if (path.equals(biomePath)) matches = true;
            }
        }
        cir.setReturnValue(!scoped || matches);
    }
}
