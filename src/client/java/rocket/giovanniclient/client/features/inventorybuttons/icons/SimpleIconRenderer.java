package rocket.giovanniclient.client.features.inventorybuttons.icons;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class SimpleIconRenderer {
    public static void render(GuiGraphicsExtractor graphics, String iconId, int x, int y) {
        // Fallback to paper if ID is invalid
        Identifier id = Identifier.tryParse(iconId);
        if (id == null) {
            graphics.item(new ItemStack(Items.PAPER), x, y);
            return;
        }

        var item = BuiltInRegistries.ITEM.getValue(id);
        graphics.item(new ItemStack(item), x, y);
    }
}