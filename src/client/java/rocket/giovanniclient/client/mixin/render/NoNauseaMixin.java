package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(GameRenderer.class)
public abstract class NoNauseaMixin {
    @ModifyVariable(method = "renderLevel", at = @At("STORE"), name = "spinningEffectIntensity")
    private float nauseaStrengthToZero(float spinningEffectIntensity) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_NAUSEA)
            return 0.0F;
        return spinningEffectIntensity;
    }
}

