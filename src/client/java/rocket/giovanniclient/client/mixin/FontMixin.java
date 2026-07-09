package rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(Font.class)
public class FontMixin {
    @ModifyVariable(method = { "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;", "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;" }, at = @At("HEAD"), argsOnly = true, name = "drawShadow")
    private boolean modifyTextShadow(boolean drawShadow) {
        if (ConfigManager.getConfig().miscConfig.NO_TEXT_SHADOWS) return false;

        else return drawShadow;
    }
}