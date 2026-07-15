package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(FluidStateModelSet.class)
public class LavaFluidModelMixin {
    @Unique
    private FluidModel giovanni$cachedLavaModel;

    @Unique
    private FluidModel giovanni$cachedTranslucentLavaModel;

    @ModifyReturnValue(method = "get", at = @At("RETURN"))
    private FluidModel giovanni$useTranslucentLavaModel(FluidModel original, FluidState state) {
        if (!Boolean.TRUE.equals(ConfigManager.getConfig().rc.kuudraAccordion.TRANSPARENT_LAVA.get())
                || !state.is(FluidTags.LAVA)
                || original.layer() == ChunkSectionLayer.TRANSLUCENT) {
            return original;
        }

        if (original != giovanni$cachedLavaModel) {
            giovanni$cachedLavaModel = original;
            giovanni$cachedTranslucentLavaModel = new FluidModel(
                    ChunkSectionLayer.TRANSLUCENT,
                    original.stillMaterial(),
                    original.flowingMaterial(),
                    original.overlayMaterial(),
                    original.tintSource()
            );
        }

        return giovanni$cachedTranslucentLavaModel;
    }
}
