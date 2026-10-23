package com.sanhiruzu.magitemaocean;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded burial-depth ore patches used only by the exploration profile. */
public final class OceanOreVein {
 private OceanOreVein() {}
 private static final TagKey<Block> ORES=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,"exploration_ores"));
 private static final TagKey<Block> HOSTS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,"exploration_ore_hosts"));
 public static void generate(WorldGenLevel level,ChunkAccess chunk,RandomSource random) {
  List<BlockState> ores=new ArrayList<>();
  for(var holder:level.registryAccess().registryOrThrow(Registries.BLOCK).getTagOrEmpty(ORES)) ores.add(holder.value().defaultBlockState());
  if(ores.isEmpty()) return;
  int cx=chunk.getPos().getMinBlockX(),cz=chunk.getPos().getMinBlockZ();
  for(int vein=0;vein<8;vein++) {
   // Keep the 5x5 vein envelope inside this chunk. That makes the per-chunk
   // placement budget meaningful and avoids neighbor chunks writing into it.
   int x=cx+2+random.nextInt(12),z=cz+2+random.nextInt(12);
   int surface=level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x,z)-1;
   int burial=24+random.nextInt(380);
   int y=Math.max(level.getMinBuildHeight()+48,surface-burial);
   BlockState ore=ores.get(random.nextInt(ores.size()));
   for(int i=0;i<9;i++) {
    BlockPos p=new BlockPos(x+random.nextInt(5)-2,y+random.nextInt(5)-2,z+random.nextInt(5)-2);
    BlockState host=level.getBlockState(p);
    if(host.is(HOSTS)) level.setBlock(p,ore,2);
   }
  }
 }
}
