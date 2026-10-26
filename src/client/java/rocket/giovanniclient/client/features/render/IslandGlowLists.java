package rocket.giovanniclient.client.features.render;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Persistent name/colour selections, with an independent limit for each TAB Area. */
public final class IslandGlowLists {
    public static final int MAX_NAMES = 10;
    private static final int[] COLORS = {
            0xFF3333, // red
            0x33FF33, // green
            0x3333FF, // blue
            0xFFFF33, // yellow
            0xFF33FF, // magenta
            0x33FFFF, // cyan
            0xFF9933, // orange
            0x9933FF, // purple
            0xFF99CC, // pink
            0xFFFFFF  // white
    };

    @Expose
    private Map<String, List<NameColor>> islands = new LinkedHashMap<>();

    public enum AddResult { ADDED, ALREADY_PRESENT, FULL }

    public record NameColor(@Expose String name, @Expose int color) {}

    public List<NameColor> names(String island) {
        String key = islandKey(island);
        if (key == null || islands == null) return List.of();
        List<NameColor> names = islands.get(key);
        return names == null ? List.of() : List.copyOf(names.subList(0, Math.min(names.size(), MAX_NAMES)));
    }

    public AddResult addName(String island, String name) {
        String key = requireIsland(island);
        String nameKey = nameKey(name);
        if (nameKey.isEmpty()) throw new IllegalArgumentException("Enter a mob name.");
        List<NameColor> names = new ArrayList<>(names(key));
        if (names.stream().anyMatch(entry -> nameKey(entry.name()).equals(nameKey))) return AddResult.ALREADY_PRESENT;
        if (names.size() >= MAX_NAMES) return AddResult.FULL;

        Set<Integer> usedColors = new HashSet<>();
        for (NameColor entry : names) usedColors.add(entry.color());
        for (int color : COLORS) {
            if (usedColors.contains(color)) continue;
            names.add(new NameColor(name.trim(), color));
            if (islands == null) islands = new LinkedHashMap<>();
            islands.put(key, names);
            return AddResult.ADDED;
        }
        return AddResult.FULL;
    }

    public boolean removeName(String island, String name) {
        String key = requireIsland(island);
        List<NameColor> names = new ArrayList<>(names(key));
        if (!names.removeIf(entry -> nameKey(entry.name()).equals(nameKey(name)))) return false;
        islands.put(key, names);
        return true;
    }

    public void clearNames(String island) {
        String key = requireIsland(island);
        if (islands != null) islands.remove(key);
    }

    static String nameKey(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static String islandKey(String island) {
        return island == null || island.isBlank() || island.trim().equalsIgnoreCase("None") ? null : island.trim();
    }

    private static String requireIsland(String island) {
        String key = islandKey(island);
        if (key == null) throw new IllegalStateException("Island unknown. Wait for the SkyBlock TAB Area to appear.");
        return key;
    }
}
