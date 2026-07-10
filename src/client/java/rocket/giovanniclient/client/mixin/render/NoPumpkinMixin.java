package rocket.giovanniclient.client.mixin.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(Gui.class)
public class NoPumpkinMixin {

    @WrapWithCondition(
            method = "extractCameraOverlays",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractTextureOverlay(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/resources/Identifier;F)V")
    )
    private boolean giovanni$shouldRenderOverlay(Gui instance, GuiGraphicsExtractor graphics, Identifier texture, float alpha) {
        // check for Pumpkin Blur, the texture is "textures/misc/pumpkinblur.png"
        if (ConfigManager.getConfig().rc.cameraAccordion.NO_CAMERA_OVERLAYS && texture.getPath().contains("pumpkinblur")) {
            return false; // Don't render
        }

        return true; // Render everything else normally
    }
}
