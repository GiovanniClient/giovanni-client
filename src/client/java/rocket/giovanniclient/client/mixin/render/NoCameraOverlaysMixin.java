package rocket.giovanniclient.client.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(ScreenEffectRenderer.class)
public class NoCameraOverlaysMixin {
    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void giovanni$renderFireOverlay(PoseStack poseStack, SubmitNodeCollector collector, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_CAMERA_OVERLAYS) ci.cancel();
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void giovanni$renderInWallOverlay(TextureAtlasSprite sprite, PoseStack poseStack, SubmitNodeCollector collector, int color, CallbackInfo ci) {
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_CAMERA_OVERLAYS) ci.cancel();
    }
}
