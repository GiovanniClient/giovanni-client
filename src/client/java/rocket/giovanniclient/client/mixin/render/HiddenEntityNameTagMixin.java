package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.features.render.NamedEntityHide;

@Mixin(EntityRenderer.class)
public abstract class HiddenEntityNameTagMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V", at = @At("RETURN"))
    private void giovanni$hideEntityNameTag(Entity entity, EntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (NamedEntityHide.isHiddenMob(entity)) {
            state.nameTag = null;
            state.scoreText = null;
        }
    }
}
