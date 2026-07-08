package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;

@Mixin(AbstractRecipeBookScreen.class)
public abstract class RecipeBookDisableMixin {

    @Inject(method = "initButton", at = @At("HEAD"), cancellable = true)
    private void disableRecipeBook(CallbackInfo ci) {
        if (ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID) {
            ci.cancel();
        }
    }
}
