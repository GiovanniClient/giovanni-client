package rocket.giovanniclient.client.features.inventorybuttons;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class InventoryButtonsConfig {

    @Expose
    @ConfigOption(name = "Buttons in Craft Grid", desc = "§cif you turn this OFF, restart the game. Sorry I'm lazy :)")
    @ConfigEditorBoolean
    public boolean INV_BUTTONS_IN_CRAFTING_GRID = true;

    @Expose
    @ConfigOption(name = "Fix REI Dragging", desc = "Works around a REI bug that can stop items from being dragged out of the item list.")
    @ConfigEditorBoolean
    public boolean FIX_REI_DRAGGING = true;

    @Expose
    @ConfigOption(name = "Equipment mod", desc = "If you have a mod that renders equipment inside your inventory, you might want to mess with this")
    @ConfigEditorDropdown
    public Property<EquipmentSide> EQUIPMENT = Property.of(EquipmentSide.Right);

    public enum EquipmentSide {
        None,
        Left,
        Right
    }
}
