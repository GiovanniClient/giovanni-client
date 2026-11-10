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

    private static final int TIMEOUT_MILLIS = 10_000;
    private static final String OWNER = "GiovanniClient";
    private static final String RELEASE_REPOSITORY = "giovanni-client";
    private static final String TEST_REPOSITORY = "test-gc-updates-repo";

    private final GithubReleasesUpdater releaseUpdater;
    private final GithubReleasesUpdater testUpdater;

    public UpdateChecker(String modId, String version, String mcVersion, Class<?> modClass) {
        HttpClient.setConnectionPatcher(connection -> {
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", GiovanniClientClient.MOD_ID + "/" + GiovanniClientClient.MOD_VERSION);
            if (connection instanceof HttpURLConnection http) {
                http.setRequestProperty("Accept", "application/vnd.github+json");
            }
        });
        this.releaseUpdater = new GithubReleasesUpdater(modId, version, OWNER, RELEASE_REPOSITORY, mcVersion, modClass);
        this.testUpdater = new GithubReleasesUpdater(modId, version, OWNER, TEST_REPOSITORY, mcVersion, modClass);
    }

    public CompletableFuture<UpdateCheckResult> check(boolean useTestRepository) {
        GithubReleasesUpdater updater = useTestRepository ? testUpdater : releaseUpdater;
        Utils.log("Checking for updates in " + OWNER + "/" + (useTestRepository ? TEST_REPOSITORY : RELEASE_REPOSITORY));
        return updater.checkForUpdate(UpdateStream.RELEASE);
    }

    public CompletableFuture<UpdateCheckOutcome> checkSafely(boolean useTestRepository) {
        return check(useTestRepository).thenApply(UpdateCheckOutcome::success).exceptionally(ex -> {
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
