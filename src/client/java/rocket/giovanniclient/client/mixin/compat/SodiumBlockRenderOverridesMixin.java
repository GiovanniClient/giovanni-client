package rocket.giovanniclient.client.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.features.render.BlockRenderOverrides;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public class SodiumBlockRenderOverridesMixin {
    @Shadow @Final private ClientLevel level;

    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private BlockState giovanni$applyBlockRenderOverrides(BlockState state, int x, int y, int z) {
        return BlockRenderOverrides.resolve(level, state, x, y, z);
    }
}
