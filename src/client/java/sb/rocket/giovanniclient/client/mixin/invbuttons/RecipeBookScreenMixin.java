package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonsConfig;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookScreenMixin {
    @Unique
    private final InventoryButtonsConfig ibc = ConfigManager.getConfig().ibc;

    @Inject(method = "addRecipeBook", at = @At("HEAD"), cancellable = true)
    private void disableRecipeBook(CallbackInfo ci) {
        if (ibc.NO_RECIPE_BOOK_TOGGLE) ci.cancel(); // niente bottone + niente widget recipe book
    }
}
