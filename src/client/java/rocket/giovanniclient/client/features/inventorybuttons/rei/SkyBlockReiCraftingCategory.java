package rocket.giovanniclient.client.features.inventorybuttons.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiCraftingDisplay;

import java.util.ArrayList;
import java.util.List;

public final class SkyBlockReiCraftingCategory implements DisplayCategory<SkyBlockReiCraftingDisplay> {
    @Override
    public CategoryIdentifier<? extends SkyBlockReiCraftingDisplay> getCategoryIdentifier() {
        return SkyBlockReiCraftingDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("SkyBlock Crafting");
    }

    @Override
    public Renderer getIcon() {
        return EntryStack.of(VanillaEntryTypes.ITEM, new ItemStack(Items.CRAFTING_TABLE));
    }

    @Override
    public List<Widget> setupDisplay(SkyBlockReiCraftingDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int startX = bounds.getCenterX() - 58;
        int startY = bounds.getCenterY() - 27;
        List<EntryIngredient> inputs = display.getInputEntries();
        for (int i = 0; i < 9; i++) {
            int x = startX + (i % 3) * 18;
            int y = startY + (i / 3) * 18;
            widgets.add(Widgets.createSlot(new Point(x, y))
                    .entries(inputs.get(i))
                    .markInput());
        }

        widgets.add(Widgets.createArrow(new Point(startX + 62, startY + 18)));
        widgets.add(Widgets.createResultSlotBackground(new Point(startX + 98, startY + 18)));
        widgets.add(Widgets.createSlot(new Point(startX + 98, startY + 18))
                .entries(display.getOutputEntries().getFirst())
                .markOutput());
        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 74;
    }
}
