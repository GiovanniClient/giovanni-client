package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.PlayerLocator;

@Mixin(RenderSectionRegion.class)
public class KuudraBarrierSectionCompilerMixin {
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState giovanni$replaceKuudraBarrierState(BlockState state, BlockPos pos) {
        if (!giovanni$shouldRenderAsGlass(pos, state)) return state;

        return pos.getX() == -111 && pos.getY() == 75 && pos.getZ() == -37
                ? Blocks.STAINED_GLASS.pick(DyeColor.GREEN).defaultBlockState()
                : Blocks.STAINED_GLASS.pick(DyeColor.BLACK).defaultBlockState();
    }

    @Unique
    private static boolean giovanni$shouldRenderAsGlass(BlockPos pos, BlockState blockState) {
        return Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.GLASS_BARRIER_BLOCKS.get())
                && PlayerLocator.isPlayerInKuudra()
                && blockState.is(Blocks.BARRIER)
                && pos.getY() == 75;
    }
}
