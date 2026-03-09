package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(InGameHud.class)
public class NoPumpkinMixin {

    @WrapWithCondition(
            method = "renderMiscOverlays",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V")
    )
    private boolean giovanni$shouldRenderOverlay(InGameHud instance, DrawContext context, Identifier texture, float opacity) {
        // check for Pumpkin Blur, the texture is "textures/misc/pumpkinblur.png"
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_PUMPKIN_OVERLAY && texture.getPath().contains("pumpkinblur")) {
            return false; // Don't render
        }

        return true; // Render everything else normally
    }
}