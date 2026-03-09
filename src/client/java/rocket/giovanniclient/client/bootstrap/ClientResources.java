package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.features.misc.InventoryBackgroundColor;

public final class ClientResources {

    private ClientResources() {}

    private static final Identifier RELOAD_ID =
            Identifier.of(GiovanniClientClient.MODID, "inventory_bg_color_reload");

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return RELOAD_ID;
                    }

                    @Override
                    public void reload(ResourceManager manager) {
                        InventoryBackgroundColor.invalidate();
                    }
                }
        );
    }
}
