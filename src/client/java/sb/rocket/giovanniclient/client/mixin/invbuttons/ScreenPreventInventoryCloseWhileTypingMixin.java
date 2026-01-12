package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

@Mixin(Screen.class)
public abstract class ScreenPreventInventoryCloseWhileTypingMixin {

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void giovanni$blockInventoryKeyWhileTyping(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!UiButtonsConfigManager.EDIT_MODE) return;

        Screen self = (Screen)(Object)this;
        if (!(self instanceof InventoryScreen)) return;

        Element focused = self.getFocused();
        if (!(focused instanceof TextFieldWidget)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null && mc.options.inventoryKey.matchesKey(keyCode, scanCode)) {
            // Consuma la pressione della keybind (di default E) così non chiude l’inventario
            cir.setReturnValue(true);
        }
    }
}
