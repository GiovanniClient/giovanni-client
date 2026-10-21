package rocket.giovanniclient.client.mixin.keybinds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.bootstrap.ClientKeybinds;

@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {

    // Labels belong only to the local controls UI, never to the global language table.
    @WrapOperation(
            method = "<init>(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsScreen;Lnet/minecraft/client/Minecraft;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                    ordinal = 0))
    private MutableComponent giovanni$labelOwnKeybinds(String key, Operation<MutableComponent> original) {
        String name = ClientKeybinds.getDisplayName(key);
        return name != null ? Component.literal(name) : original.call(key);
    }
}
