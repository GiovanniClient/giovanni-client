package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sb.rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import sb.rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import sb.rocket.giovanniclient.client.features.inventorybuttons.overlay.TooltipThing;

@Mixin(HandledScreen.class)
public class HandledScreenButtonsInputMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(
            Click click,
            boolean doubled,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object) this instanceof InventoryScreen) {
            if (TooltipThing.activeOverlay != null
                    && TooltipThing.activeOverlay.mouseClicked(click, doubled)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            if (TooltipThing.activeOverlay instanceof EditModeOverlay edit) {
                // If the user is typing in the text box, don't let the 'E' key close the inventory!
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.options.inventoryKey.matchesKey(input)) {
                    if ((edit.commandField != null && edit.commandField.isFocused()) ||
                            (edit.iconField != null && edit.iconField.isFocused())) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onRemoved(CallbackInfo ci) {
        // Ensure we are only doing this when the InventoryScreen closes
        if ((Object) this instanceof InventoryScreen) {
            EditModeState.setEditMode(false);
            TooltipThing.activeOverlay = null;
        }
    }

    @Inject(method = "isPointWithinBounds", at = @At("HEAD"), cancellable = true)
    private void blockHoverThroughPanel(int x, int y, int width, int height, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            // If the mouse is over our custom grey panel, tell Minecraft it's NOT over the slot
            if (TooltipThing.isHoveringPanel(pointX, pointY)) {
                cir.setReturnValue(false);
            }
        }
    }
}