package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonSlot;
import sb.rocket.giovanniclient.client.features.inventorybuttons.NeuButtonWidget;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonDef;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfig;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsConfigManager;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenButtonsMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void giovanni$addNeuButtons(CallbackInfo ci) {
        InventoryScreen self = (InventoryScreen) (Object) this;
        if (self.getScreenHandler() == null) return;

        int guiX = ((HandledScreenAccessor) self).giovanni$getX();
        int guiY = ((HandledScreenAccessor) self).giovanni$getY();

        boolean edit = UiButtonsConfigManager.isEditMode();

        if (edit) {
            for (InventoryButtonSlot slot : InventoryButtonSlot.all()) {
                UiButtonDef def = UiButtonsConfigManager.findInventoryDef(slot.id())
                        .orElseGet(() -> placeholderDef(slot.id()));

                int x = guiX + slot.relX();
                int y = guiY + slot.relY();

                NeuButtonWidget w = new NeuButtonWidget(x, y, def, true);
                ((ScreenInvoker) (Object) self).giovanni$addDrawableChild(w);
            }
            return;
        }

        UiButtonsConfig cfg = UiButtonsConfigManager.get();
        for (UiButtonDef def : cfg.buttons) {
            if (def == null) continue;
            if (!"inventory".equalsIgnoreCase(def.screen)) continue;

            if (!def.visible) continue;
            if (def.command == null || def.command.isBlank()) continue;

            InventoryButtonSlot slot = InventoryButtonSlot.fromId(def.slot);
            if (slot == null) continue;

            int x = guiX + slot.relX();
            int y = guiY + slot.relY();

            NeuButtonWidget w = new NeuButtonWidget(x, y, def, false);
            ((ScreenInvoker) (Object) self).giovanni$addDrawableChild(w);
        }
    }

    private static UiButtonDef placeholderDef(String slotId) {
        UiButtonDef d = new UiButtonDef();
        d.id = "placeholder_" + slotId;
        d.screen = "inventory";
        d.slot = slotId;

        d.command = "";  // vuoto => placeholder
        d.icon = "";     // IMPORTANT: vuoto (placeholder non deve mostrare icona)

        d.tooltip = "";
        d.w = 18;
        d.h = 18;
        d.visible = true;
        d.enabled = true;
        return d;
    }

}
