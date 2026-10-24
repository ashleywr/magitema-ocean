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
  var biomeRegistry=level.registryAccess().registryOrThrow(Registries.BIOME);
  var surfaceBiomesTag=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,"surface_biomes"));
  var surfaceBiome=level.getBiome(new BlockPos(cp.getMinBlockX(),level.getSeaLevel(),cp.getMinBlockZ()));
  String surfaceBiomePath=surfaceBiome.unwrapKey().map(key->key.location().getPath()).orElse("");
  var tag=TagKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,profile));
  int i=0;
  if(profile.equals("exploration")) OceanOreVein.generate(level,chunk,random);
  for(Holder<PlacedFeature> h:registry.getTagOrEmpty(tag)) {
   if(profile.equals("exploration") && isIslandScoped(registry,h)) continue;
   if(!fitsSurfaceBiome(registry,biomeRegistry,surfaceBiomesTag,h,profile,surfaceBiomePath)) continue;
   random.setSeed(level.getSeed()^(cp.x*341873128712L)^(cp.z*132897987541L)^(++i*971711L));
   h.value().placeWithBiomeCheck(level,generator,random,new BlockPos(cp.getMinBlockX(),level.getSeaLevel(),cp.getMinBlockZ()));
  }
  if(profile.equals("exploration")) placeIslandFeatures(level,chunk,generator,registry,random);
 }
 private static final String[] ISLAND_BIOMES={"sandy_island","jungle_island","exposed_cliff","volcanic_island"};
 private static boolean isIslandScoped(net.minecraft.core.Registry<PlacedFeature> features,
                                       Holder<PlacedFeature> feature) {
  for(String path:ISLAND_BIOMES) {
   var scope=TagKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,"exploration_"+path));
   for(Holder<PlacedFeature> scoped:features.getTagOrEmpty(scope)) if(scoped.equals(feature)) return true;
  }
  return false;
 }
 private static void placeIslandFeatures(WorldGenLevel level,ChunkAccess chunk,ChunkGenerator generator,
                                         net.minecraft.core.Registry<PlacedFeature> features,RandomSource random) {
  ChunkPos cp=chunk.getPos();
  var scopedFeatures=new LinkedHashMap<String,List<Holder<PlacedFeature>>>();
  for(String path:ISLAND_BIOMES) {
   var scope=TagKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,"exploration_"+path));
   List<Holder<PlacedFeature>> selected=new ArrayList<>();
   features.getTagOrEmpty(scope).forEach(selected::add);
   if(!selected.isEmpty()) scopedFeatures.put(path,selected);
  }
  if(scopedFeatures.isEmpty()) return;

  var anchors=new LinkedHashMap<String,BlockPos>();
  // One fixed 3x3 chunk grid finds broad island summits and shoulders without
  // repeating biome scans for each island class.
  for(int grid=0;grid<9&&anchors.size()<scopedFeatures.size();grid++) {
   int x=cp.getMinBlockX()+2+(grid%3)*6,z=cp.getMinBlockZ()+2+(grid/3)*6;
   var surface=new BlockPos(x,level.getSeaLevel(),z);
   String found=level.getBiome(surface).unwrapKey().map(key->key.location().getPath()).orElse("");
   if(!scopedFeatures.containsKey(found)||anchors.containsKey(found)) continue;
   int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG,x,z);
   if(y>level.getSeaLevel()) anchors.put(found,new BlockPos(x,y,z));
  }
  for(var entry:anchors.entrySet()) {
   for(Holder<PlacedFeature> feature:scopedFeatures.get(entry.getKey())) {
    random.setSeed(level.getSeed()^(cp.x*341873128712L)^(cp.z*132897987541L)^feature.unwrapKey().map(key->key.location().toString().hashCode()).orElse(0));
    feature.value().placeWithBiomeCheck(level,generator,random,entry.getValue());
   }
  }
 }
 private static boolean fitsSurfaceBiome(net.minecraft.core.Registry<PlacedFeature> features,
                                       net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomes,
                                       TagKey<net.minecraft.world.level.biome.Biome> surfaceBiomesTag,
                                       Holder<PlacedFeature> feature,String profile,String currentBiomePath) {
  boolean scoped=false,matched=false;
  for(Holder<net.minecraft.world.level.biome.Biome> biome:biomes.getTagOrEmpty(surfaceBiomesTag)) {
   String path=biome.unwrapKey().map(key->key.location().getPath()).orElse("");
   if(path.isEmpty()) continue;
   var scope=TagKey.create(Registries.PLACED_FEATURE,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,profile+"_"+path));
   for(Holder<PlacedFeature> scopedFeature:features.getTagOrEmpty(scope)) {
    if(scopedFeature.equals(feature)) {
     scoped=true;
     if(path.equals(currentBiomePath)) matched=true;
    }
   }
  }
  return !scoped||matched;
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
