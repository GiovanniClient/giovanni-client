package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.FeatureManager;
import rocket.giovanniclient.client.features.inventorybuttons.rei.ReiAccessibilityManager;
import rocket.giovanniclient.client.util.Utils;

public final class ClientLifecycle {
    private static boolean shutdownHookRegistered;
    private static boolean clientStartedRegistered;

    private ClientLifecycle() {}

    public static void registerShutdownHook() {
        if (shutdownHookRegistered) return;
        shutdownHookRegistered = true;

        Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::shutdown));
    }

    public static void registerClientStarted(boolean enableFeatures) {
        if (clientStartedRegistered) return;
        clientStartedRegistered = true;

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ConfigManager.init();
            Utils.init(ConfigManager.getConfig().debugConfig);

            if (enableFeatures) {
                if (ConfigManager.getConfig().ibc.INV_BUTTONS_IN_CRAFTING_GRID
                        && FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
                    ReiAccessibilityManager.disableClickableRecipeArrowsIfNeeded();
                }

                FeatureManager.registerAll();
            }

            Utils.debug("GiovanniClient initialized successfully! Version: " + GiovanniClientClient.MOD_VERSION);
        });
    }
}
