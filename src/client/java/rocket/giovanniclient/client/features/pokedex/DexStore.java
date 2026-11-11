package rocket.giovanniclient.client.features.pokedex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Reads one catalog at a time. Only the personal catalog is writable. */
public final class DexStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final String BUNDLED_DIRECTORY = "assets/giovanniclient/pokedex";
    private final Path directory;

    public record Entry(String name, List<DexProfile> variants) {
        public Entry {
            if (name == null || name.isBlank() || name.length() > 80) throw new IllegalArgumentException("Use a mob name of 1-80 characters.");
            name = name.trim();
            variants = List.copyOf(variants);
            if (variants.isEmpty()) throw new IllegalArgumentException("Missing mob variants.");
        }
    }

    public record Island(int version, String island, List<Entry> entries) {
        public Island {
            if (version != 1 || island == null || island.isBlank()) throw new IllegalArgumentException("Unsupported Pokédex format.");
            entries = List.copyOf(entries);
            var names = new HashSet<String>();
            for (Entry entry : entries) {
                if (!names.add(DexProfile.key(entry.name()))) throw new IllegalArgumentException("Duplicate mob name.");
            }
        }
    }

    public DexStore(Path directory) {
        this.directory = directory;
    }

    public Path path(String island) {
        return directory.resolve(readableStem(island) + "-user.json");
    }

    private static String readableStem(String island) {
        // Use underscores for spaces and hyphens for scope separators. Escape literal
        // underscores/hyphens and unsafe characters so distinct catalog IDs stay distinct.
        return URLEncoder.encode(DexProfile.key(island), StandardCharsets.UTF_8)
                .replace("_", "%5F").replace("-", "%2D").replace("*", "%2A")
                .replace("%27", "'").replace("%3A", "-").replace('+', '_');
    }

    /** Reads only personal captures. An absent file is an empty personal catalog. */
    public Island load(String island) throws IOException {
        Path file = path(island);
        if (Files.notExists(file)) return new Island(1, island, List.of());
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return read(reader, island, file.toString());
        }
    }

    /** Reads the release's catalog directly from the mod, without creating a local copy. */
    public Island loadBundled(String island) throws IOException {
        String resource = "/" + BUNDLED_DIRECTORY + "/" + bundledFilename(island);
        var stream = DexStore.class.getResourceAsStream(resource);
        if (stream == null) return new Island(1, island, List.of());
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return read(reader, island, resource);
        }
    }

    private static String bundledFilename(String island) {
        return readableStem(island) + "-jar.json";
    }

    /** Copies the whole release catalog to user files, keeping personal variants first. */
    public int importBundled(Path bundledDirectory) throws IOException {
        List<Path> files;
        try (var paths = Files.list(bundledDirectory)) {
            files = paths.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith("-jar.json"))
                    .sorted().toList();
        }
        if (files.isEmpty()) throw new IOException("No bundled Pokédex files found.");

        // Validate every source and destination before writing any catalog.
        List<Island> updates = new ArrayList<>();
        for (Path file : files) {
            Island bundled;
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                bundled = read(reader, null, file.toString());
            }
            String id = bundled.island();
            if (!file.getFileName().toString().equals(bundledFilename(id))) {
                throw new IOException("Catalog ID does not match file: " + file);
            }
            Island personal = load(id);
            Island merged = merge(id, personal, bundled);
            if (!merged.equals(personal) || Files.notExists(path(id))) updates.add(merged);
        }
        for (Island update : updates) save(update);
        return files.size();
    }

    private static Island read(Reader reader, String island, String source) throws IOException {
        try {
            Island data = GSON.fromJson(reader, Island.class);
            if (data == null || (island != null && !data.island().equals(island))) {
                throw new IllegalArgumentException("Catalog ID does not match file.");
            }
            return data;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Pokédex file: " + source, exception);
        }
    }

    public void save(Island data) throws IOException {
        Files.createDirectories(directory);
        Path destination = path(data.island());
        Path temporary = Files.createTempFile(directory, "pokedex-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(data) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Island withVariant(Island data, String name, DexProfile profile) {
        List<Entry> entries = new ArrayList<>(data.entries());
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (!DexProfile.key(entry.name()).equals(DexProfile.key(name))) continue;
            if (entry.variants().stream().anyMatch(variant -> sameVariant(variant, profile))) return data;
            List<DexProfile> variants = new ArrayList<>(entry.variants());
            variants.add(profile);
            entries.set(i, new Entry(entry.name(), variants));
            return new Island(1, data.island(), entries);
        }
        entries.add(new Entry(name, List.of(profile)));
        return new Island(1, data.island(), entries);
    }

    /** Builds the effective catalog without changing either the personal or bundled data. */
    public static Island merge(String island, Island... sources) {
        var entries = new ArrayList<Entry>();
        for (Island source : sources) {
            if (!island.equals(source.island())) throw new IllegalArgumentException("Cannot merge different Pokédex catalogs.");
            entries.addAll(source.entries());
        }
        return new Island(1, island, mergeEntries(entries, List.of()));
    }

    /** First source wins for display names, entry order and equivalent profiles. */
    public static List<Entry> mergeEntries(List<Entry> preferred, List<Entry> fallback) {
        var entries = new LinkedHashMap<String, MergedEntry>();
        Stream.concat(preferred.stream(), fallback.stream()).forEachOrdered(entry -> {
            var merged = entries.computeIfAbsent(DexProfile.key(entry.name()), ignored ->
                    new MergedEntry(entry.name(), new LinkedHashMap<>()));
            for (DexProfile profile : entry.variants()) merged.variants().putIfAbsent(VariantKey.of(profile), profile);
        });
        return entries.values().stream().map(entry -> new Entry(entry.name(), List.copyOf(entry.variants().values()))).toList();
    }

    public static boolean sameVariant(DexProfile first, DexProfile second) {
        return first.equals(second) || VariantKey.of(first).equals(VariantKey.of(second));
    }

    private record MergedEntry(String name, Map<VariantKey, DexProfile> variants) {}

    /** Anchor is significant. The remaining parts are unordered, but multiplicity matters. */
    private record VariantKey(DexProfile.Part anchor, Map<DexProfile.Part, Integer> secondaryParts) {
        static VariantKey of(DexProfile profile) {
            var parts = profile.parts();
            var secondary = new HashMap<DexProfile.Part, Integer>();
            for (int i = 1; i < parts.size(); i++) secondary.merge(parts.get(i), 1, Integer::sum);
            return new VariantKey(parts.getFirst(), Map.copyOf(secondary));
        }
    }
}
