package rocket.giovanniclient.client.features.pokedex;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/** Reads exactly one island at a time. Failed reads never turn into writes of an empty dex. */
public final class DexStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        String key = DexProfile.key(island);
        String slug = key.replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        if (slug.isEmpty()) slug = "island";
        if (slug.length() > 64) slug = slug.substring(0, 64);
        String hash = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString().substring(0, 8);
        return directory.resolve(slug + "-" + hash + ".json");
    }

    public Island load(String island) throws IOException {
        if (!Files.exists(path(island))) return new Island(1, island, List.of());
        try (var reader = Files.newBufferedReader(path(island), StandardCharsets.UTF_8)) {
            Island data = GSON.fromJson(reader, Island.class);
            if (data == null || !data.island().equals(island)) throw new IllegalArgumentException("Island does not match file.");
            return data;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Pokédex file: " + path(island).getFileName(), exception);
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

    /** Explicit variant capture must not silently create another species after a mistyped name. */
    public static Island withExistingVariant(Island data, String name, DexProfile profile) {
        if (data.entries().stream().noneMatch(entry -> DexProfile.key(entry.name()).equals(DexProfile.key(name)))) {
            throw new IllegalArgumentException("Mob non presente nel Pokédex di quest'isola: " + name
                    + ". Registra prima il mob con /gio dex add oppure /gio dex capture.");
        }
        return withVariant(data, name, profile);
    }

    public static Island withVariant(Island data, String name, DexProfile profile) {
        List<Entry> entries = new ArrayList<>(data.entries());
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (!DexProfile.key(entry.name()).equals(DexProfile.key(name))) continue;
            if (entry.variants().contains(profile)) return data;
            List<DexProfile> variants = new ArrayList<>(entry.variants());
            variants.add(profile);
            entries.set(i, new Entry(entry.name(), variants));
            return new Island(1, data.island(), entries);
        }
        entries.add(new Entry(name, List.of(profile)));
        return new Island(1, data.island(), entries);
    }
}
