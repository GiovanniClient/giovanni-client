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
        INSTALLING,
        INSTALLED,
        UP_TO_DATE
    }

    private final AtomicReference<State> state =
            new AtomicReference<>(State.IDLE);
    private final Supplier<AboutConfig> config;

    private final UpdateChecker checker;
    private final UpdateInstaller installer;
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
        this.installer = new UpdateInstaller();
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

        // Allow re-checking from any completed state (IDLE, UP_TO_DATE, UPDATE_AVAILABLE, INSTALLED)
        if (!state.compareAndSet(currentState, State.CHECKING)) {
            Utils.log("Failed to start update check: state changed concurrently. Current state: " + state.get());
            return;
        }

        // Reset previous results for a fresh check
        Utils.log("Starting update flow pipeline from state: " + currentState);
        pendingUpdate = null;
        pendingUpdateData = null;
        safetyStatus = RatterScannerChecker.SafetyStatus.UNCHECKED;

        checker.check()
                .thenCompose(this::handleCheckResult)
                .thenCompose(this::handleSafetyResult)
                .thenCompose(this::handleNotifyAndInstall)
                .exceptionally(ex -> {
                    state.set(State.IDLE);
                    Utils.log("Update flow failed: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }

    private CompletableFuture<UpdateCheckResult> handleCheckResult(UpdateCheckResult result) {
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

    private CompletableFuture<Void> handleNotifyAndInstall(UpdateCheckResult result) {
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
            Utils.log("Automatic installation blocked: Update requires different Minecraft version.");
            return CompletableFuture.completedFuture(null);
        } else {
            Minecraft.getInstance().execute(() ->
                    notifier.sendUpdateAvailable(update.getUpdate(), safetyStatus)
            );
            Utils.log("Notification sent");
        }

        // Continue with normal update logic
        if (isMalicious()) {
            Utils.log("Automatic installation blocked: Update flagged as malicious.");
            return CompletableFuture.completedFuture(null);
        }

        if (shouldAutoInstall()) {
            Utils.log("Proceeding with automatic update installation.");
            return install(update);
        }

        Utils.log("Update notification sent. Manual installation required.");
        return CompletableFuture.completedFuture(null);
    }

    private boolean isMalicious() {
        return safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS;
    }

    private boolean shouldAutoInstall() {
        return config.get().AUTO_DOWNLOAD_UPDATES;
    }

    // -------------------------
    // Manual install (for command)
    // -------------------------

    public CompletableFuture<Void> manualInstall() {
        if (pendingUpdate == null || pendingUpdateData == null) {
            Utils.log("Manual install failed: " + "No pending update found.");
            return CompletableFuture.completedFuture(null);
        }

        if (safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS) {
            Utils.log("Manual install blocked: File is malicious.");
            return CompletableFuture.completedFuture(null);
        }

        return install(pendingUpdate);
    }

    private CompletableFuture<Void> install(PotentialUpdate update) {
        state.set(State.INSTALLING);
        Utils.log("Installing: " + update.getUpdate().getVersionName());

        return installer.install(update)
                .thenRun(() -> {
                    Utils.log("Update installed successfully");
                    state.set(State.INSTALLED);
                    Minecraft.getInstance().execute(notifier::sendInstalled);
                })
                .exceptionally(ex -> {
                    Utils.log("Installation failed: " + ex.getMessage());
                    ex.printStackTrace();
                    state.set(State.IDLE); // CRITICAL: Reset on failure
                    return null;
                });
    }

    public void sendUpdateFoundMessage() {
        if (pendingUpdateData == null) return;
        notifier.sendUpdateAvailable(pendingUpdateData, safetyStatus);
    }

    public void cleanup() {
        Utils.log("Cleaning up UpdateManager resources.");
        checker.cleanup();
    }

    // -------------------------
    // State Queries
    // -------------------------

    public boolean hasUpdate() { return pendingUpdate != null; }

    public boolean isUpdateScheduled() {
        return state.get() == State.INSTALLED;
    }

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