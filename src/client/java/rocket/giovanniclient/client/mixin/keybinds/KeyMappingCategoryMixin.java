package rocket.giovanniclient.client.mixin.keybinds;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.bootstrap.ClientKeybinds;

@Mixin(KeyMapping.Category.class)
public abstract class KeyMappingCategoryMixin {

    @Inject(method = "label()Lnet/minecraft/network/chat/Component;", at = @At("HEAD"), cancellable = true)
    private void giovanni$labelOwnCategory(CallbackInfoReturnable<Component> cir) {
        if ((Object) this == ClientKeybinds.GIOVANNI) {
            cir.setReturnValue(Component.literal("GiovanniClient"));
        }
    }
}
