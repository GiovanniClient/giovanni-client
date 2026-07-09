package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.Utils;

public final class ClientWorldJoinEvents {
    private static boolean startupUpdateCheckDone;

    private ClientWorldJoinEvents() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Utils.debug("Player joined world.");

            if (startupUpdateCheckDone) {
                Utils.log("Startup update check skipped: already checked this session.");
                return;
            }

            startupUpdateCheckDone = true;
            updateCheckOnStartup();
        });
    }

    private static void updateCheckOnStartup() {
        if (!GiovanniClientClient.isCurrentVersionSupported()) {
            Utils.log("Update check skipped: minecraft version unsupported, message sent.");
            return;
        }

        if (!ConfigManager.getConfig().about.AUTO_CHECK_FOR_UPDATES) {
            Utils.log("Update check skipped: Auto-check disabled in config.");
            return;
        }

        GiovanniClientClient.UPDATE_MANAGER.runUpdateFlow();
    }
}
