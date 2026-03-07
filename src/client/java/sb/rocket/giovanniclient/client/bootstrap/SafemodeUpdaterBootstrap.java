package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import sb.rocket.giovanniclient.client.GiovanniClientClient;

import static sb.rocket.giovanniclient.client.GiovanniClientClient.SUPPORTED_VERSIONS;

public final class SafemodeUpdaterBootstrap {
    private SafemodeUpdaterBootstrap() {}

    public static void register() {
        String currentMcVersion = GiovanniClientClient.getCurrentMcVersion();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!SUPPORTED_VERSIONS.contains(currentMcVersion)) {
                if (client.player == null) return;

                MutableText warning = Text.literal("\n\n\n===== ").formatted(Formatting.GOLD, Formatting.BOLD)
                        .append(Text.literal("GIOVANNI CLIENT").formatted(Formatting.AQUA, Formatting.BOLD))
                        .append(Text.literal(" =====\n").formatted(Formatting.GOLD, Formatting.BOLD))
                        .append(Text.literal("\nUNSUPPORTED MC VERSION: ").formatted(Formatting.RED, Formatting.BOLD))
                        .append(Text.literal(currentMcVersion).formatted(Formatting.WHITE))
                        .append(Text.literal("\nFeatures are disabled to prevent crashing.\n").formatted(Formatting.GRAY))
                        .append(Text.literal("\n[CLICK TO CHECK FOR UPDATES]").formatted(Formatting.AQUA, Formatting.BOLD, Formatting.UNDERLINE)
                                .styled(s -> s.withClickEvent(new ClickEvent.RunCommand("/giovanni-check-update"))))
                        .append(Text.literal("\n\n=========================").formatted(Formatting.GOLD, Formatting.BOLD));

                client.player.sendMessage(warning, false);
            }
        });
    }
}