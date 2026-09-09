package rocket.giovanniclient.client.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.events.GiovanniWorldRenderEvent;

@Mixin(LevelRenderer.class)
public abstract class GiovanniLevelRendererMixin {
    @Inject(method = "submitFeatures", at = @At("TAIL"))
    private void giovanni$submitWorldOverlays(LevelRenderState levelRenderState,
                                               SubmitNodeCollector collector,
                                               boolean renderOutline,
                                               CallbackInfo ci) {
        new GiovanniWorldRenderEvent(
                levelRenderState.cameraRenderState,
                new PoseStack(),
                Minecraft.getInstance().getDeltaTracker(),
                collector
        ).render();
    }
}
