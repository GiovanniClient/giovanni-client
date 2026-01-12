package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonSlot;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

@Mixin(HandledScreen.class)
public abstract class HandledScreenEditorClickMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void giovanni$editorClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!UiButtonsConfigManager.EDIT_MODE) return;

        HandledScreen<?> self = (HandledScreen<?>)(Object)this;

        // attiva solo per player inventory
        if (!(self instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) return;

        int bgW = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundWidth();
        int bgH = ((HandledScreenAccessor)(Object)self).giovanni$getBackgroundHeight();
        int guiX = (self.width - bgW) / 2;
        int guiY = (self.height - bgH) / 2;

        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = guiX + slot.relX();
            int y = guiY + slot.relY();
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                UiButtonsConfigManager.EDIT_SELECTED_SLOT = slot.id();
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
