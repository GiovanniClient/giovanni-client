package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;

@Mixin(value = FogRenderer.class, priority = 200000)
public class NoFogMixin {

    @ModifyExpressionValue(
            method = "getBuffer",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;fogEnabled:Z", opcode = Opcodes.GETSTATIC)
    )
    private boolean giovanni$disableFogWhenConfigured(boolean original) {
        MainConfig config = ConfigManager.getConfig();
        return original && (config == null || config.rc == null || config.rc.cameraAccordion == null || !config.rc.cameraAccordion.NO_FOG_EFFECTS);
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void giovanni$clearFogData(Camera camera, int renderDistance, DeltaTracker deltaTracker, float bossColorModifier, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (!giovanni$isNoFogEnabled()) return;

        FogData fog = cir.getReturnValue();
        fog.environmentalStart = Float.MAX_VALUE;
        fog.environmentalEnd = Float.MAX_VALUE;
        fog.renderDistanceStart = Float.MAX_VALUE;
        fog.renderDistanceEnd = Float.MAX_VALUE;
        fog.skyEnd = Float.MAX_VALUE;
        fog.cloudEnd = Float.MAX_VALUE;
    }

    private boolean giovanni$isNoFogEnabled() {
        MainConfig config = ConfigManager.getConfig();
        return config != null && config.rc != null && config.rc.cameraAccordion != null && config.rc.cameraAccordion.NO_FOG_EFFECTS;
    }
}
