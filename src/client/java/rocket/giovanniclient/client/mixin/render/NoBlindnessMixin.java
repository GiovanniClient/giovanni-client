package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.environment.BlindnessFogEnvironment;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(value = BlindnessFogEnvironment.class, priority = 200000)
public class NoBlindnessMixin {
    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private void giovanni$skipBlindnessFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_FOG_EFFECTS) ci.cancel();
    }

    @Inject(method = "getModifiedDarkness", at = @At("HEAD"), cancellable = true)
    public void giovanni$skipBlindnessDarknessModifier(LivingEntity entity, float darkness, float partialTickTime, CallbackInfoReturnable<Float> cir) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_FOG_EFFECTS) cir.setReturnValue(darkness);
    }
}
