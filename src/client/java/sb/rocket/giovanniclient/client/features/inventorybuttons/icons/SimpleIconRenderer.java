package sb.rocket.giovanniclient.client.features.inventorybuttons.icons;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class SimpleIconRenderer {
    public static void render(DrawContext ctx, String iconId, int x, int y) {
        // Fallback to paper if ID is invalid
        Identifier id = Identifier.tryParse(iconId);
        if (id == null) {
            ctx.drawItem(new ItemStack(Items.PAPER), x, y);
            return;
        }

        var item = Registries.ITEM.get(id);
        ctx.drawItem(new ItemStack(item), x, y);
    }
}