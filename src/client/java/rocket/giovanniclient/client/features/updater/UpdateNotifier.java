package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateData;
import moe.nea.libautoupdate.source.GithubReleaseUpdateData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.util.Utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class UpdateNotifier {
    private static final String CHAT_SAFETY_LABEL = "RatterScanner status: ";
    private static final Pattern MINECRAFT_VERSION_IN_JAR =
            Pattern.compile("-mc(\\d+(?:\\.\\d+)+)(?:-\\d{1,3})?(?:-sources)?\\.jar$");

    private static MutableComponent buildDownloadLink(UpdateData data) {
        String releaseUrl = data instanceof GithubReleaseUpdateData release && release.getHtmlUrl() != null
                ? release.getHtmlUrl() : GiovanniClientClient.RELEASES_URL;
        return Component.literal("[CLICK TO DOWNLOAD]")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(releaseUrl))));
    }

    private MutableComponent buildDownloadLine(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        MutableComponent prompt = safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS
                ? Component.literal("RatterScanner flagged this jar. Verify before downloading: ")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                : Component.literal("GiovanniClient update: ").withStyle(ChatFormatting.GOLD);

        return prompt.append(buildDownloadLink(data))
                .append("\n")
                .append(buildSafetyStatus(CHAT_SAFETY_LABEL, safetyStatus));
    }

    private static Component buildSafetyStatus(String label, RatterScannerChecker.SafetyStatus safetyStatus) {
        ChatFormatting statusColor = switch (safetyStatus) {
            case VERIFIED_SAFE -> ChatFormatting.GREEN;
            case MALICIOUS -> ChatFormatting.DARK_RED;
            case OFF -> ChatFormatting.GRAY;
            case UNCHECKED -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };

        return Component.literal(label).withStyle(ChatFormatting.WHITE)
                .append(Component.literal(safetyStatus.getLabel()).withStyle(statusColor, ChatFormatting.BOLD));
    }

    private MutableComponent buildUpdateToastBody(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        return Component.literal("Version ")
                .append(Component.literal(data.getVersionName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(" - ")
                .append(buildSafetyStatus("RatterScanner: ", safetyStatus));
    }

    private void showToast(Component title, Component message) {
        Minecraft client = Minecraft.getInstance();
        SystemToast.addOrUpdate(
                client.gui.toastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                title,
                message
        );
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_TRADE, 1.0F));
    }

    public void sendUpdateForDifferentMcVersion(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        String minecraftVersion = requiredMinecraftVersion(data);
        String requirement = minecraftVersion == null
                ? "Needs another Minecraft version." : "Requires Minecraft " + minecraftVersion + ".";
        showToast(
                Component.literal("GiovanniClient Update").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Version ")
                        .append(Component.literal(data.getVersionName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append("\n")
                        .append(Component.literal(requirement).withStyle(ChatFormatting.RED))
                        .append("\n")
                        .append(buildSafetyStatus("RatterScanner: ", safetyStatus))
        );
        Utils.mutableComponentToChat(buildDifferentMcVersionChatMessage(data, minecraftVersion, safetyStatus));
    }

    private static Component buildDifferentMcVersionChatMessage(
            UpdateData data, String minecraftVersion, RatterScannerChecker.SafetyStatus safetyStatus) {
        String requirement = minecraftVersion == null
                ? "requires another MC version" : "requires MC " + minecraftVersion;
        return Component.literal("GiovanniClient update found, ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(requirement).withStyle(ChatFormatting.RED))
                .append("\n    ")
                .append(buildDownloadLink(data))
                .append("\n")
                .append(buildSafetyStatus(CHAT_SAFETY_LABEL, safetyStatus));
    }

    private static String requiredMinecraftVersion(UpdateData data) {
        try {
            var match = MINECRAFT_VERSION_IN_JAR.matcher(data.getRawFileName());
            if (match.find()) {
                return match.group(1);
            }
        } catch (URISyntaxException | IllegalStateException ignored) {
            // Use the generic warning when the release URL has no usable filename.
        }
        return null;
    }

    public void sendUpdateAvailable(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        showToast(
                Component.literal("GiovanniClient Update Available").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                buildUpdateToastBody(data, safetyStatus)
        );
        Utils.mutableComponentToChat(buildDownloadLine(data, safetyStatus));
    }

    public void sendNoUpdates() {
        showToast(
                Component.literal("GiovanniClient").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("You are up to date.").withStyle(ChatFormatting.GREEN)
        );
    }
}
