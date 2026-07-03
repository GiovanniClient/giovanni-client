package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(BlindnessFogEnvironment.class)
public class NoBlindnessMixin {
    @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
    public void giovanni$applyDarknessModifier(LivingEntity entity, float darkness, float partialTickTime, CallbackInfoReturnable<Float> cir) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_BLINDNESS) cir.cancel();
    }
}
