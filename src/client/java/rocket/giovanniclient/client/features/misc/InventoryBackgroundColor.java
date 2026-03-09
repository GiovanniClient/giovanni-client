package rocket.giovanniclient.client.features.misc;

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

                // Pixel “sicuro” di background (nella texture che hai inviato è background pieno)
                int argb = img.getColorArgb(25, 6);

                // forza alpha pieno (in caso il pixel abbia alpha strana)
                cached = argb; //(argb & 0x00FFFFFF) | 0xFF000000;
                img.close();

                return cached;
            }
        } catch (Exception e) {
            return 0xFF000000; // fallback
        }
    }

    // Se cambi resource pack in-game, devi invalidare la cache (opzionale).
    public static void invalidate() {
        cached = null;
    }

    private InventoryBackgroundColor() {}
}
