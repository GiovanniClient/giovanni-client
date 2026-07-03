package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

public class UpdateNotifier {

    private MutableComponent buildMessage(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        boolean isMalicious = (safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS);

        ChatFormatting statusColor = switch (safetyStatus) {
            case VERIFIED_SAFE -> ChatFormatting.GREEN;
            case MALICIOUS, OFF -> ChatFormatting.DARK_RED;
            case UNCHECKED -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };

        MutableComponent installBtn = isMalicious
                ? Component.literal("[DANGEROUS FILE - INSTALLATION BLOCKED]").withStyle(ChatFormatting.DARK_RED, ChatFormatting.STRIKETHROUGH)
                : Component.literal("/giovanni-do-update").withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE)
                .withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/giovanni-do-update")));

        return Component.literal("\n\n====== GIOVANNI CLIENT ======\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\nNEW UPDATE AVAILABLE! ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Version: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(data.getVersionName() + "\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("RatterScanner: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(safetyStatus.getLabel() + "\n").withStyle(statusColor, ChatFormatting.BOLD))
                .append(Component.literal("\nTo install: ").withStyle(ChatFormatting.GRAY))
                .append(installBtn)
                .append(Component.literal("\n\n=========================").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private MutableComponent buildNoUpdatesMessage() {
        return Component.literal("\nGiovanniClient is up to date! " + Utils.rocketEmoji).withStyle(ChatFormatting.GREEN);
    }

    private MutableComponent buildInstalledMessage() {
        return Component.literal("\n\n====== GIOVANNI CLIENT ======\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\nUPDATE INSTALLED! Restart required.\n").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD))
                .append(Component.literal("\nPlease restart Minecraft to apply the update.\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("\n===========================").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private MutableComponent buildDifferentMcVersionMessage(UpdateData data) {
        return Component.literal("\n\n====== GIOVANNI CLIENT ======\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\nUPDATE REQUIRES NEWER MINECRAFT!\n").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Update Version: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(data.getVersionName() + "\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\nPlease update Minecraft to install this update.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(GiovanniClientClient.getMcVersion() + " is not supported anymore.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n===========================").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    public void sendUpdateForDifferentMcVersion(UpdateData data) {
        Utils.mutableComponentToChat(buildDifferentMcVersionMessage(data));
    }

    public void sendUpdateAvailable(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        Utils.mutableComponentToChat(buildMessage(data, safetyStatus));
    }

    public void sendNoUpdates() {
        Utils.mutableComponentToChat(buildNoUpdatesMessage());
    }

    public void sendInstalled() {
        Utils.mutableComponentToChat(buildInstalledMessage());
    }
}