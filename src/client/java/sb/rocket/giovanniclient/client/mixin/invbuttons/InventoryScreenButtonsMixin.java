package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import sb.rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import sb.rocket.giovanniclient.client.features.inventorybuttons.overlay.NormalModeOverlay;
import sb.rocket.giovanniclient.client.features.inventorybuttons.overlay.TooltipThing;

@Mixin(InventoryScreen.class)
public class InventoryScreenButtonsMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Replace with your actual Edit Mode boolean toggle

        InventoryScreen self = (InventoryScreen) (Object) this;
        if (EditModeState.isEditMode()) {
            TooltipThing.activeOverlay = new EditModeOverlay(self);
        } else {
            TooltipThing.activeOverlay = new NormalModeOverlay(self);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (TooltipThing.activeOverlay != null) {
            TooltipThing.activeOverlay.render(ctx, mouseX, mouseY);
        }
    }
}