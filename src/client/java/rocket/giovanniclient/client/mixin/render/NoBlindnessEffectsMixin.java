package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.config.MainConfig;

@Mixin(value = LivingEntity.class, priority = 2000)
public class NoBlindnessEffectsMixin {
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideBlindnessPresence(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (giovanni$shouldHideFogEffect(effect)) cir.setReturnValue(false);
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideBlindnessInstance(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (giovanni$shouldHideFogEffect(effect)) cir.setReturnValue(null);
    }

    @Inject(method = "getEffectBlendFactor", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideBlindnessBlend(Holder<MobEffect> effect, float partialTick, CallbackInfoReturnable<Float> cir) {
        if (giovanni$shouldHideFogEffect(effect)) cir.setReturnValue(0.0F);
    }

    @Unique
    private boolean giovanni$shouldHideFogEffect(Holder<MobEffect> effect) {
        MainConfig config = ConfigManager.getConfig();
        if (config == null || config.rc == null || config.rc.cameraAccordion == null || !config.rc.cameraAccordion.NO_FOG_EFFECTS) {
            return false;
        }

        if (!effect.is(MobEffects.BLINDNESS) && !effect.is(MobEffects.DARKNESS)) {
            return false;
        }

        return Minecraft.getInstance().player == (Object) this;
    }
}
