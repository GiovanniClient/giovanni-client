package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.features.inventorybuttons.InventoryBackgroundColor;

public final class ClientResources {
    private ClientResources() {}

    private static final Identifier RELOAD_ID = Identifier.fromNamespaceAndPath(
            GiovanniClientClient.MOD_ID,
            "inventory_bg_color_reload"
    );

    public static void register() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
                RELOAD_ID,
                (ResourceManagerReloadListener) manager -> InventoryBackgroundColor.invalidate()
        );
    }
}