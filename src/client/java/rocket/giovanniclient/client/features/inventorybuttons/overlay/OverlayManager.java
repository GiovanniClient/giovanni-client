package rocket.giovanniclient.client.features.inventorybuttons.overlay;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import rocket.giovanniclient.client.features.inventorybuttons.rei.ReiOverlayHelper;
import rocket.giovanniclient.client.features.inventorybuttons.itemlist.ItemListDragHelper;

public class OverlayManager {
    public static AbstractOverlay activeOverlay = null;

    // item tooltip thing
    // we check if under the mouse should be an item and if so we don't render the tooltip of the item
    // (the button editor is rendered on top of the inventory)
    public static boolean isHoveringPanel(double mx, double my) {
        if (activeOverlay instanceof EditModeOverlay edit) {
            return edit.isMouseOverPanel(mx, my);
        }
        return false;
    }

    public static boolean isHoveringReiEntryList(double mx, double my) {
        return FabricLoader.getInstance().isModLoaded("roughlyenoughitems")
                && ReiOverlayHelper.isMouseOverEntryList(mx, my);
    }

    public static boolean isHoveringItemList(double mx, double my) {
        Minecraft client = Minecraft.getInstance();
        return client.screen != null && ItemListDragHelper.isMouseOverItemList(client.screen, mx, my);
    }
}
