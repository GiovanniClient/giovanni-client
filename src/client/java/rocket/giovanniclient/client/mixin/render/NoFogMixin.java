package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.objectweb.asm.Opcodes;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;

@Mixin(FogRenderer.class)
public class NoFogMixin {

    @ModifyExpressionValue(
            method = "getBuffer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;fogEnabled:Z", opcode = Opcodes.GETSTATIC)
    )
    private boolean giovanni$disableFogWhenConfigured(boolean original) {
        MainConfig config = ConfigManager.getConfig();
        return original && (config == null || config.rc == null || config.rc.cameraAccordion == null || !config.rc.cameraAccordion.NO_FOG_EFFECTS);
    }
}
