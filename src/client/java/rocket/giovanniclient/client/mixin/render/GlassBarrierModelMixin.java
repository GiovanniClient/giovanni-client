package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(BlockStateModelSet.class)
public class GlassBarrierModelMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void giovanni$getGlassModelForBarrier(BlockState state, CallbackInfoReturnable<BlockStateModel> cir) {
        if (Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.GLASS_BARRIER_BLOCKS.get()) && state.is(Blocks.BARRIER)) {
            cir.setReturnValue(((BlockStateModelSet) (Object) this).get(Blocks.GLASS.defaultBlockState()));
        }
    }
}
