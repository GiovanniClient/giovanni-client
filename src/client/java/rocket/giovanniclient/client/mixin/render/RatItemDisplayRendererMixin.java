package rocket.giovanniclient.client.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.features.render.RatRenderStateAccess;
import rocket.giovanniclient.client.features.render.RatReplacer;

@Mixin(DisplayRenderer.ItemDisplayRenderer.class)
public class RatItemDisplayRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Display$ItemDisplay;Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;F)V", at = @At("RETURN"))
    private void giovanni$markRatReplacement(Display.ItemDisplay display, ItemDisplayEntityRenderState state, float tickProgress, CallbackInfo ci) {
        ((RatRenderStateAccess) state).giovanni$setRatReplacement(RatReplacer.shouldReplace(display));
    }

    @Inject(method = "submitInner(Lnet/minecraft/client/renderer/entity/state/ItemDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V", at = @At("HEAD"), cancellable = true)
    private void giovanni$hideRatItemDisplay(ItemDisplayEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, float interpolationProgress, CallbackInfo ci) {
        if (((RatRenderStateAccess) state).giovanni$isRatReplacement()) {
            ci.cancel();
        }
    }
}
