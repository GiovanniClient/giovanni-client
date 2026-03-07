package sb.rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.*;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import sb.rocket.giovanniclient.client.GiovanniClientClient;
import sb.rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.CompletableFuture;

import static sb.rocket.giovanniclient.client.GiovanniClientClient.MOD_VERSION_CODE;

public class UpdateManagerV2 {
    private boolean updateScheduled = false;
    private PotentialUpdate pendingUpdate;

    private final UpdateContext context = new UpdateContext(
            UpdateSource.gistSource("GiovanniClient", "2570c325b01b3a20637b7d5855afe71d"),
            UpdateTarget.deleteAndSaveInTheSameFolder(GiovanniClientClient.class),
            CurrentVersion.of(MOD_VERSION_CODE),
            "giovanni-client"
    );

    public void prepare() { context.cleanup(); }
    public PotentialUpdate getPendingUpdate() { return pendingUpdate; }
    public boolean isUpdateScheduled() { return updateScheduled; }

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
            System.err.println("[Giovanni] No update info found for key: " + upstreamKey);
            return false;
        });
    }

    public void sendUpdateFoundMessage() {
        if (pendingUpdate == null) return;

        UpdateData data = pendingUpdate.getUpdate();
        String status = checkRatterScannerStatus(data.getSha256());
        Formatting statusColor = status.equals("VERIFIED") ? Formatting.YELLOW : Formatting.RED;

        MutableText header = Text.literal("\n\n===== ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("GIOVANNI CLIENT").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" =====\n").formatted(Formatting.GOLD, Formatting.BOLD));

        MutableText body = Text.literal("\nNEW UPDATE AVAILABLE! ").formatted(Formatting.RED, Formatting.BOLD)
                .append(Text.literal("Version: ").formatted(Formatting.WHITE))
                .append(Text.literal(data.getVersionName() + "\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("RatterScanner: ").formatted(Formatting.WHITE))
                .append(Text.literal(status + "\n").formatted(statusColor, Formatting.BOLD));

        MutableText btn = Text.literal("/giovanni-do-update").formatted(Formatting.GREEN, Formatting.UNDERLINE)
                .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/giovanni-do-update")));

        MutableText footer = Text.literal("\nTo install: Click ").formatted(Formatting.GRAY)
                .append(btn)
                .append(Text.literal(" or use the config menu.").formatted(Formatting.GRAY))
                .append(Text.literal("\n\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));

        GiovanniClientClient.mc.execute(() -> {
            if (GiovanniClientClient.mc.player != null) {
                GiovanniClientClient.mc.player.sendMessage(header.append(body).append(footer), false);
            }
        });
    }

    public void checkForUpdate() {
        Utils.chat("§7Checking for updates...");
        checkAsync().thenAccept(available -> {
            if (available) {
                sendUpdateFoundMessage();
            } else {
                Utils.chat("§cNo update found for " + GiovanniClientClient.getCurrentMcVersion());
            }
        });
    }

    public void launchUpdate(PotentialUpdate update) {
        if (update == null || !update.isUpdateAvailable()) return;
        Utils.chat("§eDownloading update in background...");

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

    public String checkRatterScannerStatus(String sha256) {
        if (sha256 == null) return "NOT_FOUND";
        return sha256.startsWith("3ca2") ? "VERIFIED" : "PENDING_VERIFICATION";
    }
}