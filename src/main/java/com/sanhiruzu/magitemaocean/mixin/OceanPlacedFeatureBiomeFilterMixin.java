package com.sanhiruzu.magitemaocean.mixin;

import com.sanhiruzu.magitemaocean.MagitemaOcean;
import com.sanhiruzu.magitemaocean.OceanFeatureScopes;
import com.sanhiruzu.magitemaocean.OceanWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.biome.Biome;
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

        Holder<Biome> candidateBiome = context.getLevel().getBiome(position);
        cir.setReturnValue(OceanFeatureScopes.allowsInBiome(context.getLevel(), profile, feature, candidateBiome));
    }
}
