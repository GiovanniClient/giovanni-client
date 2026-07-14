package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(value = DarknessFogEnvironment.class, priority = 200000)
public class NoDarknessMixin {
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void giovanni$skipDarknessFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_FOG_EFFECTS) ci.cancel();
    }

    @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
    private void giovanni$skipDarknessModifier(LivingEntity entity, float darkness, float partialTickTime, CallbackInfoReturnable<Float> cir) {
        var cfg = ConfigManager.getConfig().rc.cameraAccordion;

        if (cfg.NO_FOG_EFFECTS) cir.setReturnValue(darkness);
    }
}
