package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.util.StatusBarUtils;

@Mixin(InGameHud.class)
public class ActionBarMixin {
    @Inject(method = "setOverlayMessage(Lnet/minecraft/text/Text;Z)V", at = @At("HEAD"))
    private void setOverlayMessage(Text message, boolean tinted, CallbackInfo info) {
        StatusBarUtils.statusBarText = message.toString();
    }
}