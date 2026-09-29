package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rocket.giovanniclient.client.features.render.RatRenderStateAccess;
import rocket.giovanniclient.client.features.render.HiddenStandRenderStateAccess;

@Mixin(ArmorStandRenderState.class)
public class ArmorStandRenderStateMixin implements RatRenderStateAccess, HiddenStandRenderStateAccess {
    @Unique
    private boolean giovanni$ratReplacement;

    @Unique
    private boolean giovanni$hiddenStand;

    @Override
    public void giovanni$setRatReplacement(boolean ratReplacement) {
        this.giovanni$ratReplacement = ratReplacement;
    }

    @Override
    public boolean giovanni$isRatReplacement() {
        return giovanni$ratReplacement;
    }

    @Override
    public void giovanni$setHiddenStand(boolean hidden) {
        this.giovanni$hiddenStand = hidden;
    }

    @Override
    public boolean giovanni$isHiddenStand() {
        return giovanni$hiddenStand;
    }
}
