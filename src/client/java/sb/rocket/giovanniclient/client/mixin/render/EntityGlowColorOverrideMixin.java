package sb.rocket.giovanniclient.client.mixin.render;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.features.render.GlowOverrideManager;

@Mixin(Entity.class)
public abstract class EntityGlowColorOverrideMixin {

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void giovanni$forceGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity e = (Entity) (Object) this;

        Integer rgb = GlowOverrideManager.getColorOrDefault(e);
        if (rgb != null) {
            cir.setReturnValue(rgb);
        }
    }
}
