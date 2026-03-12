package rocket.giovanniclient.client.mixin.invbuttons;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.inventorybuttons.InventoryBackgroundColor;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenCraftingMaskMixin {

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void maskCraftingArea(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID) return;
        int x = ((HandledScreenAccessor) this).giovanni$getX();
        int y = ((HandledScreenAccessor) this).giovanni$getY();

        int left   = x + 76;
        int top    = y + 17;
        int right  = x + 173;
        int bottom = y + 79;

        boolean isDebugActive = ConfigManager.getConfig().debugConfig.DEBUG;
        ctx.fill(left, top, right, bottom, isDebugActive ? 0x4411ffcc : InventoryBackgroundColor.get());
    }


}
