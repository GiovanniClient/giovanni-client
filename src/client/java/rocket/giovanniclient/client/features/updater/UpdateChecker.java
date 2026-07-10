package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateCheckResult;
import moe.nea.libautoupdate.source.UpdateStream;
import moe.nea.libautoupdate.updaters.GithubReleasesUpdater;
import moe.nea.libautoupdate.util.HttpClient;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {

    private final GithubReleasesUpdater updater;

    public UpdateChecker(String modId, String version, String mcVersion, Class<?> modClass) {
        HttpClient.setConnectionPatcher(connection -> {
            connection.setRequestProperty("User-Agent", GiovanniClientClient.MOD_ID + "/" + GiovanniClientClient.MOD_VERSION);
            if (connection instanceof HttpURLConnection http) {
                http.setRequestProperty("Accept", "application/vnd.github+json");
            }
        });
        this.updater = new GithubReleasesUpdater(modId, version, "GiovanniClient", "giovanni-client", mcVersion, modClass);
    }

    public CompletableFuture<UpdateCheckResult> check() {
        return updater.checkForUpdate(UpdateStream.RELEASE);
    }

    public CompletableFuture<UpdateCheckOutcome> checkSafely() {
        return check().thenApply(UpdateCheckOutcome::success).exceptionally(ex -> {
            Utils.log("Update check failed: " + ex.getMessage());
            return UpdateCheckOutcome.failure(ex);
        });
    }

    public record UpdateCheckOutcome(UpdateCheckResult result, Throwable error) {
        public static UpdateCheckOutcome success(UpdateCheckResult result) {
            return new UpdateCheckOutcome(result, null);
        }

        public static UpdateCheckOutcome failure(Throwable error) {
            return new UpdateCheckOutcome(null, error);
        }

        public boolean failed() {
            return error != null;
        }
    }
}
