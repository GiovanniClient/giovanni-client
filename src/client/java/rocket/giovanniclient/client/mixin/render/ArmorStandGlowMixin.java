package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandRenderer.class)
public abstract class ArmorStandGlowMixin {
    @Inject(method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
            at = @At("HEAD"), cancellable = true)
    private void giovanni$outlineEquipmentOnly(ArmorStandRenderState state, boolean bodyVisible, boolean translucent,
                                               boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        // Vanilla outlines the invisible wooden body too. Skip only that base mesh:
        // equipment/head layers still render with the original outlineColor.
        if (state.isInvisible && glowing) cir.setReturnValue(null);
    }
}
