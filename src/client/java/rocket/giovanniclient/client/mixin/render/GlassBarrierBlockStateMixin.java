package rocket.giovanniclient.client.mixin.render;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class GlassBarrierBlockStateMixin {
    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void giovanni$renderBarrierBlocks(CallbackInfoReturnable<RenderShape> cir) {
        if (Boolean.TRUE.equals(ConfigManager.getConfig().rc.GLASS_BARRIER_BLOCKS.get())
                && ((BlockState) (Object) this).is(Blocks.BARRIER)) {
            cir.setReturnValue(RenderShape.MODEL);
        }
    }
}
