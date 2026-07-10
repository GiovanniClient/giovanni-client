package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.PotentialUpdate;
import moe.nea.libautoupdate.UpdateCheckResult;
import moe.nea.libautoupdate.UpdateData;
import net.minecraft.client.Minecraft;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.AboutConfig;
import rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class UpdateManagerV3 {

    private enum State {
        IDLE,
        CHECKING,
        SAFETY_CHECKING,
        UPDATE_AVAILABLE,
        UP_TO_DATE
    }

    private final AtomicReference<State> state =
            new AtomicReference<>(State.IDLE);
    private final Supplier<AboutConfig> config;

    private final UpdateChecker checker;
    private final UpdateNotifier notifier;

    private volatile PotentialUpdate pendingUpdate;
    private volatile UpdateData pendingUpdateData;
    private volatile RatterScannerChecker.SafetyStatus safetyStatus =
            RatterScannerChecker.SafetyStatus.UNCHECKED;

    public UpdateManagerV3(Supplier<AboutConfig> config) {
        this.config = config;
        this.checker = new UpdateChecker(
                GiovanniClientClient.MOD_ID,
                GiovanniClientClient.MOD_VERSION,
                GiovanniClientClient.getMcVersion(),
                GiovanniClientClient.class
        );
        this.notifier = new UpdateNotifier();
    }

    // -------------------------
    // Pipeline
    // -------------------------

    public void runUpdateFlow() {
        // Get current state
        State currentState = state.get();

        // Prevent concurrent checks if already in progress
        if (currentState == State.CHECKING || currentState == State.SAFETY_CHECKING) {
            Utils.log("Update check already in progress. Current state: " + currentState);
            return;
        }

        // Allow re-checking from any completed state (IDLE, UP_TO_DATE, UPDATE_AVAILABLE)
        if (!state.compareAndSet(currentState, State.CHECKING)) {
            Utils.log("Failed to start update check: state changed concurrently. Current state: " + state.get());
            return;
        }

        // Reset previous results for a fresh check
        Utils.log("Starting update flow pipeline from state: " + currentState);
        pendingUpdate = null;
        pendingUpdateData = null;
        safetyStatus = RatterScannerChecker.SafetyStatus.UNCHECKED;

        checker.checkSafely()
                .thenCompose(this::handleCheckOutcome)
                .thenCompose(this::handleCheckResult)
                .thenCompose(this::handleSafetyResult)
                .thenCompose(this::handleNotify)
                .exceptionally(ex -> {
                    state.set(State.IDLE);
                    Utils.log("Update flow failed: " + ex.getMessage());
                    return null;
                });
    }

    private CompletableFuture<UpdateCheckResult> handleCheckOutcome(UpdateChecker.UpdateCheckOutcome outcome) {
        if (outcome.failed()) {
            state.set(State.IDLE);
            Utils.log("Update check skipped: GitHub releases could not be reached.");
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.completedFuture(outcome.result());
    }

    private CompletableFuture<UpdateCheckResult> handleCheckResult(UpdateCheckResult result) {
        if (result == null) {
            return CompletableFuture.completedFuture(null);
        }

        Utils.log("Current Mod Version: " + GiovanniClientClient.MOD_VERSION);
        Utils.log("Current MC Version: " + GiovanniClientClient.getMcVersion());

        return CompletableFuture.completedFuture(switch (result) {
            case UpdateCheckResult.UpdateAvailable u -> {
                var update = u.update();
                pendingUpdate = update;
                pendingUpdateData = update.getUpdate();
                state.set(State.UPDATE_AVAILABLE);
                yield result;
            }

            case UpdateCheckResult.DifferentMcVersionOnly d -> {
                var update = d.latestAvailable(); // Store the update
                pendingUpdate = update;
                pendingUpdateData = update.getUpdate();
                Utils.log("Update available for different MC version: " + update.getUpdate().getVersionName());
                // Don't set UP_TO_DATE here - let safety check run
                yield result;
            }

            case UpdateCheckResult.UpToDate() -> {
                Utils.log("No updates found.");
                state.set(State.UP_TO_DATE);
                yield result;
            }
        });
    }

    private CompletableFuture<UpdateCheckResult> handleSafetyResult(UpdateCheckResult result) {
        if (result == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Extract update from either type
        PotentialUpdate update = null;
        if (result instanceof UpdateCheckResult.UpdateAvailable(PotentialUpdate u)) {
            update = u;
        } else if (result instanceof UpdateCheckResult.DifferentMcVersionOnly(PotentialUpdate latestAvailable)) {
            update = latestAvailable;
        }

        if (update == null) {
            Utils.log("Safety check skipped: No update available");
            return CompletableFuture.completedFuture(result);
        }

        if (!config.get().RATTER_SCANNER_CHECK) {
            Utils.log("Safety check skipped: RatterScanner check disabled in config.");
            safetyStatus = RatterScannerChecker.SafetyStatus.OFF;
            return CompletableFuture.completedFuture(result);
        }

        state.set(State.SAFETY_CHECKING);
        Utils.log("Verifying update hash with RatterScanner for: " + update.getUpdate().getVersionName());
        Utils.log("Hash: " + update.getUpdate().getSha256());

        return RatterScannerChecker.checkHash(update.getUpdate().getSha256())
                .thenApply(status -> {
                    Utils.log("Safety check final result: " + status.name());
                    safetyStatus = status;
                    // Always set to UPDATE_AVAILABLE so notification shows status
                    state.set(State.UPDATE_AVAILABLE);
                    return result;
                })
                .exceptionally(ex -> {
                    Utils.log("Safety check failed with exception: " + ex.getMessage());
                    safetyStatus = RatterScannerChecker.SafetyStatus.ERROR;
                    state.set(State.UPDATE_AVAILABLE);
                    return result;
                });
    }

    private CompletableFuture<Void> handleNotify(UpdateCheckResult result) {
        if (result == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (result instanceof UpdateCheckResult.UpToDate) {
            Minecraft.getInstance().execute(notifier::sendNoUpdates);
            return CompletableFuture.completedFuture(null);
        }

        PotentialUpdate update;
        boolean forDifferentMcVersion = false;

        if (result instanceof UpdateCheckResult.UpdateAvailable(PotentialUpdate u)) {
            update = u;
        } else if (result instanceof UpdateCheckResult.DifferentMcVersionOnly(PotentialUpdate u)) {
            update = u;
            forDifferentMcVersion = true;
        } else {
            update = null;
        }

        if (update == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Send different notifications based on update type
        if (forDifferentMcVersion) {
            Minecraft.getInstance().execute(() ->
                    notifier.sendUpdateForDifferentMcVersion(update.getUpdate())
            );
            Utils.log("Notification sent (different MC version)");
            Utils.log("Update requires a different Minecraft version. Manual download link shown.");
            return CompletableFuture.completedFuture(null);
        } else {
            Minecraft.getInstance().execute(() ->
                    notifier.sendUpdateAvailable(update.getUpdate(), safetyStatus)
            );
            Utils.log("Notification sent");
        }

        Utils.log("Update notification sent. Manual download required.");
        return CompletableFuture.completedFuture(null);
    }

    public void sendUpdateFoundMessage() {
        if (pendingUpdateData == null) return;
        notifier.sendUpdateAvailable(pendingUpdateData, safetyStatus);
    }

    // -------------------------
    // State Queries
    // -------------------------

    public boolean hasUpdate() { return pendingUpdate != null; }

    public State getState() { return state.get(); }

    public PotentialUpdate getPendingUpdate() {
        return pendingUpdate;
    }

    public UpdateData getPendingUpdateData() {
        return pendingUpdateData;
    }

    public RatterScannerChecker.SafetyStatus getSafetyStatus() {
        return safetyStatus;
    }
}
