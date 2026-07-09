package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

import java.net.URI;

public class UpdateNotifier {
    private static MutableComponent buildDownloadLink() {
        return Component.literal("[OPEN GITHUB RELEASES]")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(GiovanniClientClient.RELEASES_URL))));
    }

    private MutableComponent buildMessage(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        ChatFormatting statusColor = switch (safetyStatus) {
            case VERIFIED_SAFE -> ChatFormatting.GREEN;
            case MALICIOUS -> ChatFormatting.DARK_RED;
            case OFF -> ChatFormatting.GRAY;
            case UNCHECKED -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };

        MutableComponent downloadPrompt = safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS
                ? Component.literal("RatterScanner flagged this jar. Do not download it unless you verify the release yourself.\nRelease page: ")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                : Component.literal("Download manually: ").withStyle(ChatFormatting.GRAY);

        return Component.literal("\n\n====== GIOVANNI CLIENT ======\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\nNEW UPDATE AVAILABLE! ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Version: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(data.getVersionName() + "\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("RatterScanner: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(safetyStatus.getLabel() + "\n").withStyle(statusColor, ChatFormatting.BOLD))
                .append(Component.literal("\nGiovanniClient will not download updates automatically.\n").withStyle(ChatFormatting.GRAY))
                .append(downloadPrompt)
                .append(buildDownloadLink())
                .append(Component.literal("\n\n=========================").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    private MutableComponent buildNoUpdatesMessage() {
        return Component.literal("\nGiovanniClient is up to date! " + Utils.rocketEmoji).withStyle(ChatFormatting.GREEN);
    }

    private MutableComponent buildDifferentMcVersionMessage(UpdateData data) {
        return Component.literal("\n\n====== GIOVANNI CLIENT ======\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("\nUPDATE REQUIRES NEWER MINECRAFT!\n").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .append(Component.literal("Update Version: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(data.getVersionName() + "\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("\nPlease update Minecraft to use this update.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(GiovanniClientClient.getMcVersion() + " is not supported anymore.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\nDownload manually: ").withStyle(ChatFormatting.GRAY))
                .append(buildDownloadLink())
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
}
