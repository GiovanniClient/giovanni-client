package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonsConfig;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Unique
    private final InventoryButtonsConfig ibc = ConfigManager.getConfig().ibc;

    // in this context this call only draws the "Crafting" string in the inventory
    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    protected void drawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (ibc.NO_CRAFTING_STRING_TOGGLE) ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void moveCraftingSlotsOffscreen(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen)(Object)this;

        if (self.getScreenHandler() instanceof PlayerScreenHandler handler) {
            // PlayerScreenHandler: 0=result, 1..4=input 2x2
            for (int i = 0; i <= 4 && i < handler.slots.size(); i++) {
                Slot s = handler.slots.get(i);
                SlotAccessor a = (SlotAccessor) s;
                a.setX(-10000);
                a.setY(-10000);
            }
        }
    }
}