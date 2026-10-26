package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.features.render.NamedEntityHide;

@Mixin(EntityRenderDispatcher.class)
public abstract class HiddenPokedexPartMixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void giovanni$hidePokedexPart(Entity entity, Frustum frustum, double x, double y, double z,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (NamedEntityHide.isHiddenDecoration(entity)) cir.setReturnValue(false);
    }
}
