package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.OverlayManager;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenButtonsInputMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(
            MouseButtonEvent click,
            boolean doubled,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof InventoryScreen) {
            if (OverlayManager.activeOverlay != null
                    && OverlayManager.activeOverlay.mouseClicked(click, doubled)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            if (OverlayManager.activeOverlay instanceof EditModeOverlay edit) {
                // If the user is typing in the text box, don't let the 'E' key close the inventory!
                Minecraft mc = Minecraft.getInstance();
                if (mc.options.keyInventory.matches(input)) {
                    if ((edit.commandField != null && edit.commandField.isFocused()) ||
                            (edit.iconField != null && edit.iconField.isFocused())) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        // Ensure we are only doing this when the InventoryScreen closes
        if ((Object) this instanceof InventoryScreen) {
            EditModeState.setEditMode(false);
            OverlayManager.activeOverlay = null;
        }
    }

    @Inject(method = "isHovering(IIIIDD)Z", at = @At("HEAD"), cancellable = true)
    private void blockHoverThroughPanel(int x, int y, int width, int height, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            // If the mouse is over our custom grey panel, tell Minecraft it's NOT over the slot
            if (OverlayManager.isHoveringPanel(pointX, pointY)) {
                cir.setReturnValue(false);
            }
        }
    }
}
