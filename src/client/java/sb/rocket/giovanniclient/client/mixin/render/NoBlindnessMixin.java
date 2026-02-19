package sb.rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.render.fog.BlindnessEffectFogModifier;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.config.ConfigManager;

@Mixin(BlindnessEffectFogModifier.class)
public class NoBlindnessMixin {
    @Inject(method = "applyDarknessModifier", at = @At("HEAD"), cancellable = true)
    public void giovanni$applyDarknessModifier(LivingEntity cameraEntity, float darkness, float tickProgress, CallbackInfoReturnable<Float> cir) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_BLINDNESS) cir.cancel();
    }
}
