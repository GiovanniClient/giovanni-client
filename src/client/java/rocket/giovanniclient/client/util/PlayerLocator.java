package rocket.giovanniclient.client.util;

import net.minecraft.client.Minecraft;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;

import java.util.Locale;

public class PlayerLocator extends AbstractFeature {

    // Area is the "Island" the player is in, the one written in TAB
    // Location is more specific, and it's on the scoreboard
    private static final String[] LOCATION_MARKERS = {"\uE067", "\uE020", "ф", "⏣"};
    private static volatile String CURRENT_PLAYER_LOCATION = "None";
    private static volatile String CURRENT_PLAYER_AREA = "None";
    private int tick = 0;

    @Override
    public void onTick(Minecraft client) {
        tick++;

        if (tick % 60 == 0) {
            setPlayerLocation(findPlayerLocation());

            if (TabListUtils.tabContainsRaw("Area")) {
                setPlayerArea(stripLeadingSymbols(TabListUtils.getCleanLineContaining("Area")).substring(6));
            } else setPlayerArea("None");

        }

        if (client != null) {
            if (tick % 200 == 0)
                Utils.debug("You are located in: " + CURRENT_PLAYER_AREA + ", " + CURRENT_PLAYER_LOCATION);
        }
    }

    public static String getPlayerLocation() {
        return CURRENT_PLAYER_LOCATION;
    }

    private void setPlayerLocation(String location) {
        boolean wasInKuudra = isPlayerInKuudra();
        CURRENT_PLAYER_LOCATION = location;
        if (wasInKuudra != isPlayerInKuudra()) ConfigManager.reloadChunks();
    }

    private static String getPlayerArea() { return CURRENT_PLAYER_AREA; }

    private void setPlayerArea(String area) {
        boolean wasInKuudra = isPlayerInKuudra();
        CURRENT_PLAYER_AREA = area;
        if (wasInKuudra != isPlayerInKuudra()) ConfigManager.reloadChunks();
    }

    public static String stripLeadingSymbols(String input) {
        return input.replaceFirst("^[\\s\\p{So}\\p{Co}ф]+", "");
    }

    private static String findPlayerLocation() {
        for (String marker : LOCATION_MARKERS) {
            if (ScoreboardUtils.scoreboardContainsRaw(marker)) {
                return stripLeadingSymbols(ScoreboardUtils.getRawLineContaining(marker));
            }
        }
        return "None";
    }

    public static boolean isPlayerIn(String loc) {
        return loc.equals(CURRENT_PLAYER_AREA) || loc.equals(CURRENT_PLAYER_LOCATION);
    }

    public static boolean isPlayerInKuudra() {
        return CURRENT_PLAYER_AREA.toLowerCase(Locale.ROOT).contains("kuudra")
                || CURRENT_PLAYER_LOCATION.toLowerCase(Locale.ROOT).contains("kuudra");
    }

}
