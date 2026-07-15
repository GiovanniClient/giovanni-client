package rocket.giovanniclient.client.mixin.compat;

import net.minecraft.tags.FluidTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.DefaultFluidRenderer", remap = false)
public class SodiumLavaTransparencyMixin {
    @Shadow
    @Final
    private int[] quadColors;

    @Unique
    private boolean giovanni$renderingTransparentLava;

    @ModifyVariable(method = "render", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FluidState giovanni$identifyTransparentLava(FluidState state) {
        giovanni$renderingTransparentLava = Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.TRANSPARENT_LAVA.get())
                && state.is(FluidTags.LAVA);
        return state;
    }

    @Inject(method = "updateQuad", at = @At("RETURN"))
    private void giovanni$applyLavaTransparency(CallbackInfo ci) {
        if (!giovanni$renderingTransparentLava) return;

        for (int i = 0; i < quadColors.length; i++) {
            quadColors[i] = ARGB.multiplyAlpha(quadColors[i], 0.5F);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void giovanni$finishRenderingLava(CallbackInfo ci) {
        giovanni$renderingTransparentLava = false;
    }
}
