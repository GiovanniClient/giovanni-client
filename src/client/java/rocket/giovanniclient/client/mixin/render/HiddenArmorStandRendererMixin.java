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
import rocket.giovanniclient.client.features.render.HiddenStandRenderStateAccess;
import rocket.giovanniclient.client.features.render.NamedEntityHide;

@Mixin(ArmorStandRenderer.class)
public class HiddenArmorStandRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V", at = @At("RETURN"))
    private void giovanni$markHiddenStand(ArmorStand stand, ArmorStandRenderState state, float tickProgress, CallbackInfo ci) {
        ((HiddenStandRenderStateAccess) state).giovanni$setHiddenStand(NamedEntityHide.isHiddenStand(stand));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideStand(ArmorStandRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (((HiddenStandRenderStateAccess) state).giovanni$isHiddenStand()) ci.cancel();
    }
}
