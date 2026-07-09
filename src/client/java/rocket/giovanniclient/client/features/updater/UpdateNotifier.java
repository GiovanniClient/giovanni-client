package rocket.giovanniclient.client.features.updater;

import moe.nea.libautoupdate.UpdateData;
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

public class UpdateNotifier {
    private static MutableComponent buildDownloadLink() {
        return Component.literal("[OPEN GITHUB RELEASES]")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.UNDERLINE)
                .withStyle(style -> style.withClickEvent(new ClickEvent.OpenUrl(URI.create(GiovanniClientClient.RELEASES_URL))));
    }

    private MutableComponent buildDownloadLine(RatterScannerChecker.SafetyStatus safetyStatus) {
        MutableComponent prompt = safetyStatus == RatterScannerChecker.SafetyStatus.MALICIOUS
                ? Component.literal("RatterScanner flagged this jar. Verify before downloading: ")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                : Component.literal("GiovanniClient update: ").withStyle(ChatFormatting.GOLD);

        return prompt.append(buildDownloadLink());
    }

    private MutableComponent buildUpdateToastBody(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        ChatFormatting statusColor = switch (safetyStatus) {
            case VERIFIED_SAFE -> ChatFormatting.GREEN;
            case MALICIOUS -> ChatFormatting.DARK_RED;
            case OFF -> ChatFormatting.GRAY;
            case UNCHECKED -> ChatFormatting.GOLD;
            default -> ChatFormatting.GRAY;
        };

        return Component.literal("Version ")
                .append(Component.literal(data.getVersionName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal(" - RatterScanner: ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(safetyStatus.getLabel()).withStyle(statusColor, ChatFormatting.BOLD));
    }

    private void showToast(Component title, Component message) {
        Minecraft client = Minecraft.getInstance();
        SystemToast.addOrUpdate(
                client.getToastManager(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                title,
                message
        );
        client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_TRADE, 1.0F));
    }

    public void sendUpdateForDifferentMcVersion(UpdateData data) {
        showToast(
                Component.literal("GiovanniClient Update").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal("Version ")
                        .append(Component.literal(data.getVersionName()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                        .append(Component.literal(" needs another Minecraft version.").withStyle(ChatFormatting.RED))
        );
        Utils.mutableComponentToChat(
                Component.literal("GiovanniClient update for another MC version: ").withStyle(ChatFormatting.GOLD)
                        .append(buildDownloadLink())
        );
    }

    public void sendUpdateAvailable(UpdateData data, RatterScannerChecker.SafetyStatus safetyStatus) {
        showToast(
                Component.literal("GiovanniClient Update Available").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                buildUpdateToastBody(data, safetyStatus)
        );
        Utils.mutableComponentToChat(buildDownloadLine(safetyStatus));
    }

    public void sendNoUpdates() {
        showToast(
                Component.literal("GiovanniClient").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal("You are up to date.").withStyle(ChatFormatting.GREEN)
        );
    }
}
