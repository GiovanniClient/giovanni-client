package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocket.giovanniclient.client.features.render.RatRenderStateAccess;

@Mixin(ItemDisplayEntityRenderState.class)
public class ItemDisplayRenderStateMixin implements RatRenderStateAccess {
    @Unique
    private boolean giovanni$ratReplacement;

    @Override
    public void giovanni$setRatReplacement(boolean ratReplacement) {
        this.giovanni$ratReplacement = ratReplacement;
    }

    @Override
    public boolean giovanni$isRatReplacement() {
        return giovanni$ratReplacement;
    }
}
