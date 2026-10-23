package com.sanhiruzu.magitemaocean;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasureStructure;
import java.util.Optional;

/** Sparse, depth-sited, fully buried original salvage room. */
public final class OceanSalvageStructure extends Structure {
 public static final MapCodec<OceanSalvageStructure> CODEC=Structure.simpleCodec(OceanSalvageStructure::new);
 public OceanSalvageStructure(Structure.StructureSettings settings){super(settings);}
 @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
  ChunkPos chunk=c.chunkPos(); int x=chunk.getMiddleBlockX(),z=chunk.getMiddleBlockZ();
  int floor=c.chunkGenerator().getFirstOccupiedHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())-1;
  if(floor > -120 || floor < -600) return Optional.empty();
  int min=floor,max=floor;
  for(int dx=-6;dx<=6;dx+=3) for(int dz=-6;dz<=6;dz+=3) {
   int h=c.chunkGenerator().getFirstOccupiedHeight(x+dx,z+dz,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())-1;
   min=Math.min(min,h);max=Math.max(max,h);
  }
  if(max-min>22 || min-52<c.heightAccessor().getMinBuildHeight()+16) return Optional.empty();
  int base=min-48;
  if(base+11>=min-32 || base< c.heightAccessor().getMinBuildHeight()+8) return Optional.empty();
  BlockPos origin=new BlockPos(x-4,base,z-4);
  return Optional.of(new GenerationStub(origin,b->b.addPiece(new OceanSalvagePiece(origin))));
 }
 @Override public StructureType<?> type(){return MagitemaOcean.SALVAGE_STRUCTURE.get();}
}
