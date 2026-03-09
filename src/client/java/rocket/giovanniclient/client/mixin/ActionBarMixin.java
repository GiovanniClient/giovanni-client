package rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.util.StatusBarUtils;

// if i recall correctly we should use an Accessor mixin, this might not be the best solution but for the time being ill keep it

@Mixin(InGameHud.class)
public class ActionBarMixin {
    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void setOverlayMessage(Text message, boolean tinted, CallbackInfo info) {
        StatusBarUtils.statusBarText = message.toString();
    }
}