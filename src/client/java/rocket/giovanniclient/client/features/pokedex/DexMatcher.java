package rocket.giovanniclient.client.features.pokedex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;

/** Matches all parts of a stored profile. */
public final class DexMatcher {
    private static final double HORIZONTAL_TOLERANCE = 0.65;
    private static final double VERTICAL_TOLERANCE = 0.9;
    private static final double ANCHOR_MARGIN = 0.05;

    public record Sample(int id, UUID uuid, double x, double y, double z, DexProfile.Signature signature,
                         Double baseMaxHealth, DexProfile.Size size) {}
    public record Match(String name, List<Sample> parts, List<DexProfile.Role> roles) {}

    private record Variant(String name, List<DexProfile.Part> parts, List<DexProfile.Role> roles, int constraints) {
        Variant(String name, DexProfile profile) {
            this(name, profile.parts(), profile.parts().stream().map(DexProfile.Part::role).toList(),
                    profile.parts().stream().map(DexProfile.Part::signature).mapToInt(signature ->
                            (signature.baseMaxHealth() == null ? 0 : 1) + (signature.size() == null ? 0 : 1)).sum());
        }
    }

    private record IndexedSample(Sample sample, int order) {}

    private DexMatcher() {}

    public static List<Match> match(List<DexStore.Entry> entries, List<Sample> samples) {
        return match(entries, samples, Map.of());
    }

    static List<Match> match(List<DexStore.Entry> entries, List<Sample> samples, Map<UUID, List<UUID>> bindings) {
        var bySignature = new SampleIndex(samples);
        List<Variant> variants = entries.stream()
                .flatMap(entry -> entry.variants().stream().map(profile -> new Variant(entry.name(), profile)))
                .sorted(Comparator.comparingInt((Variant v) -> v.parts().size())
                        .thenComparingInt(Variant::constraints).reversed()).toList();
        Set<Integer> claimed = new HashSet<>();
        List<Match> result = new ArrayList<>();
        for (Variant variant : variants) {
            checkInterrupted();
            List<DexProfile.Part> parts = variant.parts();
            SpatialIndex anchors = bySignature.get(parts.getFirst().signature());
            var anchorErrors = new HashMap<DexProfile.Part, Map<Integer, Double>>();
            for (IndexedSample indexedAnchor : anchors.samples) {
                checkInterrupted();
                Sample anchor = indexedAnchor.sample();
                if (claimed.contains(anchor.id())) continue;
                List<UUID> bound = parts.size() > 1 ? bindings.get(anchor.uuid()) : null;
                if (bound != null && bound.size() != parts.size()) continue;
                List<Sample> assembly = new ArrayList<>(parts.size());
                assembly.add(anchor);
                Set<Integer> used = new HashSet<>();
                used.add(anchor.id());
                for (int i = 1; i < parts.size(); i++) {
                    DexProfile.Part part = parts.get(i);
                    Map<Integer, Double> errors = anchorErrors.computeIfAbsent(part, ignored -> new HashMap<>());
                    UUID boundPart = bound == null ? null : bound.get(i);
                    Sample closest = findPart(anchor, part, bySignature.get(part.signature()), anchors, errors, claimed, used, boundPart);
                    if (closest == null) break;
                    assembly.add(closest);
                    used.add(closest.id());
                }
                if (assembly.size() != parts.size()) continue;
                for (Sample sample : assembly) claimed.add(sample.id());
                result.add(new Match(variant.name(), List.copyOf(assembly), variant.roles()));
            }
        }
        return result;
    }

    private static Sample findPart(Sample anchor, DexProfile.Part part, SpatialIndex candidates, SpatialIndex anchors,
                                   Map<Integer, Double> anchorErrors, Set<Integer> claimed, Set<Integer> used, UUID boundPart) {
        IndexedSample closest = null;
        double best = Double.MAX_VALUE;
        for (List<IndexedSample> bucket : candidates.near(anchor.x(), anchor.y() + part.yOffset(), anchor.z(),
                part.horizontalOffset() + HORIZONTAL_TOLERANCE, VERTICAL_TOLERANCE)) {
            checkInterrupted();
            for (IndexedSample indexed : bucket) {
                Sample candidate = indexed.sample();
                if (claimed.contains(candidate.id()) || used.contains(candidate.id())) continue;
                if (boundPart != null && !boundPart.equals(candidate.uuid())) continue;
                double horizontalError = Math.abs(Math.hypot(candidate.x() - anchor.x(), candidate.z() - anchor.z()) - part.horizontalOffset());
                double verticalError = Math.abs(candidate.y() - anchor.y() - part.yOffset());
                if (horizontalError > HORIZONTAL_TOLERANCE || verticalError > VERTICAL_TOLERANCE) continue;
                double error = horizontalError + verticalError;
                boolean earlierTie = error == best && closest != null && indexed.order() < closest.order();
                if (!(error < best || earlierTie)) continue;

                // The closest eligible anchor for this part depends only on its offsets and the candidate.
                // Including the current anchor in the minimum is safe because of the comparison margin.
                if (error > ANCHOR_MARGIN) {
                    double nearest = anchorErrors.computeIfAbsent(candidate.id(),
                            ignored -> closestAnchorError(anchors, candidate, part));
                    if (nearest + ANCHOR_MARGIN < error) continue;
                }
                closest = indexed;
                best = error;
            }
        }
        return closest == null ? null : closest.sample();
    }

    private static double closestAnchorError(SpatialIndex anchors, Sample candidate, DexProfile.Part part) {
        double best = Double.MAX_VALUE;
        double maxError = HORIZONTAL_TOLERANCE + VERTICAL_TOLERANCE;
        for (List<IndexedSample> bucket : anchors.near(candidate.x(), candidate.y() - part.yOffset(), candidate.z(),
                part.horizontalOffset() + maxError, maxError)) {
            checkInterrupted();
            for (IndexedSample indexed : bucket) {
                Sample other = indexed.sample();
                if (other.id() == candidate.id()) continue;
                double horizontal = Math.hypot(candidate.x() - other.x(), candidate.z() - other.z());
                double dy = candidate.y() - other.y();
                double error = Math.abs(horizontal - part.horizontalOffset()) + Math.abs(dy - part.yOffset());
                if (error < best) best = error;
                if (best == 0) return best;
            }
        }
        return best;
    }

    private static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("Pokédex matching was interrupted");
    }

    private static final class SampleIndex {
        private final Map<DexProfile.Signature, List<IndexedSample>> samples = new HashMap<>();
        private final Map<DexProfile.Signature, SpatialIndex> indexes = new HashMap<>();

        SampleIndex(List<Sample> input) {
            for (int i = 0; i < input.size(); i++) {
                Sample sample = input.get(i);
                samples.computeIfAbsent(sample.signature(), ignored -> new ArrayList<>()).add(new IndexedSample(sample, i));
            }
        }

        SpatialIndex get(DexProfile.Signature signature) {
            return indexes.computeIfAbsent(signature, key -> {
                Double health = key.baseMaxHealth();
                DexProfile.Size size = key.size();
                if (health == null && size == null) return new SpatialIndex(samples.getOrDefault(key, List.of()));
                var group = samples.getOrDefault(key.withBaseMaxHealth(null).withSize(null), List.of()).stream()
                        .filter(indexed -> (health == null || health.equals(indexed.sample().baseMaxHealth()))
                                && (size == null || size.equals(indexed.sample().size())))
                        .toList();
                return new SpatialIndex(group);
            });
        }
    }

    private static final class SpatialIndex {
        private static final int CELL_SIZE = 4;
        private static final int LINEAR_SEARCH_LIMIT = 16;
        private final List<IndexedSample> samples;
        private final Map<Cell, List<IndexedSample>> cells;

        SpatialIndex(List<IndexedSample> samples) {
            this.samples = samples;
            cells = new HashMap<>();
            if (samples.size() <= LINEAR_SEARCH_LIMIT) return;
            for (IndexedSample indexed : samples) {
                Sample sample = indexed.sample();
                var cell = new Cell(cellOf(sample.x()), cellOf(sample.y()), cellOf(sample.z()));
                cells.computeIfAbsent(cell, ignored -> new ArrayList<>()).add(indexed);
            }
        }

        List<List<IndexedSample>> near(double x, double y, double z, double horizontal, double vertical) {
            if (cells.isEmpty()) return List.of(samples);
            int minX = cellOf(x - horizontal);
            int maxX = cellOf(x + horizontal);
            int minY = cellOf(y - vertical);
            int maxY = cellOf(y + vertical);
            int minZ = cellOf(z - horizontal);
            int maxZ = cellOf(z + horizontal);
            var result = new ArrayList<List<IndexedSample>>();
            for (long cellX = minX; cellX <= maxX; cellX++) {
                for (long cellY = minY; cellY <= maxY; cellY++) {
                    for (long cellZ = minZ; cellZ <= maxZ; cellZ++) {
                        var bucket = cells.get(new Cell((int) cellX, (int) cellY, (int) cellZ));
                        if (bucket != null) result.add(bucket);
                    }
                }
            }
            return result;
        }

        private static int cellOf(double coordinate) {
            return (int) Math.floor(coordinate / CELL_SIZE);
        }

        private record Cell(int x, int y, int z) {}
    }
}
