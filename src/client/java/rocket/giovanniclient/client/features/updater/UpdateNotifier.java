package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateData;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

public class UpdateNotifier {

    public MutableText buildMessage(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        boolean isMalicious = safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS;

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

        return Text.literal("\n\n===== ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("GIOVANNI CLIENT").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" =====\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\nNEW UPDATE AVAILABLE! ").formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal("Version: ").formatted(Formatting.WHITE))
                .append(Text.literal(data.getVersionName() + "\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("RatterScanner: ").formatted(Formatting.WHITE))
                .append(Text.literal(safetyStatus.getLabel() + "\n").formatted(statusColor, Formatting.BOLD))
                .append(Text.literal("\nTo install: ").formatted(Formatting.GRAY))
                .append(installBtn)
                .append(Text.literal("\n\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    public MutableText buildNoUpdatesMessage() {
        return Text.literal("\n\n===== ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("GIOVANNI CLIENT").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" =====\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\nYou are up to date! ").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("Latest version installed: " + GiovanniClientClient.MOD_VERSION + " for mc" + GiovanniClientClient.getMcVersion() + "\n").formatted(Formatting.GREEN))
                .append(Text.literal("\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    public MutableText buildInstalledMessage() {
        return Text.literal("\n\n===== ").formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal("GIOVANNI CLIENT").formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" =====\n").formatted(Formatting.GOLD, Formatting.BOLD))
                .append(Text.literal("\nUPDATE INSTALLED! ").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal("Restart required.\n").formatted(Formatting.GREEN))
                .append(Text.literal("\nPlease restart Minecraft to apply the update.\n").formatted(Formatting.WHITE))
                .append(Text.literal("\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));
    }

    public void sendInstalled() {
        Utils.mutableTextToChat(buildInstalledMessage());
    }

    public void sendUpdateAvailable(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        Utils.mutableTextToChat(buildMessage(data, safetyStatus));
    }

    public void sendNoUpdates() {
        Utils.mutableTextToChat(buildNoUpdatesMessage());
    }
}