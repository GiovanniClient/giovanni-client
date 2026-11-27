package rocket.giovanniclient.client.features.dungeons;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.ScoreboardUtils;

import java.util.regex.Pattern;

public final class SpiritLeap extends AbstractFeature {
    private static final Pattern CONFIRMED_LEAP = Pattern.compile("You have teleported to ([A-Za-z0-9_]{1,16})!");

    public SpiritLeap() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay || !ConfigManager.getConfig().dungeon.announceLeaps) return;

            var match = CONFIRMED_LEAP.matcher(ScoreboardUtils.stripMinecraftFormatting(message.getString()).strip());
            if (!match.matches()) return;

            Minecraft client = Minecraft.getInstance();
            var connection = client.getConnection();
            if (connection != null && LeapTargets.isInDungeon(client)) {
                connection.sendCommand("pc Leaped to " + match.group(1) + "!");
            }
        });
    }

    public static Screen replaceScreen(Screen next) {
        if (!(next instanceof ContainerScreen original) || next instanceof LeapScreen) return next;

        String title = ScoreboardUtils.stripMinecraftFormatting(next.getTitle().getString());
        if (!title.equals("Spirit Leap") && !title.equals("Teleport to Player")) return next;
        if (!ConfigManager.getConfig().dungeon.leapMenu) return next;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.player.containerMenu != original.getMenu()) return next;
        if (title.equals("Teleport to Player") && !LeapTargets.isInDungeon(client)) return next;

        return new LeapScreen(original, client.player.getInventory());
    }
}
