package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import sb.rocket.giovanniclient.client.features.AbstractFeature;

public class PlayerLocator extends AbstractFeature {

    // Area is the "Island" the player is in, the one written in TAB
    // Location is more specific, and it's on the scoreboard
    private static String CURRENT_PLAYER_LOCATION = "None";
    private static String CURRENT_PLAYER_AREA = "None";
    private int tick = 0;

    @Override
    public void onTick(MinecraftClient client) {
        tick++;

        if (tick % 60 == 0) {
            if (ScoreboardUtils.scoreboardContainsRaw("⏣")) {
                setPlayerLocation(stripLeadingSymbols(ScoreboardUtils.getRawLineContaining("⏣")));
            } else if (ScoreboardUtils.scoreboardContainsRaw("ф")) { // rift
                setPlayerLocation(stripLeadingSymbols(ScoreboardUtils.getRawLineContaining("ф")));
            } else setPlayerLocation("None");

            if (TabListUtils.tabContainsRaw("Area")) {
                setPlayerArea(stripLeadingSymbols(TabListUtils.getCleanLineContaining("Area")).substring(6));
            } else setPlayerArea("None");

        }

        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            if (tick % 200 == 0)
                Utils.debug("You are located in: " + CURRENT_PLAYER_AREA + ", " + CURRENT_PLAYER_LOCATION);
        }
    }

    public static String getPlayerLocation() {
        return CURRENT_PLAYER_LOCATION;
    }

    private void setPlayerLocation(String location) {
        CURRENT_PLAYER_LOCATION = location;
    }

    private static String getPlayerArea() { return CURRENT_PLAYER_AREA; }

    private void setPlayerArea(String area) { CURRENT_PLAYER_AREA = area; }

    public static String stripLeadingSymbols(String input) {
        return input.replaceFirst("^[\\s\\p{So}ф]+", "");
    }

    public static boolean isPlayerIn(String loc) {
        return loc.equals(CURRENT_PLAYER_AREA) || loc.equals(CURRENT_PLAYER_LOCATION);
    }

}
