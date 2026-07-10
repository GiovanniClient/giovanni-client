package rocket.giovanniclient.client.mixin.render;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.features.render.GlowOverrideManager;

@Mixin(Entity.class)
public abstract class EntityGlowColorOverrideMixin {

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void giovanni$forceGlowColor(CallbackInfoReturnable<Integer> cir) {
        Entity e = (Entity) (Object) this;

        if (GlowOverrideManager.has(e)) {
            cir.setReturnValue(GlowOverrideManager.getColorOrDefault(e));
        }
    }
}
