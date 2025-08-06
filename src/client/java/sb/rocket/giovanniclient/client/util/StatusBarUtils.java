package sb.rocket.giovanniclient.client.util;

public class StatusBarUtils {
    public static String statusBarText;
    private static final String[] EMPTY_BAR = {};

    // 1,000/1,000❤     1000❈ Defense     1,000/1,000✎ Mana
    public static String getStatusBarText() {
        try {
            return ScoreboardUtils.stripMinecraftFormatting(statusBarText);
        } catch(NullPointerException ignored) {
            return "";
        }
    }

    private static String[] getSplittedStatusBar() {
        try {
            return getStatusBarText().replaceAll(",", "").split(" {5}");
        } catch(NullPointerException ignored) {
            return EMPTY_BAR;
        }
    }

    public static int getMana() {
        try {
            return Integer.parseInt(getSplittedStatusBar()[2].split("/")[0]);
        } catch (Exception ignored) {
            return 0;
        }
    }
}
