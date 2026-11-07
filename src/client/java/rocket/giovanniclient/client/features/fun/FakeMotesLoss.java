package rocket.giovanniclient.client.features.fun;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;
import rocket.giovanniclient.client.util.ScoreboardUtils;
import rocket.giovanniclient.client.util.Utils;

import java.util.regex.Pattern;

public final class FakeMotesLoss extends AbstractFeature {
    private static final Pattern MOTES = Pattern.compile("Motes:\\s+([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]+)?)(?:\\s+.*)?");

    public FakeMotesLoss() {
        ClientReceiveMessageEvents.GAME.register(FakeMotesLoss::onGameMessage);
    }

    private static void onGameMessage(Component message, boolean overlay) {
        if (overlay) return;
        var config = ConfigManager.getConfig().fc;
        if (!config.TROLL_FEATURES || !config.FAKE_MOTES_LOSS) return;
        if (!"The Rift".equals(PlayerLocator.getPlayerArea())) return;
        if (!ScoreboardUtils.stripMinecraftFormatting(message.getString()).stripLeading().startsWith("\u2620 You ")) return;

        for (String line : ScoreboardUtils.getCleanedSidebarLines()) {
            var motes = MOTES.matcher(line.strip());
            if (!motes.matches()) continue;

            Component loss = Component.literal("You died and lost " + motes.group(1) + " motes!")
                    .withStyle(ChatFormatting.RED);
            // GAME fires before the original message is added to chat.
            Minecraft.getInstance().schedule(() -> Utils.mutableComponentToChat(loss));
            return;
        }
    }
}
