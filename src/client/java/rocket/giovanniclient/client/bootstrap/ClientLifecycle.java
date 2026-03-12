package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.FeatureManager;
import rocket.giovanniclient.client.util.Utils;

public final class ClientLifecycle {

    private ClientLifecycle() {}

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::shutdown));
    }

    public static void registerClientStarted() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ConfigManager.init();
            Utils.init(ConfigManager.getConfig().debugConfig);

            FeatureManager.registerAll();

            Utils.debug("GiovanniClient initialized successfully! Version: " + GiovanniClientClient.MOD_VERSION);
        });
    }
}
