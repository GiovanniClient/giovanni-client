package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(InventoryScreen.class)
public class InventoryScreenCraftingSuppressionMixin {

    // in this context this call only draws the "Crafting" string in the inventory
    @Inject(method = "drawForeground", at = @At("HEAD"), cancellable = true)
    protected void drawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        if (ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID) ci.cancel();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void moveSlotsOffscreen(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (self.getScreenHandler() instanceof PlayerScreenHandler handler) {
            if (ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID) {
                // PlayerScreenHandler: 0=result, 1..4=input 2x2
                for (int i = 0; i <= 4 && i < handler.slots.size(); i++) {
                    Slot s = handler.slots.get(i);
                    SlotAccessor a = (SlotAccessor) s;
                    a.setX(-10000);
                    a.setY(-10000);
                }

                if (handler.slots.size() > PlayerScreenHandler.OFFHAND_ID) {
                    Slot offhandSlot = handler.slots.get(PlayerScreenHandler.OFFHAND_ID);
                    SlotAccessor offhandAccessor = (SlotAccessor) offhandSlot;
                    offhandAccessor.setX(-10000);
                    offhandAccessor.setY(-10000);
                }
            }
        }
    }
}
