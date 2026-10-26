package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.features.render.WeatherOverride;

@Mixin(Level.class)
public abstract class LevelWeatherMixin {
    @Shadow
    public abstract boolean isClientSide();

    // Override queries instead of stored values so server updates stay available for /gio weather off.
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void giovanni$rainLevel(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (isClientSide() && WeatherOverride.mode().isActive()) {
            cir.setReturnValue(WeatherOverride.mode().rainLevel());
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void giovanni$thunderLevel(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (isClientSide() && WeatherOverride.mode().isActive()) {
            cir.setReturnValue(WeatherOverride.mode().thunderLevel());
        }
    }

    @Inject(method = "canHaveWeather", at = @At("HEAD"), cancellable = true)
    private void giovanni$allowWeatherInAllDimensions(CallbackInfoReturnable<Boolean> cir) {
        if (isClientSide() && WeatherOverride.mode().isActive()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "precipitationAt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation giovanni$precipitation(Biome.Precipitation original) {
        return isClientSide() && WeatherOverride.mode().isActive() ? WeatherOverride.mode().precipitation() : original;
    }
}
