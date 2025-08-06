package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.*;

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
    public static ScoreboardObjective getSidebarObjective(MinecraftClient client) {
        if (client.world == null || client.player == null) return null;

        Scoreboard scoreboard = client.world.getScoreboard();
        if (scoreboard == null) return null;

        Team team = scoreboard.getScoreHolderTeam(client.player.getNameForScoreboard());
        if (team != null) {
            ScoreboardDisplaySlot displaySlot = ScoreboardDisplaySlot.fromFormatting(team.getColor());
            if (displaySlot != null) {
                ScoreboardObjective teamObjective = scoreboard.getObjectiveForSlot(displaySlot);
                if (teamObjective != null) return teamObjective;
            }
        }

        return scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
    }

    /**
     * Gets the formatted scoreboard lines for a given objective.
     * Includes the objective's title as the first line.
     * @param objective The scoreboard objective
     * @return List of formatted Text entries
     */
    public static List<Text> getObjectiveFormattedLines(ScoreboardObjective objective) {
        if (objective == null) return List.of();

        Scoreboard scoreboard = objective.getScoreboard();
        if (scoreboard == null) return List.of();

        Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
        List<ScoreboardEntry> filtered = entries.stream()
                .filter(entry -> entry != null && entry.owner() != null && !entry.owner().startsWith("#"))
                .sorted(Comparator.comparingInt(ScoreboardEntry::value).reversed())
                .toList();

        List<Text> lines = new ArrayList<>();
        if (objective.getDisplayName() != null) {
            lines.add(objective.getDisplayName());
        }

        int start = Math.max(filtered.size() - 15, 0);
        List<ScoreboardEntry> relevant = filtered.subList(start, filtered.size());

        for (ScoreboardEntry entry : relevant) {
            Team team = scoreboard.getScoreHolderTeam(entry.owner());
            MutableText lineText = Text.empty();

            if (team != null) {
                if (team.getPrefix() != null) lineText.append(team.getPrefix());
                lineText.append(Text.literal(entry.owner()));
                if (team.getSuffix() != null) lineText.append(team.getSuffix());
            } else {
                lineText = Text.literal(entry.owner());
            }

            lines.add(lineText);
        }

        return lines;
    }

    /**
     * Gets the formatted Text lines from the current sidebar objective.
     * @return A list of formatted Text lines
     */
    public static List<Text> getSidebarLines() {
        MinecraftClient client = MinecraftClient.getInstance();
        ScoreboardObjective objective = getSidebarObjective(client);
        return getObjectiveFormattedLines(objective);
    }

    /**
     * Gets the sidebar lines as cleaned strings with no formatting.
     * @return A list of strings stripped of Minecraft formatting
     */
    public static List<String> getCleanedSidebarLines() {
        return getSidebarLines().stream()
                .map(Text::getString)
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
    public static String getRawLineThatContains(String searchString) {
        return getCleanedSidebarLines().stream()
                .filter(line -> line.contains(searchString))
                .findFirst()
                .orElse(null);
    }
}
