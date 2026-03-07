package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.rei.ReiAccessibilityManager;
import sb.rocket.giovanniclient.client.util.Utils;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookDisableMixin {

    @Inject(method = "addRecipeBook", at = @At("HEAD"), cancellable = true)
    private void disableRecipeBook(CallbackInfo ci) {
        if (ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID) {
            // this is probably the wrong place where to put this code, but it's 3am
            if (ReiAccessibilityManager.areClickableRecipeArrowsEnabled()) {
                Utils.debug("REI Clickable Recipe Arrows is ENABLED");
                Utils.debug("trying to disable it");
                if (ReiAccessibilityManager.disableClickableRecipeArrows())
                    Utils.debug("[REI] Clickable Recipe Arrows disabled successfully.");
                else
                    Utils.debug("[REI] Failed — could not find backing field.");
            }
            else Utils.debug("REI Clickable Recipe Arrows is DISABLED");

            // this is the main reason for this mixin
            ci.cancel();
        }
    }
}
