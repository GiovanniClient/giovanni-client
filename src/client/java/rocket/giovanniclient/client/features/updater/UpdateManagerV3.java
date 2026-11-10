package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.PotentialUpdate;
import moe.nea.libautoupdate.UpdateCheckResult;
import moe.nea.libautoupdate.UpdateData;
import net.minecraft.client.Minecraft;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.MainConfig;
import rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
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
    private final Supplier<MainConfig> config;

    private final UpdateChecker checker;
    private final UpdateNotifier notifier;

    private volatile PotentialUpdate pendingUpdate;
    private volatile UpdateData pendingUpdateData;
    private volatile RatterScannerChecker.SafetyStatus safetyStatus =
            RatterScannerChecker.SafetyStatus.UNCHECKED;

    public UpdateManagerV3(Supplier<MainConfig> config) {
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
        runUpdateFlow(Minecraft.getInstance()::execute);
    }

    public void runUpdateFlow(Executor notificationExecutor) {
        // Get current state
        State currentState = state.get();

        // Prevent concurrent checks if already in progress
        if (currentState == State.CHECKING || currentState == State.SAFETY_CHECKING) {
            Utils.log("Update check already in progress. Current state: " + currentState);
            debugOnClient("Update check already in progress.");
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

        MainConfig currentConfig = config.get();
        boolean useTestRepository = currentConfig != null && currentConfig.debugConfig.USE_TEST_UPDATE_REPOSITORY;
        boolean checkSafety = currentConfig != null && currentConfig.about.RATTER_SCANNER_CHECK;

        checker.checkSafely(useTestRepository)
                .thenCompose(this::handleCheckOutcome)
                .thenCompose(this::handleCheckResult)
                .thenCompose(result -> handleSafetyResult(result, checkSafety))
                .thenAccept(result -> {
                    handleNotify(result, notificationExecutor);
                    state.set(switch (result) {
                        case null -> State.IDLE;
                        case UpdateCheckResult.UpToDate ignored -> State.UP_TO_DATE;
                        case UpdateCheckResult.UpdateAvailable ignored -> State.UPDATE_AVAILABLE;
                        case UpdateCheckResult.DifferentMcVersionOnly ignored -> State.UPDATE_AVAILABLE;
                    });
                })
                .exceptionally(ex -> {
                    state.set(State.IDLE);
                    Utils.log("Update flow failed: " + ex.getMessage());
                    debugOnClient("Update check failed. See latest.log for details.");
                    return null;
                });
    }

    private CompletableFuture<UpdateCheckResult> handleCheckOutcome(UpdateChecker.UpdateCheckOutcome outcome) {
        if (outcome.failed()) {
            Utils.log("Update check skipped: GitHub releases could not be reached.");
            debugOnClient("GitHub release check failed: " + outcome.error().getMessage());
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
                yield result;
            }

            case UpdateCheckResult.DifferentMcVersionOnly d -> {
                var update = d.latestAvailable(); // Store the update
                pendingUpdate = update;
                pendingUpdateData = update.getUpdate();
                Utils.log("Update available for different MC version: " + update.getUpdate().getVersionName());
                yield result;
            }

            case UpdateCheckResult.UpToDate() -> {
                Utils.log("No updates found.");
                yield result;
            }
        });
    }

    private CompletableFuture<UpdateCheckResult> handleSafetyResult(UpdateCheckResult result, boolean checkSafety) {
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

        if (!checkSafety) {
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
                    if (status == RatterScannerChecker.SafetyStatus.ERROR) {
                        debugOnClient("RatterScanner check failed. See latest.log for details.");
                    }
                    return result;
                })
                .exceptionally(ex -> {
                    Utils.log("Safety check failed with exception: " + ex.getMessage());
                    safetyStatus = RatterScannerChecker.SafetyStatus.ERROR;
                    debugOnClient("RatterScanner check failed. See latest.log for details.");
                    return result;
                });
    }

    private void handleNotify(UpdateCheckResult result, Executor notificationExecutor) {
        if (result instanceof UpdateCheckResult.UpToDate) {
            notificationExecutor.execute(notifier::sendNoUpdates);
        } else if (result instanceof UpdateCheckResult.UpdateAvailable(PotentialUpdate update)) {
            var status = safetyStatus;
            notificationExecutor.execute(() -> {
                notifier.sendUpdateAvailable(update.getUpdate(), status);
                Utils.log("Update notification sent. Manual download required.");
            });
        } else if (result instanceof UpdateCheckResult.DifferentMcVersionOnly(PotentialUpdate update)) {
            var status = safetyStatus;
            notificationExecutor.execute(() -> {
                notifier.sendUpdateForDifferentMcVersion(update.getUpdate(), status);
                Utils.log("Update requires a different Minecraft version. Manual download link shown.");
            });
        }
    }

    private static void debugOnClient(String message) {
        Minecraft.getInstance().execute(() -> Utils.debug(message));
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
