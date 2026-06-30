package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(Gui.class)
public class NoPumpkinMixin {

    @WrapWithCondition(
            method = "renderCameraOverlays",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V")
    )
    private boolean giovanni$shouldRenderOverlay(Gui instance, GuiGraphics guiGraphics, Identifier identifier, float f) {
        // check for Pumpkin Blur, the texture is "textures/misc/pumpkinblur.png"
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_PUMPKIN_OVERLAY && identifier.getPath().contains("pumpkinblur")) {
            return false; // Don't render
        }

        return true; // Render everything else normally
    }
}