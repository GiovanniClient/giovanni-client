package sb.rocket.giovanniclient.client.util;

import java.util.Optional;

public class StatusBarUtils {
    public static String statusBarText;

    // 1,000/1,000❤     1000❈ Defense     1,000/1,000✎ Mana
    public static String getStatusBarText() {
        try {
            return ScoreboardUtils.stripMinecraftFormatting(statusBarText);
        } catch(NullPointerException ignored) {
            return "";
        }
    }

    private static String[] getSplittedStatusBar() {
        return Optional.of(getStatusBarText())
                .map(text -> text.replace(",", "").split(" {5}"))
                .orElse(new String[0]);
    }


    public static int getMana() {
        try {
            return Integer.parseInt(getSplittedStatusBar()[2].split("/")[0]);
        } catch (Exception ignored) {
            return 0;
        }
    }
}
