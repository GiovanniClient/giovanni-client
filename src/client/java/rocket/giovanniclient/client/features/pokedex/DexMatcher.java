package rocket.giovanniclient.client.features.pokedex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Matches a whole assembly, never a bare vanilla body detached from its identifying parts. */
public final class DexMatcher {
    public record Sample(int id, UUID uuid, double x, double y, double z, DexProfile.Signature signature) {}
    public record Match(String name, List<Sample> parts, List<DexProfile.Role> roles) {}
    private record Variant(String name, DexProfile profile) {}

    private DexMatcher() {}

    public static List<Match> match(List<DexStore.Entry> entries, List<Sample> samples) {
        return match(entries, samples, Map.of());
    }

    static List<Match> match(List<DexStore.Entry> entries, List<Sample> samples, Map<UUID, List<UUID>> bindings) {
        Map<DexProfile.Signature, List<Sample>> bySignature = new HashMap<>();
        for (Sample sample : samples) bySignature.computeIfAbsent(sample.signature(), ignored -> new ArrayList<>()).add(sample);
        List<Variant> variants = entries.stream()
                .flatMap(entry -> entry.variants().stream().map(profile -> new Variant(entry.name(), profile)))
                .sorted(Comparator.comparingInt((Variant v) -> v.profile().parts().size()).reversed()).toList();
        Set<Integer> claimed = new HashSet<>();
        List<Match> result = new ArrayList<>();
        for (Variant variant : variants) {
            DexProfile profile = variant.profile();
            for (Sample anchor : bySignature.getOrDefault(profile.parts().getFirst().signature(), List.of())) {
                if (claimed.contains(anchor.id())) continue;
                List<UUID> bound = bindings.get(anchor.uuid());
                if (bound != null && bound.size() != profile.parts().size()) continue;
                List<Sample> assembly = new ArrayList<>();
                Set<Integer> used = new HashSet<>(claimed);
                assembly.add(anchor);
                used.add(anchor.id());
                for (DexProfile.Part part : profile.parts().subList(1, profile.parts().size())) {
                    Sample closest = null;
                    double best = Double.MAX_VALUE;
                    for (Sample candidate : bySignature.getOrDefault(part.signature(), List.of())) {
                        if (used.contains(candidate.id())) continue;
                        if (bound != null && !bound.get(assembly.size()).equals(candidate.uuid())) continue;
                        double horizontal = Math.hypot(candidate.x() - anchor.x(), candidate.z() - anchor.z());
                        double dy = candidate.y() - anchor.y();
                        double error = Math.abs(horizontal - part.horizontalOffset()) + Math.abs(dy - part.yOffset());
                        if (Math.abs(horizontal - part.horizontalOffset()) > 0.65 || Math.abs(dy - part.yOffset()) > 0.9) continue;
                        // Prefer a part's nearest compatible anchor when identical mobs stand side by side.
                        boolean belongsElsewhere = bySignature.get(profile.parts().getFirst().signature()).stream()
                                .anyMatch(other -> other.id() != anchor.id() && other.id() != candidate.id()
                                        && offsetError(other, candidate, part) + 0.05 < error);
                        if (!belongsElsewhere && error < best) {
                            closest = candidate;
                            best = error;
                        }
                    }
                    if (closest == null) break;
                    assembly.add(closest);
                    used.add(closest.id());
                }
                if (assembly.size() != profile.parts().size()) continue;
                claimed.addAll(assembly.stream().map(Sample::id).toList());
                result.add(new Match(variant.name(), List.copyOf(assembly), profile.parts().stream().map(DexProfile.Part::role).toList()));
            }
        }
        return result;
    }

    private static double offsetError(Sample anchor, Sample candidate, DexProfile.Part part) {
        return Math.abs(Math.hypot(candidate.x() - anchor.x(), candidate.z() - anchor.z()) - part.horizontalOffset())
                + Math.abs(candidate.y() - anchor.y() - part.yOffset());
    }
}
