package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonSlot;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

@Mixin(HandledScreen.class)
public abstract class HandledScreenInvButtonsInputMixin {

    @Unique
    private static boolean giovanni$isInv(HandledScreen<?> self) {
        return !(self instanceof InventoryScreen);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void giovanni$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (giovanni$isInv(self)) return;
        if (!UiButtonsConfigManager.isEditMode()) return;

        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();

        for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
            int x = guiX + slot.relX();
            int y = guiY + slot.relY();
            if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                UiButtonsConfigManager.setSelectedSlot(slot.id());
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void giovanni$onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (giovanni$isInv(self)) return;
        if (!UiButtonsConfigManager.isEditMode()) return;

        Element focused = self.getFocused();
        if (!(focused instanceof TextFieldWidget)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            cir.setReturnValue(true);
        }
    }
}
