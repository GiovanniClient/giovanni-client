package rocket.giovanniclient.client.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.features.render.RatRenderStateAccess;
import rocket.giovanniclient.client.features.render.RatReplacer;

@Mixin(ArmorStandRenderer.class)
public class RatArmorStandRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V", at = @At("RETURN"))
    private void giovanni$markRatReplacement(ArmorStand armorStand, ArmorStandRenderState armorStandRenderState, float tickProgress, CallbackInfo ci) {
        ((RatRenderStateAccess) armorStandRenderState).giovanni$setRatReplacement(RatReplacer.shouldReplace(armorStand));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideRatArmorStand(ArmorStandRenderState armorStandRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (((RatRenderStateAccess) armorStandRenderState).giovanni$isRatReplacement()) {
            ci.cancel();
        }
    }
}
