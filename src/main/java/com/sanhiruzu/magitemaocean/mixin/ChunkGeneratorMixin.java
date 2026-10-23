package com.sanhiruzu.magitemaocean.mixin;
import com.sanhiruzu.magitemaocean.*;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Redirect(method="createStructures",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;possibleStructureSets()Ljava/util/List;"))
    private List<Holder<StructureSet>> allowedSets(ChunkGeneratorStructureState state) {
        ChunkGenerator g = (ChunkGenerator)(Object)this;
        if (!OceanWorld.isOcean(g)) return state.possibleStructureSets();
        var tag = TagKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath(MagitemaOcean.ID, OceanWorld.exploration(g)?"exploration":"oceanblock"));
        return state.possibleStructureSets().stream().filter(h->h.is(tag)).toList();
    }
    @Inject(method="applyBiomeDecoration",at=@At("HEAD"),cancellable=true)
    private void approvedDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager manager, CallbackInfo ci) {
        ChunkGenerator g = (ChunkGenerator)(Object)this;
        if (OceanWorld.isOcean(g)) {
            OceanDecoration.decorate(level,chunk,manager,g);
            ci.cancel();
        }
    }
}
