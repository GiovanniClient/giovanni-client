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
        if (!UiButtonsConfigManager.isEditMode()) return;

        HandledScreen<?> self = (HandledScreen<?>)(Object)this;

        // attiva solo per player inventory
        if (!(self instanceof net.minecraft.client.gui.screen.ingame.InventoryScreen)) return;

        int guiX = ((HandledScreenAccessor)(Object)self).giovanni$getX();
        int guiY = ((HandledScreenAccessor)(Object)self).giovanni$getY();

        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = guiX + slot.relX();
            int y = guiY + slot.relY();
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                UiButtonsConfigManager.setSelectedSlot(slot.id());
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
