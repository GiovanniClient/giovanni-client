package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class InventoryButtonsConfig {

    @Expose
    @ConfigOption(name = "No Crafting Text", desc = "Remove the \"Crafting\" text in the inventory")
    @ConfigEditorBoolean
    public boolean NO_CRAFTING_STRING_TOGGLE = false;

    @Expose
    @ConfigOption(name = "No Vanilla Crafting Grid", desc = "Remove the crafting grid in the inventory.\n" +
            "If you toggle ON->OFF its bugged, restart the game to fix (i can't be bothered right now lol)")
    @ConfigEditorBoolean
    public boolean NO_CRAFTING_GRID_TOGGLE = false;


    @Expose
    @ConfigOption(name = "No Recipe Book", desc = "Remove the Recipe Book in the inventory")
    @ConfigEditorBoolean
    public boolean NO_RECIPE_BOOK_TOGGLE = false;
}
