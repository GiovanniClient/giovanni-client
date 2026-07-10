package rocket.giovanniclient.client.features.inventorybuttons.icons;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class SimpleIconRenderer {
    public static void render(GuiGraphicsExtractor graphics, String iconId, int x, int y) {
        graphics.item(IconStackCodec.decode(iconId), x, y);
    }
}
