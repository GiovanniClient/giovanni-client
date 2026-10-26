package rocket.giovanniclient.client.features.pokedex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable scan-time data. Deliberately holds no live Minecraft entities. */
public record DexScan(String island, double radius, List<Part> entities, Set<Integer> proposedIds) {
    public static final double DEFAULT_RADIUS = 7;
    public static final double MAX_RADIUS = 128;

    public DexScan {
        Objects.requireNonNull(island);
        validateRadius(radius);
        entities = List.copyOf(entities);
        proposedIds = Set.copyOf(proposedIds);
    }

    public record Part(int id, double x, double y, double z, DexProfile.Signature signature, DexProfile.Role role) {
        public Part {
            Objects.requireNonNull(signature);
            Objects.requireNonNull(role);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("Invalid entity position.");
            }
        }
    }

    public record Capture(List<Part> entities, DexProfile profile) {
        public Capture { entities = List.copyOf(entities); }
    }

    public Capture captureIds(String rawIds) {
        var selected = new LinkedHashMap<Integer, Part>();
        for (String token : rawIds.trim().split("[ ,]+")) {
            int id;
            try { id = Integer.parseInt(token); }
            catch (NumberFormatException ignored) { throw new IllegalArgumentException("Usa gli ID dell'ultima /gio dex scan, separati da spazi."); }
            Part part = entities.stream().filter(candidate -> candidate.id() == id).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("ID " + id + " non presente nell'ultima scan. Esegui /gio dex scan [raggio] per aggiornarla."));
            selected.putIfAbsent(id, part);
        }
        return capture(new ArrayList<>(selected.values()));
    }

    public static Capture capture(List<Part> entities) {
        if (entities.isEmpty()) throw new IllegalArgumentException("Select at least one entity to capture.");
        Part anchor = entities.stream().filter(part -> part.signature().distinctive())
                .max(Comparator.comparingInt(part -> part.signature().name().isEmpty() ? 1 : 2))
                .orElseGet(() -> entities.stream().filter(part -> part.role() == DexProfile.Role.BODY).findFirst().orElse(entities.getFirst()));
        List<Part> ordered = new ArrayList<>();
        ordered.add(anchor);
        entities.stream().filter(part -> part != anchor).sorted(Comparator.comparingInt(Part::id)).forEach(ordered::add);
        List<DexProfile.Part> parts = ordered.stream().map(part -> new DexProfile.Part(part.signature(), part.role(),
                round(Math.hypot(part.x() - anchor.x(), part.z() - anchor.z())), round(part.y() - anchor.y()))).toList();
        return new Capture(ordered, new DexProfile(parts));
    }

    public static String describe(Part part) {
        var signature = part.signature();
        return "#" + part.id() + " " + signature.type().replace("minecraft:", "") + " [" + part.role() + "]"
                + (signature.name().isEmpty() ? "" : " \"" + signature.name() + "\"")
                + (signature.equipment().isEmpty() ? "" : " " + signature.equipment())
                + (signature.appearance().isEmpty() ? "" : " " + signature.appearance());
    }

    public static void validateRadius(double radius) {
        if (!Double.isFinite(radius) || radius < 1 || radius > MAX_RADIUS) {
            throw new IllegalArgumentException("Il raggio della scan deve essere tra 1 e 128 blocchi.");
        }
    }

    private static double round(double value) { return Math.round(value * 100) / 100.0; }

    /** World identity matters even when two servers report the same island name. */
    public static final class Cache {
        private Object world;
        private DexScan scan;

        public void remember(Object world, DexScan scan) {
            this.world = Objects.requireNonNull(world);
            this.scan = Objects.requireNonNull(scan);
        }

        public DexScan get(Object world, String island) {
            if (this.world != world || scan == null || !scan.island().equals(island)) clear();
            return scan;
        }

        public void clear() { world = null; scan = null; }
    }
}
