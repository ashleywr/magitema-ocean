package com.sanhiruzu.magitemaocean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;

/** 9 x 11 x 9 vault; its roof remains at least 32 blocks under the nearby low seafloor. */
public final class OceanSalvagePiece extends StructurePiece {
 private static final ResourceKey<LootTable> LOOT=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID,"chests/salvage"));
 public OceanSalvagePiece(BlockPos p){super(MagitemaOcean.SALVAGE_PIECE.get(),0,new BoundingBox(p.getX(),p.getY(),p.getZ(),p.getX()+8,p.getY()+10,p.getZ()+8));}
 public OceanSalvagePiece(CompoundTag tag){super(MagitemaOcean.SALVAGE_PIECE.get(),tag);}
 @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag){}
 @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
  int x=boundingBox.minX(),y=boundingBox.minY(),z=boundingBox.minZ();
  for(int dx=0;dx<=8;dx++) for(int dy=0;dy<=10;dy++) for(int dz=0;dz<=8;dz++) {
   boolean edge=dx==0||dx==8||dy==0||dy==10||dz==0||dz==8;
   var p=new BlockPos(x+dx,y+dy,z+dz);
   if(!clip.isInside(p)) continue;
   if(edge) level.setBlock(p,(dy==0||dy==10)?Blocks.DEEPSLATE_BRICKS.defaultBlockState():Blocks.PRISMARINE_BRICKS.defaultBlockState(),2);
   else level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
  }
  createChest(level,clip,random,x+4,y+1,z+4,LOOT);
  level.setBlock(new BlockPos(x+4,y+1,z+2),Blocks.LANTERN.defaultBlockState(),2);
 }
}
