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
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiMobDropDisplay;

import java.util.ArrayList;
import java.util.List;

public final class SkyBlockReiMobDropCategory implements DisplayCategory<SkyBlockReiMobDropDisplay> {
    @Override
    public CategoryIdentifier<? extends SkyBlockReiMobDropDisplay> getCategoryIdentifier() {
        return SkyBlockReiMobDropDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Mob Drops");
    }

    @Override
    public Renderer getIcon() {
        return EntryStack.of(VanillaEntryTypes.ITEM, new ItemStack(Items.DIAMOND_SWORD));
    }

    @Override
    public List<Widget> setupDisplay(SkyBlockReiMobDropDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        widgets.add(Widgets.createLabel(new Point(bounds.x + 8, bounds.y + 6), Component.literal(display.mobName())).leftAligned());

        int infoY = bounds.y + 20;
        for (String infoLine : display.infoLines()) {
            widgets.add(Widgets.createLabel(new Point(bounds.x + 8, infoY), Component.literal(infoLine)).leftAligned());
            infoY += 10;
            if (infoY > bounds.y + 58) {
                break;
            }
        }

        List<EntryIngredient> outputs = display.getOutputEntries();
        for (int i = 0; i < outputs.size(); i++) {
            int x = bounds.x + 72 + (i % 5) * 18;
            int y = bounds.y + 22 + (i / 5) * 18;
            var slot = Widgets.createSlot(new Point(x, y))
                    .entries(outputs.get(i))
                    .markOutput();
            List<Component> tooltip = display.drops().get(i).tooltipLines().stream()
                    .<Component>map(Component::literal)
                    .toList();
            widgets.add(tooltip.isEmpty() ? slot : Widgets.withTooltip(slot, tooltip));
        }
        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 108;
    }

    @Override
    public int getDisplayWidth(SkyBlockReiMobDropDisplay display) {
        return 176;
    }
}
