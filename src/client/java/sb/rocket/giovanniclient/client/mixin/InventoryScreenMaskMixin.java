package sb.rocket.giovanniclient.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sb.rocket.giovanniclient.client.features.misc.InventoryBackgroundColor;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMaskMixin {

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void maskCraftingArea(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        int x = ((HandledScreenAccessor) this).giovanni$getX();
        int y = ((HandledScreenAccessor) this).giovanni$getY();

        int left   = x + 97;
        int top    = y + 17;
        int right  = x + 172;
        int bottom = y + 53;

        ctx.fill(left, top, right, bottom, InventoryBackgroundColor.get());
    }
}
