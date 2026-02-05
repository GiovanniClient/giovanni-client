package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import sb.rocket.giovanniclient.client.mixin.PlayerListHudAccessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TabListUtils {

    private TabListUtils() {}

    // =========================
    // RAW LINES (Text)
    // =========================

    public static List<Text> getTabLines(boolean includeHeaderFooter, boolean includePlayers) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.getNetworkHandler() == null) return List.of();

        PlayerListHud hud = client.inGameHud.getPlayerListHud();
        List<Text> out = new ArrayList<>();

        if (includeHeaderFooter && hud instanceof PlayerListHudAccessor acc) {
            Text header = acc.giovanni$getHeader();
            if (header != null && !header.getString().isBlank()) out.add(header);
        }

        if (includePlayers) {
            List<PlayerListEntry> entries = new ArrayList<>(client.getNetworkHandler().getPlayerList());
            entries.sort(TAB_COMPARATOR);

            for (PlayerListEntry e : entries) {
                if (e == null || e.getProfile() == null) continue;

                Text display = e.getDisplayName();
                if (display != null) {
                    out.add(display);
                    continue;
                }

                String name = e.getProfile().name();
                Team team = e.getScoreboardTeam();

                MutableText line = Text.empty();
                if (team != null) {
                    if (team.getPrefix() != null) line.append(team.getPrefix());
                    line.append(Text.literal(name));
                    if (team.getSuffix() != null) line.append(team.getSuffix());
                } else {
                    line.append(Text.literal(name));
                }
                out.add(line);
            }
        }

        if (includeHeaderFooter && hud instanceof PlayerListHudAccessor acc) {
            Text footer = acc.giovanni$getFooter();
            if (footer != null && !footer.getString().isBlank()) out.add(footer);
        }

        return out;
    }

    // =========================
    // STRING HELPERS (RAW / CLEAN)
    // =========================

    /** "Raw" nel senso: Text#getString() (con simboli unicode, ma senza i codici § perché getString li rimuove). */
    public static List<String> getRawLines(boolean includeHeaderFooter, boolean includePlayers) {
        return getTabLines(includeHeaderFooter, includePlayers).stream()
                .map(Text::getString)
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
    private static final Comparator<PlayerListEntry> TAB_COMPARATOR =
            Comparator.<PlayerListEntry, Boolean>comparing(e -> e.getGameMode() != null && !e.getGameMode().isSurvivalLike())
                    .thenComparing(e -> {
                        Team t = e.getScoreboardTeam();
                        return t != null ? t.getName() : "";
                    }, String::compareToIgnoreCase)
                    .thenComparing(e -> e.getProfile() != null ? e.getProfile().name() : "", String::compareToIgnoreCase);
}
