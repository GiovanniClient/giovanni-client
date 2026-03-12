package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateData;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

public class UpdateNotifier {

    private MutableText buildMessage(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        boolean isMalicious = (safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS);

        Formatting statusColor = switch (safetyStatus) {
            case VERIFIED_SAFE -> Formatting.GREEN;
            case MALICIOUS, OFF -> Formatting.DARK_RED;
            case UNCHECKED -> Formatting.GOLD;
            default -> Formatting.GRAY;
        };

        MutableText installBtn = isMalicious
                ? Text.literal("[DANGEROUS FILE - INSTALLATION BLOCKED]").formatted(Formatting.DARK_RED, Formatting.STRIKETHROUGH)
                : Text.literal("/giovanni-do-update").formatted(Formatting.GREEN, Formatting.UNDERLINE)
                .styled(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/giovanni-do-update")));

        return Text.literal("\n\n====== GIOVANNI CLIENT ======\n").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("\nNEW UPDATE AVAILABLE! ").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal("Version: ").formatted(Formatting.WHITE))
                .append(Text.literal(data.getVersionName() + "\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("RatterScanner: ").formatted(Formatting.WHITE))
                .append(Text.literal(safetyStatus.getLabel() + "\n").formatted(statusColor, Formatting.BOLD))
                .append(Text.literal("\nTo install: ").formatted(Formatting.GRAY))
                .append(installBtn)
                .append(Text.literal("\n\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    private MutableText buildNoUpdatesMessage() {
        return Text.literal("\nGiovanniClient is up to date! " + Utils.rocketEmoji).formatted(Formatting.GREEN);
    }

    private MutableText buildInstalledMessage() {
        return Text.literal("\n\n====== GIOVANNI CLIENT ======\n").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("\nUPDATE INSTALLED! Restart required.\n").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("\nPlease restart Minecraft to apply the update.\n").formatted(Formatting.WHITE))
                .append(Text.literal("\n===========================").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    private MutableText buildDifferentMcVersionMessage(UpdateData data) {
        return Text.literal("\n\n====== GIOVANNI CLIENT ======\n").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("\nUPDATE REQUIRES NEWER MINECRAFT!\n").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal("Update Version: ").formatted(Formatting.WHITE))
                .append(Text.literal(data.getVersionName() + "\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\nPlease update Minecraft to install this update.\n").formatted(Formatting.GRAY))
                .append(Text.literal(GiovanniClientClient.getMcVersion() + " is not supported anymore.\n").formatted(Formatting.GRAY))
                .append(Text.literal("\n===========================").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    public void sendUpdateForDifferentMcVersion(UpdateData data) {
        Utils.mutableTextToChat(buildDifferentMcVersionMessage(data));
    }

    public void sendUpdateAvailable(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        Utils.mutableTextToChat(buildMessage(data, safetyStatus));
    }

    public void sendNoUpdates() {
        Utils.mutableTextToChat(buildNoUpdatesMessage());
    }

    public void sendInstalled() {
        Utils.mutableTextToChat(buildInstalledMessage());
    }
}