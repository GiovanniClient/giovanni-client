package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import sb.rocket.giovanniclient.client.config.ConfigManager;
@Mixin(FogRenderer.class)
public class NoFogMixin {

    @ModifyArgs(
            method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")
    )
    private static void giovanni$modifyFogDistances(Args args) {
        if (ConfigManager.getConfig().rc.NO_FOG) {
            // Index 3: environmentalStart
            // Index 4: environmentalEnd
            // Index 5: renderDistanceStart
            // Index 6: renderDistanceEnd
            args.set(3, 998f);
            args.set(4, 999f);
            args.set(5, 998f);
            args.set(6, 999f);
        }
    }
}