package rocket.giovanniclient.client.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.PlayerLocator;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class SodiumKuudraBarrierMixin {
    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private BlockState giovanni$replaceKuudraBarrierState(BlockState state, int x, int y, int z) {
        if (Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.GLASS_BARRIER_BLOCKS.get())
                && PlayerLocator.isPlayerInKuudra()
                && y == 75
                && state.is(Blocks.BARRIER)) {
            return x == -111 && y == 75 && z == -37
                    ? Blocks.GREEN_STAINED_GLASS.defaultBlockState()
                    : Blocks.BLACK_STAINED_GLASS.defaultBlockState();
        }

        return state;
    }
}
