package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;
import rocket.giovanniclient.client.util.PlayerLocator;

@Mixin(ParticleEngine.class)
public class SmolderingTombParticleMixin {

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideSmolderingTombParticles(ParticleOptions options, double x, double y, double z, double xa, double ya, double za, CallbackInfoReturnable<Particle> cir) {
        if (isEnabled() && (PlayerLocator.isPlayerIn("Smoldering Tomb") || PlayerLocator.isPlayerIn("The Wasteland")) && shouldHide(options.getType())) {
            cir.setReturnValue(null);
        }
    }

    private static boolean isEnabled() {
        MainConfig config = ConfigManager.getConfig();
        return config != null
                && config.sc != null
                && config.sc.blaze != null
                && config.sc.blaze.NO_BLAZE_PARTICLES;
    }

    private static boolean shouldHide(ParticleType<?> type) {
        return type == ParticleTypes.ANGRY_VILLAGER
                || type == ParticleTypes.DRIPPING_LAVA
                || type == ParticleTypes.LARGE_SMOKE
                || type == ParticleTypes.SMALL_FLAME;
    }
}
