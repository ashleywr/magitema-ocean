package com.sanhiruzu.magitemaocean;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MagitemaOcean.ID)
public final class MagitemaOcean {
 public static final String ID="magitema_ocean";
 private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITIES=DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE,ID);
 private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOMES=DeferredRegister.create(Registries.BIOME_SOURCE,ID);
 private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES=DeferredRegister.create(BuiltInRegistries.STRUCTURE_TYPE,ID);
 private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES=DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE,ID);
 public static final net.neoforged.neoforge.registries.DeferredHolder<StructureType<?>,StructureType<OceanSalvageStructure>> SALVAGE_STRUCTURE=STRUCTURE_TYPES.register("deep_salvage",()->()->OceanSalvageStructure.CODEC);
 public static final net.neoforged.neoforge.registries.DeferredHolder<StructurePieceType,StructurePieceType.ContextlessType> SALVAGE_PIECE=STRUCTURE_PIECES.register("deep_salvage",()->OceanSalvagePiece::new);
 public MagitemaOcean(IEventBus bus) {
  DENSITIES.register("floor",()->OceanFloor.CODEC.codec());DENSITIES.register("terrain",()->OceanTerrain.CODEC.codec());DENSITIES.register("safe_ceiling",()->OceanSafeCeiling.CODEC.codec());
  BIOMES.register("marine",()->MarineBiomes.CODEC);
  DENSITIES.register(bus);BIOMES.register(bus);STRUCTURE_TYPES.register(bus);STRUCTURE_PIECES.register(bus);
  NeoForge.EVENT_BUS.register(new OceanLifecycle());
 }
}
