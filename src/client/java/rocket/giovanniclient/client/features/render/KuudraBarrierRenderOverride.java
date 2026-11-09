package rocket.giovanniclient.client.features.render;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.PlayerLocator;

final class KuudraBarrierRenderOverride {
    private KuudraBarrierRenderOverride() {}

    static BlockState renderState(BlockState original, int x, int y, int z) {
        if (y != 75 || !original.is(Blocks.BARRIER) || !PlayerLocator.isPlayerInKuudra()
                || !Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.GLASS_BARRIER_BLOCKS.get())) {
            return original;
        }

        DyeColor color = x == -111 && z == -37 ? DyeColor.GREEN : DyeColor.BLACK;
        return Blocks.STAINED_GLASS.pick(color).defaultBlockState();
    }
}
