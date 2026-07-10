package rocket.giovanniclient.client.features.render;

import net.minecraft.client.player.AbstractClientPlayer;

import java.util.Locale;

public final class PlayerTracer {
    private static boolean enabled;
    private static String nameFilter;

    private PlayerTracer() {
    }

    public static void trackAll() {
        enabled = true;
        nameFilter = null;
    }

    public static void trackName(String name) {
        enabled = true;
        nameFilter = name.toLowerCase(Locale.ROOT);
    }

    public static void stop() {
        enabled = false;
        nameFilter = null;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getNameFilter() {
        return nameFilter;
    }

    public static boolean matches(AbstractClientPlayer player) {
        if (nameFilter == null) return true;
        return player.getGameProfile().name().toLowerCase(Locale.ROOT).contains(nameFilter);
    }
}
