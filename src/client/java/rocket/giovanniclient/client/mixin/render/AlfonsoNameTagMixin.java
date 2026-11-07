package rocket.giovanniclient.client.mixin.render;

import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.features.fun.Alfonso;

@Mixin(ArmorStandRenderer.class)
public class AlfonsoNameTagMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ArmorStand;Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;F)V", at = @At("RETURN"))
    private void giovanni$renameAlfonso(ArmorStand stand, ArmorStandRenderState state, float tickProgress, CallbackInfo ci) {
        state.nameTag = Alfonso.getNameTag(stand, state.nameTag);
    }
}
