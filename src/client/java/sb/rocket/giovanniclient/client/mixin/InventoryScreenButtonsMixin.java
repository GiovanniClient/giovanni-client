package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.inventorybuttons.*;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenButtonsMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void addNeuButtons(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen)(Object)this;
        if (self.getScreenHandler() == null) return;

        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();

        UiButtonsConfig cfg = UiButtonsConfigManager.get();

        for (UiButtonDef def : cfg.buttons) {
            if (!def.visible || !def.enabled) continue;
            if (!"inventory".equals(def.screen)) continue;


            // NON renderizzare bottoni vuoti
            if (def.command == null || def.command.isBlank()) continue;

            InventoryButtonSlot slot = InventoryButtonSlot.fromId(def.slot);
            if (slot == null) continue;

            int x = guiX + slot.relX();
            int y = guiY + slot.relY();

            NeuButtonWidget w = new NeuButtonWidget(x, y, def);
            ((ScreenInvoker) this).giovanni$addDrawableChild(w);
        }

    }
}
