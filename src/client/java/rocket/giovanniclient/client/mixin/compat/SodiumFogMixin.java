package rocket.giovanniclient.client.mixin.compat;

import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;

import java.lang.reflect.Field;

@Mixin(value = FogRenderer.class, priority = 500)
public class SodiumFogMixin {
    @Unique
    private static Object giovanni$sodiumNoFogParameters;

    @Inject(method = "sodium$getFogParameters", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void giovanni$disableSodiumFog(CallbackInfoReturnable<Object> cir) {
        if (giovanni$isSodiumNoFogEnabled()) cir.setReturnValue(giovanni$getSodiumNoFogParameters());
    }

    @Unique
    private static boolean giovanni$isSodiumNoFogEnabled() {
        MainConfig config = ConfigManager.getConfig();
        return config != null && config.rc != null && config.rc.cameraAccordion != null && config.rc.cameraAccordion.NO_FOG_EFFECTS;
    }

    @Unique
    private static Object giovanni$getSodiumNoFogParameters() {
        if (giovanni$sodiumNoFogParameters != null) return giovanni$sodiumNoFogParameters;

        try {
            Class<?> fogParameters = Class.forName("net.caffeinemc.mods.sodium.client.util.FogParameters");
            Field none = fogParameters.getField("NONE");
            giovanni$sodiumNoFogParameters = none.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Sodium fog parameters", exception);
        }

        return giovanni$sodiumNoFogParameters;
    }
}
