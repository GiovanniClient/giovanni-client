package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;

import static rocket.giovanniclient.client.GiovanniClientClient.MOD_VERSION_CODE;

public class UpdateManagerV2 {
    private boolean updateScheduled = false;
    private PotentialUpdate pendingUpdate;
    private RatterScannerChecker.SafetyStatus currentSafetyStatus = RatterScannerChecker.SafetyStatus.UNCHECKED;

    private final UpdateContext context = new UpdateContext(
            UpdateSource.gistSource("GiovanniClient", "2570c325b01b3a20637b7d5855afe71d"),
            UpdateTarget.deleteAndSaveInTheSameFolder(GiovanniClientClient.class),
            CurrentVersion.of(MOD_VERSION_CODE),
            "giovanni-client"
    );
    public void prepare() { context.cleanup(); }

    public CompletableFuture<Boolean> checkAsync() {
        String mcVersion = GiovanniClientClient.getCurrentMcVersion();
        String upstreamKey = "mc" + mcVersion;

        return context.checkUpdate(upstreamKey).thenApply(update -> {
            if (update != null && update.isUpdateAvailable()) {
                this.pendingUpdate = update;
                return true;
            }
            this.pendingUpdate = null;
            return false;
        }).exceptionally(ex -> {
            Utils.debug("[Giovanni] No update info found for key: " + upstreamKey);
            return false;
        });
    }

    /**
     * Checks for updates and then automatically chains the RatterScanner API check
     * before showing the update notification to the user.
     */
    public void checkForUpdate() {
        Utils.chat("§7Checking for updates and safety status...");

        checkAsync().thenAccept(available -> {
            if (available && pendingUpdate != null) {
                // hit the RatterScanner API with the new .jar hash
                String hash = pendingUpdate.getUpdate().getSha256();

                RatterScannerChecker.checkHash(hash).thenAccept(status -> {
                    this.currentSafetyStatus = status;
                    // now that we have both metadata and safety info, show the message
                    sendUpdateFoundMessage();
                });
            } else {
                GiovanniClientClient.mc.execute(() -> {
                    Utils.chat("§cNo update found for " + GiovanniClientClient.getCurrentMcVersion());
                });
            }
        });
    }

    public void launchUpdate(PotentialUpdate update) {
        if (update == null || !update.isUpdateAvailable()) return;

        // HARD SECURITY BLOCK
        if (this.currentSafetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS) {
            GiovanniClientClient.mc.execute(() -> {
                Utils.chat("§4§lBLOCKING UPDATE: This file is flagged as MALICIOUS by RatterScanner!");
                Utils.chat("§cPlease report this to the developers immediately.");
            });
            return;
        }

        Utils.debug("§eDownloading update in background...");

        update.launchUpdate().thenRun(() -> GiovanniClientClient.mc.execute(() -> {
            Utils.chat("§a§l[Giovanni] Update downloaded successfully!");
            Utils.chat("§fThe new version will be active next time you start the game.");
            this.updateScheduled = true;
            this.pendingUpdate = null;
        }));
    }

    public void handleCheckCommand() { this.checkForUpdate(); }

    public void handleInstallCommand() {
        if (this.pendingUpdate == null) {
            Utils.chat("§cNo pending update. Run /giovanni-check-update first.");
            return;
        }
        this.launchUpdate(this.pendingUpdate);
    }

    public void handleTestCommand(String testHash) {
        if (this.pendingUpdate == null) {
            Utils.chat("§cNo pending update found in Gist. Run /giovanni-check-update first so the mod has version data to show!");
            return;
        }

        Utils.chat("§7[TEST] Querying RatterScanner for: §f" + testHash);

        RatterScannerChecker.checkHash(testHash).thenAccept(status -> {
            this.currentSafetyStatus = status;
            sendUpdateFoundMessage();
        });
    }

    public void sendUpdateFoundMessage() {
        if (pendingUpdate == null) return;

        UpdateData data = pendingUpdate.getUpdate();

        Formatting statusColor;
        MutableText installBtn;
        boolean isMalicious = currentSafetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS;

        switch (currentSafetyStatus) {
            case VERIFIED_SAFE -> statusColor = Formatting.GREEN;
            case MALICIOUS -> statusColor = Formatting.DARK_RED;
            case UNCHECKED -> statusColor = Formatting.GOLD;
            default -> statusColor = Formatting.GRAY;
        }

        if (isMalicious) {
            installBtn = Text.literal("[DANGEROUS FILE - INSTALLATION BLOCKED]")
                    .formatted(Formatting.DARK_RED, Formatting.STRIKETHROUGH);
        } else {
            installBtn = Text.literal("/giovanni-do-update").formatted(Formatting.GREEN, Formatting.UNDERLINE)
                    .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/giovanni-do-update")));
        }

        MutableText header = Text.literal("\n\n===== ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("GIOVANNI CLIENT").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" =====\n").formatted(Formatting.GOLD, Formatting.BOLD));

        MutableText body = Text.literal("\nNEW UPDATE AVAILABLE! ").formatted(Formatting.RED, Formatting.BOLD)
                .append(Text.literal("Version: ").formatted(Formatting.WHITE))
                .append(Text.literal(data.getVersionName() + "\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("RatterScanner: ").formatted(Formatting.WHITE))
                .append(Text.literal(currentSafetyStatus.getLabel() + "\n").formatted(statusColor, Formatting.BOLD));

        MutableText footer = Text.literal("\nTo install: ").formatted(Formatting.GRAY)
                .append(installBtn)
                .append(Text.literal("\n\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));

        GiovanniClientClient.mc.execute(() -> {
            if (GiovanniClientClient.mc.player != null) {
                GiovanniClientClient.mc.player.sendMessage(header.append(body).append(footer), false);
            }
        });
    }

    public PotentialUpdate getPendingUpdate() { return pendingUpdate; }
    public boolean isUpdateScheduled() { return updateScheduled; }
    public RatterScannerChecker.SafetyStatus getCurrentSafetyStatus() { return currentSafetyStatus; }
}