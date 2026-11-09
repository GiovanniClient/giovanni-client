package rocket.giovanniclient.client.features.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import rocket.giovanniclient.client.features.farming.MagicJellybeanHider;

/** Resolves visual block states for chunk meshing in both renderers. */
public final class BlockRenderOverrides {
    private BlockRenderOverrides() {}

    public static BlockState resolve(ClientLevel level, BlockState original, int x, int y, int z) {
        // Each rule sees the original state. The first replacement wins.
        BlockState replacement = MagicJellybeanHider.renderCane(level, original, x, y, z);
        if (replacement != original) return replacement;

        replacement = BlockReplacements.renderState(original);
        if (replacement != original) return replacement;

        return KuudraBarrierRenderOverride.renderState(original, x, y, z);
    }
}
