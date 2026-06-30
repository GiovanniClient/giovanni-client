package rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.util.StatusBarUtils;

// if i recall correctly we should use an Accessor mixin, this might not be the best solution but for the time being ill keep it

@Mixin(Gui.class)
public class ActionBarMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void setOverlayMessage(Component message, boolean tinted, CallbackInfo info) {
        StatusBarUtils.statusBarText = message.toString();
    }
}