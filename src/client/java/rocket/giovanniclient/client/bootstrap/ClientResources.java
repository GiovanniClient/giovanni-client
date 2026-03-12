package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.features.inventorybuttons.InventoryBackgroundColor;

public final class ClientResources {
    private ClientResources() {}

    private static final Identifier RELOAD_ID = Identifier.of(
            GiovanniClientClient.MOD_ID,
            "inventory_bg_color_reload"
    );

    public static void register() {
        ResourceLoader.get(ResourceType.CLIENT_RESOURCES).registerReloader(
                RELOAD_ID,
                (SynchronousResourceReloader) manager -> InventoryBackgroundColor.invalidate()
        );
    }
}