package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.features.render.BlockRenderOverrides;

@Mixin(RenderSectionRegion.class)
public class RenderSectionBlockOverridesMixin {
    @Shadow @Final private ClientLevel level;

    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState giovanni$applyBlockRenderOverrides(BlockState state, BlockPos pos) {
        return BlockRenderOverrides.resolve(level, state, pos.getX(), pos.getY(), pos.getZ());
    }
}
