package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import sb.rocket.giovanniclient.client.GiovanniClientClient;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.FeatureManager;
import sb.rocket.giovanniclient.client.util.Utils;

public final class ClientLifecycle {

    private ClientLifecycle() {}

    public static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(ConfigManager::shutdown));
    }

    public static void registerClientStarted() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ConfigManager.init();
            Utils.init(ConfigManager.getConfig().dc);

            FeatureManager.registerAll();

            AutoUpdateBootstrap.updateCheckOnStartup();

            Utils.debug("GiovanniClient initialized successfully! Version: " + GiovanniClientClient.MOD_VERSION_NAME);
        });
    }
}
