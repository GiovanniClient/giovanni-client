package rocket.giovanniclient.client.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(GameRenderer.class)
public class NoHurtCamMixin {
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void giovanni$tiltViewWhenHurt(PoseStack matrices, float tickProgress, CallbackInfo ci) {
        if (ConfigManager.getConfig().rc.NO_HURT_CAM) ci.cancel();
    }
}
