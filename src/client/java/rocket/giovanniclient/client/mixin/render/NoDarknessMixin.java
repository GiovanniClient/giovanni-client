package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(DarknessFogEnvironment.class)
public class NoDarknessMixin {
    // don't touch applyStartEndModifier lmao
    @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
    private void giovanni$applyDarknessModifier(net.minecraft.world.entity.LivingEntity livingEntity, float f, float g, CallbackInfoReturnable<Float> cir) {
        var cfg = ConfigManager.getConfig().rc.cameraAccordion;

        if (cfg.NO_DARKNESS) cir.cancel();
    }
}
