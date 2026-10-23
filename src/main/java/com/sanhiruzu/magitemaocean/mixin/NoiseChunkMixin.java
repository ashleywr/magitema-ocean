package com.sanhiruzu.magitemaocean.mixin;
import com.sanhiruzu.magitemaocean.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {
    @Shadow @Final @Mutable private Aquifer aquifer;
    @Inject(method="<init>",at=@At("RETURN"))
    private void oceanFluids(int cells, RandomState state, int x, int z, NoiseSettings noise,
        DensityFunctions.BeardifierOrMarker beard, NoiseGeneratorSettings settings,
        Aquifer.FluidPicker original, Blender blender, CallbackInfo ci) {
        if (OceanWorld.isOcean(settings)) aquifer = Aquifer.createDisabled(OceanWorld.fluids(state));
    }
}
