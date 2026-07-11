package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.inventorybuttons.itemlist.ItemListDragHelper;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.NormalModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.OverlayManager;

@Mixin(InventoryScreen.class)
public class InventoryScreenButtonsMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Replace with your actual Edit Mode boolean toggle

        InventoryScreen self = (InventoryScreen) (Object) this;
        if (EditModeState.isEditMode()) {
            OverlayManager.activeOverlay = new EditModeOverlay(self);
        } else {
            OverlayManager.activeOverlay = new NormalModeOverlay(self);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (OverlayManager.activeOverlay != null) {
            OverlayManager.activeOverlay.render(graphics, mouseX, mouseY);
        }
        ItemListDragHelper.renderDraggedStack(graphics, mouseX, mouseY);
    }
}
