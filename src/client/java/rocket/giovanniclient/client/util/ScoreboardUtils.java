package rocket.giovanniclient.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.*;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class ScoreboardUtils {
    /**
     * Removes all Minecraft formatting codes (e.g., §a, §l) from a string.
     * @param input The input string
     * @return A cleaned string without formatting
     */
    public static String stripMinecraftFormatting(String input) {
        return input.replaceAll("§.", "");
    }

    /**
     * Gets the current sidebar scoreboard objective.
     * @param client The Minecraft client instance
     * @return The sidebar ScoreboardObjective, or null if unavailable
     */
    public static Objective getSidebarObjective(Minecraft client) {
        if (client.level == null || client.player == null) return null;

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) return null;

        Team team = scoreboard.getPlayersTeam(client.player.getScoreboardName());
        if (team != null) {
            DisplaySlot displaySlot = DisplaySlot.teamColorToSlot(team.getColor());
            if (displaySlot != null) {
                Objective teamObjective = scoreboard.getDisplayObjective(displaySlot);
                if (teamObjective != null) return teamObjective;
            }
        }

        return scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
    }

    /**
     * Gets the formatted scoreboard lines for a given objective.
     * Includes the objective's title as the first line.
     * @param objective The scoreboard objective
     * @return List of formatted Text entries
     */
    public static List<Component> getObjectiveFormattedLines(Objective objective) {
        if (objective == null) return List.of();

        Scoreboard scoreboard = objective.getScoreboard();
        if (scoreboard == null) return List.of();

        Collection<PlayerScoreEntry> entries = scoreboard.listPlayerScores(objective);
        List<PlayerScoreEntry> filtered = entries.stream()
                .filter(entry -> entry != null && entry.owner() != null && !entry.owner().startsWith("#"))
                .sorted(Comparator.comparingInt(PlayerScoreEntry::value).reversed())
                .toList();

        List<Component> lines = new ArrayList<>();
        if (objective.getDisplayName() != null) {
            lines.add(objective.getDisplayName());
        }

        int start = Math.max(filtered.size() - 15, 0);
        List<PlayerScoreEntry> relevant = filtered.subList(start, filtered.size());

        for (PlayerScoreEntry entry : relevant) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            MutableComponent lineText = Component.empty();

            if (team != null) {
                if (team.getPlayerPrefix() != null) lineText.append(team.getPlayerPrefix());
                lineText.append(Component.literal(entry.owner()));
                if (team.getPlayerSuffix() != null) lineText.append(team.getPlayerSuffix());
            } else {
                lineText = Component.literal(entry.owner());
            }

            lines.add(lineText);
        }

        return lines;
    }

    /**
     * Gets the formatted Text lines from the current sidebar objective.
     * @return A list of formatted Text lines
     */
    public static List<Component> getSidebarLines() {
        Minecraft client = Minecraft.getInstance();
        Objective objective = getSidebarObjective(client);
        return getObjectiveFormattedLines(objective);
    }

    /**
     * Gets the sidebar lines as cleaned strings with no formatting.
     * @return A list of strings stripped of Minecraft formatting
     */
    public static List<String> getCleanedSidebarLines() {
        return getSidebarLines().stream()
                .map(Component::getString)
                .map(ScoreboardUtils::stripMinecraftFormatting)
                .toList();
    }

    /**
     * Checks if the cleaned sidebar lines contain a given substring.
     * @param searchString The string to look for
     * @return True if found, false otherwise
     */
    public static boolean scoreboardContainsRaw(String searchString) {
        return getCleanedSidebarLines().stream().anyMatch(line -> line.contains(searchString));
    }

    /**
     * Returns the index of string in the scoreboard.
     * @param searchString The string to look for
     * @return -1 if not found, otherwise a non-negative integer
     */
    public static int getLineIndexOfString(String searchString) {
        if (scoreboardContainsRaw(searchString)) return getCleanedSidebarLines().indexOf(searchString);

        return -1;
    }

    /**
     * Checks if the cleaned sidebar lines contain a given substring.
     * @param index The index to look for
     * @return String at given index
     */
    public static String getStringAtIndex(int index) {
        if (index > 15 || index < 0) return "";

        return getCleanedSidebarLines().get(index);
    }

    /**
     * Returns the first cleaned sidebar line that contains the given substring.
     * @param searchString The string to search for
     * @return The matching line, or null if not found
     */
    public static String getRawLineContaining(String searchString) {
        return getCleanedSidebarLines().stream()
                .filter(line -> line.contains(searchString))
                .findFirst()
                .orElse(null);
    }
}
