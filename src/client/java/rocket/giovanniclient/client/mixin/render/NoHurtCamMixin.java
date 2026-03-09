package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(GameRenderer.class)
public class NoHurtCamMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void giovanni$tiltViewWhenHurt(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (ConfigManager.getConfig().rc.NO_HURT_CAM) ci.cancel();
    }
}
