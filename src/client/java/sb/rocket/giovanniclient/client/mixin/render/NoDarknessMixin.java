package sb.rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.render.fog.DarknessEffectFogModifier;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.config.ConfigManager;

@Mixin(DarknessEffectFogModifier.class)
public class NoDarknessMixin {
    // don't touch applyStartEndModifier lmao
    @Inject(method = "applyDarknessModifier", at = @At("HEAD"), cancellable = true)
    private void giovanni$applyDarknessModifier(LivingEntity cameraEntity, float darkness, float tickProgress, CallbackInfoReturnable<Float> cir) {
        var cfg = ConfigManager.getConfig().rc.cameraAccordion;

        if (cfg.NO_DARKNESS) cir.cancel();
    }
}
