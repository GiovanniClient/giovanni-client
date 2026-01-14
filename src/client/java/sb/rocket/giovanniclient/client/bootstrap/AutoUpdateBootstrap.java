package sb.rocket.giovanniclient.client.bootstrap;

import moe.nea.libautoupdate.PotentialUpdate;
import sb.rocket.giovanniclient.client.GiovanniClientClient;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;

public final class AutoUpdateBootstrap {

    private AutoUpdateBootstrap() {}

    public static void tryAutoUpdate() {
        boolean autoCheck = ConfigManager.getConfig().about.AUTO_CHECK_FOR_UPDATES;
        boolean autoDownload = ConfigManager.getConfig().about.AUTO_UPDATE;

        if (!autoCheck) return;

        CompletableFuture<PotentialUpdate> checkFuture = GiovanniClientClient.UPDATE_MANAGER.checkForUpdate();

        if (!autoDownload) return;

        checkFuture.thenAccept(potentialUpdate -> {
            if (potentialUpdate != null && potentialUpdate.isUpdateAvailable()) {
                GiovanniClientClient.UPDATE_MANAGER.launchUpdate(potentialUpdate);
            }
        }).exceptionally(ex -> {
            Utils.error("Error during auto-update check chain: ", ex);
            return null;
        });
    }
}
