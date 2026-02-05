package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonLayout;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

@Mixin(HandledScreen.class)
public abstract class HandledScreenButtonsInputMixin {

    @Unique
    private static boolean giovanni$isInventory(HandledScreen<?> self) {
        return !(self instanceof InventoryScreen);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void giovanni$onMouseClicked(Click click, boolean doubled,
                                         CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (giovanni$isInventory(self)) return;
        if (!UiButtonsConfigManager.isEditMode()) return;

        double mouseX = click.x();
        double mouseY = click.y();

        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();

        for (InventoryButtonLayout slot : InventoryButtonLayout.all()) {
            int x = guiX + slot.relX();
            int y = guiY + slot.relY();

            if (mouseX >= x && mouseX < x + 18
                    && mouseY >= y && mouseY < y + 18) {

                UiButtonsConfigManager.setSelectedSlot(slot.id());
                cir.setReturnValue(true); // consume click
                return;
            }
        }
    }


    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void giovanni$onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        HandledScreen<?> self = (HandledScreen<?>) (Object) this;
        if (giovanni$isInventory(self)) return;
        if (!UiButtonsConfigManager.isEditMode()) return;

        Element focused = self.getFocused();
        if (!(focused instanceof TextFieldWidget)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null
                && mc.options.inventoryKey.matchesKey(input)) {
            cir.setReturnValue(true); // prevent inventory closing while typing
        }

    }
}
