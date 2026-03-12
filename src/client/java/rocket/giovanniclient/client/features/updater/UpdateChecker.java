package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateCheckResult;
import moe.nea.libautoupdate.source.UpdateStream;
import moe.nea.libautoupdate.updaters.GithubReleasesUpdater;
import rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final GithubReleasesUpdater updater;

    public UpdateChecker(String modId, String version, String mcVersion, Class<?> modClass) {
        this.updater = new GithubReleasesUpdater(modId, version, "GiovanniClient", "giovanni-client", mcVersion, modClass);
    }

    public CompletableFuture<UpdateCheckResult> check() {
        return updater.checkForUpdate(UpdateStream.RELEASE).exceptionally(ex -> {
            Utils.log("Update check failed: " + ex.getMessage());
            ex.printStackTrace();
            return new UpdateCheckResult.UpToDate();
        });
    }

    public void cleanup() {
        updater.cleanup();
    }
}