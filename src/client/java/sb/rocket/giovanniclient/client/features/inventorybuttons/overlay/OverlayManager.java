package sb.rocket.giovanniclient.client.features.inventorybuttons.overlay;

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
}