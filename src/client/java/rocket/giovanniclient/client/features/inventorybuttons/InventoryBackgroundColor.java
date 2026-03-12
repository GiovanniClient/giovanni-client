package rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;

public final class InventoryBackgroundColor {

    private static final Identifier INV_TEX =
            Identifier.of("textures/gui/container/inventory.png");

    private static Integer cached = null;

    public static int get() {
        if (cached != null) return cached;

        try {
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
            try (InputStream in = rm.open(INV_TEX)) {
                NativeImage img = NativeImage.read(in);

                // "Safe" background pixel (in the texture provided, this is solid background)
                int argb = img.getColorArgb(25, 6);

                // Force full alpha (in case the pixel has unusual alpha values)
                cached = argb; //(argb & 0x00FFFFFF) | 0xFF000000;
                img.close();

                return cached;
            }
        } catch (Exception e) {
            return 0xFF000000; // Fallback
        }
    }

    // If you change resource packs in-game, you must invalidate the cache (optional).
    public static void invalidate() {
        cached = null;
    }

    private InventoryBackgroundColor() {}
}