package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.tags.FluidTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(FluidRenderer.class)
public class LavaTransparencyMixin {
    @Unique
    private boolean giovanni$renderingLava;

    @Inject(method = "tesselate", at = @At("HEAD"))
    private void giovanni$identifyLava(
            BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci
    ) {
        giovanni$renderingLava = Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.TRANSPARENT_LAVA.get())
                && fluidState.is(FluidTags.LAVA);
    }

    @Inject(method = "tesselate", at = @At("RETURN"))
    private void giovanni$finishRenderingLava(
            BlockAndTintGetter level, BlockPos pos, FluidRenderer.Output output, BlockState blockState, FluidState fluidState, CallbackInfo ci
    ) {
        giovanni$renderingLava = false;
    }

    @ModifyVariable(
            method = "vertex",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int giovanni$applyLavaTransparency(int color) {
        if (!giovanni$renderingLava) return color;
        return ARGB.multiplyAlpha(color, (100 - giovanni$getTransparency()) / 100.0F);
    }

    @Unique
    private int giovanni$getTransparency() {
        return 60;
    }
}
