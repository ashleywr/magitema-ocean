package com.sanhiruzu.magitemaocean.mixin;
import com.sanhiruzu.magitemaocean.*;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseGeneratorMixin {
    @Inject(method="applyCarvers",at=@At("HEAD"),cancellable=true)
    private void controlledCarvers(CallbackInfo ci) {
        if (OceanWorld.isOcean((NoiseBasedChunkGenerator)(Object)this)) ci.cancel();
    }
}
