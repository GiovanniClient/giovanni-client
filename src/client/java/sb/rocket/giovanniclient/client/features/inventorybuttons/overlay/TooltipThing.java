package sb.rocket.giovanniclient.client.features.inventorybuttons.overlay;

public class TooltipThing {
    public static AbstractOverlay activeOverlay = null;

    public static boolean isHoveringPanel(double mx, double my) {
        if (activeOverlay instanceof EditModeOverlay edit) {
            return edit.isMouseOverPanel(mx, my);
        }
        return false;
    }
}