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
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiSimpleRecipeDisplay;

import java.util.ArrayList;
import java.util.List;

public final class SkyBlockReiSimpleRecipeCategory implements DisplayCategory<SkyBlockReiSimpleRecipeDisplay> {
    private final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> category;
    private final Component title;
    private final ItemStack icon;

    public SkyBlockReiSimpleRecipeCategory(CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> category, Component title, ItemStack icon) {
        this.category = category;
        this.title = title;
        this.icon = icon;
    }

    @Override
    public CategoryIdentifier<? extends SkyBlockReiSimpleRecipeDisplay> getCategoryIdentifier() {
        return category;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public Renderer getIcon() {
        return EntryStack.of(VanillaEntryTypes.ITEM, icon);
    }

    @Override
    public List<Widget> setupDisplay(SkyBlockReiSimpleRecipeDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));

        int startX = bounds.x + 8;
        int startY = bounds.y + 8;
        List<EntryIngredient> inputs = display.getInputEntries();
        for (int i = 0; i < inputs.size(); i++) {
            int x = startX + (i % 5) * 18;
            int y = startY + (i / 5) * 18;
            widgets.add(Widgets.createSlot(new Point(x, y))
                    .entries(inputs.get(i))
                    .markInput());
        }

        int arrowX = bounds.x + 104;
        int arrowY = startY + 9;
        widgets.add(Widgets.createArrow(new Point(arrowX, arrowY)));
        widgets.add(Widgets.createResultSlotBackground(new Point(bounds.x + 144, startY + 9)));
        widgets.add(Widgets.createSlot(new Point(bounds.x + 144, startY + 9))
                .entries(display.getOutputEntries().getFirst())
                .markOutput());

        int labelY = bounds.y + 68;
        for (String infoLine : display.infoLines()) {
            widgets.add(Widgets.createLabel(new Point(bounds.x + 8, labelY), Component.literal(infoLine)));
            labelY += 10;
        }
        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 102;
    }

    @Override
    public int getDisplayWidth(SkyBlockReiSimpleRecipeDisplay display) {
        return 176;
    }
}
