package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.features.render.WeatherOverride;

@Mixin(ClientLevel.class)
public abstract class ClientLevelWeatherMixin {
    // Keep the unloaded-chunk guard, but allow rain and snow regardless of the biome.
    @ModifyExpressionValue(method = "getPrecipitationAt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/biome/Biome;getPrecipitationAt(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/biome/Biome$Precipitation;"))
    private Biome.Precipitation giovanni$precipitation(Biome.Precipitation original) {
        return WeatherOverride.mode().isActive() ? WeatherOverride.mode().precipitation() : original;
    }
}
