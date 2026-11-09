package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.features.farming.MagicJellybeanHider;

@Mixin(LevelExtractor.class)
public class MagicJellybeanEntityMixin {
    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideMagicJellybean(Entity entity, Frustum frustum, double x, double y, double z,
                                            CallbackInfoReturnable<Boolean> cir) {
        if (MagicJellybeanHider.shouldHide(entity)) cir.setReturnValue(false);
    }
}
