package sb.rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.util.Utils;

import static sb.rocket.giovanniclient.client.GiovanniClientClient.mc;

@Mixin(RecipeBookScreen.class)
public abstract class RecipeBookDisableMixin {

    @Inject(method = "addRecipeBook", at = @At("HEAD"), cancellable = true)
    private void disableRecipeBook(CallbackInfo ci) {
        if (ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID) {
            if (me.shedaniel.rei.api.client.config.ConfigManager.getInstance().getConfig().areClickableRecipeArrowsEnabled())
                Utils.chat("open Roughly Enough Items (REI) Settings\nAccessibility > \"Clickable Recipe Arrows\" = OFF!");

            Text text = Text.translatable("text.rei.view_recipes_for", "text")
                    .formatted(Formatting.GRAY);  // Style it (optional)
                mc.inGameHud.getChatHud().addMessage(text);
            ci.cancel();
        }
    }
}
