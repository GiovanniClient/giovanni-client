package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import sb.rocket.giovanniclient.client.features.AbstractFeature;

public class PlayerLocator extends AbstractFeature {

    // Would an enum with ALL the scoreboard locations be overkill? mmh...
    private static String CURRENT_PLAYER_LOCATION = "None";
    private int tick = 0;

    @Override
    public void onWorldLoad(MinecraftClient client) {
        // loop: delay 3 seconds then try to read scoreboard
    }

    // Or maybe do this, I'll think about it
    @Override
    public void onTick(MinecraftClient client) {
        tick++;

        if (tick % 60 == 0) {
            if (ScoreboardUtils.scoreboardContainsRaw("⏣"))
                setPlayerLocation(stripLeadingSymbols(ScoreboardUtils.getRawLineThatContains("⏣")));
        }

        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            if (tick % 200 == 0)
                Utils.debug("You are located in: " + CURRENT_PLAYER_LOCATION);
        }
    }

    public static String getPlayerLocation() {
        return CURRENT_PLAYER_LOCATION;
    }

    private void setPlayerLocation(String location) {
        CURRENT_PLAYER_LOCATION = location;
    }

    public static String stripLeadingSymbols(String input) {
        return input.replaceFirst("^[\\s\\p{So}]+", "");
    }

}
