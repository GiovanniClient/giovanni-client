package rocket.giovanniclient.client.mixin.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public class NoFogMixin {

    @Final
    @Shadow
    private GpuBuffer emptyBuffer;

    @Inject(method = "getBuffer", at = @At("HEAD"), cancellable = true)
    private void noFog(FogRenderer.FogMode mode, CallbackInfoReturnable<GpuBufferSlice> cir) {

        // ritorna buffer vuoto SEMPRE
        cir.setReturnValue((emptyBuffer.slice(0L, FogRenderer.FOG_UBO_SIZE)));
    }
}