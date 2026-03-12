package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.PotentialUpdate;
import moe.nea.libautoupdate.UpdateCheckResult;
import moe.nea.libautoupdate.UpdateData;
import net.minecraft.client.MinecraftClient;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.AboutConfig;
import rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class UpdateManagerV3 {

    public enum State {
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

    public CompletableFuture<Void> runUpdateFlow() {
        if (!config.get().AUTO_CHECK_FOR_UPDATES) {
            Utils.log(
                    "Update check skipped: Auto-check disabled in config."
            );
            return CompletableFuture.completedFuture(null);
        }
        return runCheck();
    }

    public CompletableFuture<Void> forceCheck() {
        return runCheck();
    }

    private CompletableFuture<Void> runCheck() {
        if (!state.compareAndSet(State.IDLE, State.CHECKING)
                && !state.compareAndSet(State.UP_TO_DATE, State.CHECKING)
                && !state.compareAndSet(
                State.UPDATE_AVAILABLE, State.CHECKING
        )) {
            Utils.log(
                    "Update check already in progress or completed."
            );
            return CompletableFuture.completedFuture(null);
        }

        Utils.log("Starting update flow pipeline...");
        return checker.check()
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
                Utils.log("Update available for different MC version: " + d.latestAvailable().getUpdate().getVersionName());
                state.set(State.UP_TO_DATE);

                yield result;
            }

            case UpdateCheckResult.UpToDate() -> {
                Utils.log("No updates found.");
                state.set(State.UP_TO_DATE);

                yield result;
            }
        });
    }

    private CompletableFuture<UpdateCheckResult> handleSafetyResult(
            UpdateCheckResult result
    ) {
        // Only perform safety check if we have an actual update
        if (!(result instanceof UpdateCheckResult.UpdateAvailable u)) {
            return CompletableFuture.completedFuture(result);
        }

        if (!config.get().RATTER_SCANNER_CHECK) {
            Utils.log(
                    "Safety check skipped: "
                            + "RatterScanner check disabled in config."
            );
            safetyStatus = RatterScannerChecker.SafetyStatus.OFF;
            return CompletableFuture.completedFuture(result);
        }

        state.set(State.SAFETY_CHECKING);
        Utils.log("Verifying update hash with RatterScanner...");

        return RatterScannerChecker.checkHash(u.update().getUpdate().getSha256())
                .thenApply(status -> {
                    Utils.log("Safety check result: " + status.name());
                    safetyStatus = status;
                    state.set(State.UPDATE_AVAILABLE);
                    return result;
                })
                .exceptionally(ex -> {
                    Utils.log("Safety check failed: " + ex.getMessage());
                    safetyStatus = RatterScannerChecker.SafetyStatus.OFF;
                    state.set(State.UPDATE_AVAILABLE);
                    return result;
                });
    }

    private CompletableFuture<Void> handleNotifyAndInstall(UpdateCheckResult result) {
        PotentialUpdate update = switch (result) {
            case UpdateCheckResult.UpdateAvailable(PotentialUpdate u) -> u;
            case UpdateCheckResult.DifferentMcVersionOnly(PotentialUpdate u) -> u;
            default -> null;
        };

        if (update == null) {
            if (result instanceof UpdateCheckResult.UpToDate) {
                MinecraftClient.getInstance().execute(notifier::sendNoUpdates);
            }
            return CompletableFuture.completedFuture(null);
        }

        MinecraftClient.getInstance().execute(() -> notifier.sendUpdateAvailable(update.getUpdate(), safetyStatus));
        Utils.log("Notification sent");

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

    public CompletableFuture<Void> installPending() {
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
        Utils.log("Downloading and installing update: " + update.getUpdate().getVersionName());

        return installer.install(update).thenRun(() -> {
            Utils.log("Update successfully installed. Restart required.");
            state.set(State.INSTALLED);
            MinecraftClient.getInstance().execute(notifier::sendInstalled);
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