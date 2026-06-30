package rocket.giovanniclient.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.mixin.PlayerListHudAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class TabListUtils {

    private TabListUtils() {}

    // =========================
    // RAW LINES (Component)
    // =========================

    public static List<Component> getTabLines(boolean includeHeaderFooter, boolean includePlayers) {
        // Gatekeeper pattern, this class is shit
        if (!GiovanniClientClient.isCurrentVersionSupported()) {
            return Collections.emptyList();
        }

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.getConnection() == null) return List.of();

        PlayerTabOverlay hud = client.gui.getTabList();
        List<Component> out = new ArrayList<>();

        if (includeHeaderFooter && hud instanceof PlayerListHudAccessor acc) {
            Component header = acc.giovanni$getHeader();
            if (header != null && !header.getString().isBlank()) out.add(header);
        }

        if (includePlayers) {
            List<PlayerInfo> entries = new ArrayList<>(client.getConnection().getOnlinePlayers());
            entries.sort(TAB_COMPARATOR);

            for (PlayerInfo e : entries) {
                if (e == null || e.getProfile() == null) continue;

                Component display = e.getTabListDisplayName();
                if (display != null) {
                    out.add(display);
                    continue;
                }

                String name = e.getProfile().name();
                PlayerTeam team = e.getTeam();

                MutableComponent line = Component.empty();
                if (team != null) {
                    if (team.getPlayerPrefix() != null) line.append(team.getPlayerPrefix());
                    line.append(Component.literal(name));
                    if (team.getPlayerSuffix() != null) line.append(team.getPlayerSuffix());
                } else {
                    line.append(Component.literal(name));
                }
                out.add(line);
            }
        }

        if (includeHeaderFooter && hud instanceof PlayerListHudAccessor acc) {
            Component footer = acc.giovanni$getFooter();
            if (footer != null && !footer.getString().isBlank()) out.add(footer);
        }

        return out;
    }

    // =========================
    // STRING HELPERS (RAW / CLEAN)
    // =========================

    /** "Raw" nel senso: Component#getString() (con simboli unicode, ma senza i codici § perché getString li rimuove). */
    public static List<String> getRawLines(boolean includeHeaderFooter, boolean includePlayers) {
        return getTabLines(includeHeaderFooter, includePlayers).stream()
                .map(Component::getString)
                .toList();
    }

    /** "Cleaned" come fai per scoreboard: strip dei formatting legacy, ecc. */
    public static List<String> getCleanedLines(boolean includeHeaderFooter, boolean includePlayers) {
        return getRawLines(includeHeaderFooter, includePlayers).stream()
                .map(ScoreboardUtils::stripMinecraftFormatting)
                .toList();
    }

    public static boolean tabContainsRaw(String needle) {
        if (needle == null || needle.isEmpty()) return false;
        for (String line : getRawLines(true, true)) {
            if (line != null && line.contains(needle)) return true;
        }
        return false;
    }

    public static String getRawLineContaining(String needle) {
        if (needle == null || needle.isEmpty()) return null;
        for (String line : getRawLines(true, true)) {
            if (line != null && line.contains(needle)) return line;
        }
        return null;
    }

    public static boolean tabContainsClean(String needle) {
        if (needle == null || needle.isEmpty()) return false;
        for (String line : getCleanedLines(true, true)) {
            if (line != null && line.contains(needle)) return true;
        }
        return false;
    }

    public static String getCleanLineContaining(String needle) {
        if (needle == null || needle.isEmpty()) return null;
        for (String line : getCleanedLines(true, true)) {
            if (line != null && line.contains(needle)) return line;
        }
        return null;
    }

    // Ordinamento "ragionevole" stile vanilla
    private static final Comparator<PlayerInfo> TAB_COMPARATOR =
            Comparator.<PlayerInfo, Boolean>comparing(e -> e.getGameMode() != null && !e.getGameMode().isSurvival())
                    .thenComparing(e -> {
                        PlayerTeam t = e.getTeam();
                        return t != null ? t.getName() : "";
                    }, String::compareToIgnoreCase)
                    .thenComparing(e -> e.getProfile() != null ? e.getProfile().name() : "", String::compareToIgnoreCase);
}
