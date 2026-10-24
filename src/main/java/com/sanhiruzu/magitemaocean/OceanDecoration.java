package com.sanhiruzu.magitemaocean;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Only explicitly allowlisted decoration and structures execute in this Overworld. */
public final class OceanDecoration {
 private OceanDecoration() {}
 public static void decorate(WorldGenLevel level, ChunkAccess chunk, StructureManager manager, ChunkGenerator generator) {
  ChunkPos cp=chunk.getPos();
  var random=RandomSource.create(level.getSeed() ^ (cp.x*341873128712L) ^ (cp.z*132897987541L));
  String profile=OceanWorld.exploration(generator)?"exploration":"oceanblock";
  if(manager.shouldGenerateStructures()) {
   var sets=level.registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
   var setTag=TagKey.create(Registries.STRUCTURE_SET,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,profile));
   var writable=new BoundingBox(cp.getMinBlockX(),level.getMinBuildHeight(),cp.getMinBlockZ(),cp.getMaxBlockX(),level.getMaxBuildHeight()-1,cp.getMaxBlockZ());
   for(Holder<StructureSet> set:sets.getTagOrEmpty(setTag)) for(StructureSet.StructureSelectionEntry entry:set.value().structures()) {
    Structure s=entry.structure().value();
    manager.startsForStructure(SectionPos.bottomOf(chunk),s).forEach(start->start.placeInChunk(level,manager,generator,random,writable,cp));
   }
  }
  growKelp(level,chunk,random);
  var registry=level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
  var tag=TagKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,profile));
  if(profile.equals("exploration")) OceanOreVein.generate(level,chunk,random);
  for(Holder<PlacedFeature> h:registry.getTagOrEmpty(tag)) {
   random.setSeed(level.getSeed()^(cp.x*341873128712L)^(cp.z*132897987541L)^h.unwrapKey().map(key->key.location().toString().hashCode()).orElse(0));
   h.value().placeWithBiomeCheck(level,generator,random,new BlockPos(cp.getMinBlockX(),level.getSeaLevel(),cp.getMinBlockZ()));
  }
 }
 private static void growKelp(WorldGenLevel level,ChunkAccess chunk,RandomSource random) {
  for(int patch=0;patch<16;patch++) {
   int x=chunk.getPos().getMinBlockX()+random.nextInt(16),z=chunk.getPos().getMinBlockZ()+random.nextInt(16);
   String biome=level.getBiome(new BlockPos(x,level.getSeaLevel(),z)).unwrapKey().map(key->key.location().getPath()).orElse("");
   boolean forest=biome.equals("kelp_grove");
   boolean scattered=biome.equals("sand_shelf")||biome.equals("sheltered_lagoon")||biome.equals("coral_garden");
   if(!forest&&!scattered) continue;
   if(scattered&&random.nextInt(8)!=0) continue;
   int floor=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
   if(floor < -650 || floor > 30) continue;
   var base=new BlockPos(x,floor+1,z);
   if(!level.getBlockState(base).is(Blocks.WATER) || !level.getBlockState(base.below()).isSolid()) continue;
   int height=forest?6+random.nextInt(10):3+random.nextInt(6);
   level.setBlock(base,Blocks.KELP.defaultBlockState(),2);
   for(int dy=1;dy<height;dy++) {
    var p=base.above(dy);
    if(!level.getBlockState(p).is(Blocks.WATER)) break;
    level.setBlock(p,Blocks.KELP_PLANT.defaultBlockState(),2);
   }
  }
 }
}
