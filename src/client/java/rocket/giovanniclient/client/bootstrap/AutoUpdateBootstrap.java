package rocket.giovanniclient.client.bootstrap;

import moe.nea.libautoupdate.PotentialUpdate;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.ConfigManager;

public final class AutoUpdateBootstrap {

    private AutoUpdateBootstrap() {}

    public static void updateCheckOnStartup() {
        boolean autoCheck = ConfigManager.getConfig().about.AUTO_CHECK_FOR_UPDATES;
        boolean autoDownload = ConfigManager.getConfig().about.AUTO_UPDATE;

        if (!autoCheck) return;

        GiovanniClientClient.UPDATE_MANAGER.checkAsync().thenAccept(available -> {
            if (available && autoDownload) {
                PotentialUpdate update = GiovanniClientClient.UPDATE_MANAGER.getPendingUpdate();
                if (update != null) {
                    GiovanniClientClient.UPDATE_MANAGER.launchUpdate(update);
                }
            }
        }).exceptionally(ex -> {
            System.err.println("[Giovanni] Auto-update background check failed: " + ex.getMessage());
            return null;
        });
    }
}