package sb.rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import sb.rocket.giovanniclient.client.config.ConfigManager;

@Mixin(GameRenderer.class)
public abstract class NoNauseaMixin {
    @ModifyVariable(method = "renderWorld", at = @At("STORE"), ordinal = 5)
    private float nauseaStrengthToZero(float original) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_NAUSEA)
            return 0.0F;
        return original;
    }
}

