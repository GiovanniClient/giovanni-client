package rocket.giovanniclient.client.features.pokedex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Keeps a lingering nametag from being rebound to a different mob after its owner dies. */
public final class DexTracker {
    private final Map<UUID, List<UUID>> bindings = new HashMap<>();

    public List<DexMatcher.Match> resolve(List<DexStore.Entry> entries, List<DexMatcher.Sample> samples) {
        var present = new HashSet<UUID>();
        samples.forEach(sample -> present.add(sample.uuid()));
        bindings.keySet().removeIf(anchor -> !present.contains(anchor));
        List<DexMatcher.Match> matches = DexMatcher.match(entries, samples, bindings);
        for (DexMatcher.Match match : matches) {
            bindings.putIfAbsent(match.parts().getFirst().uuid(), match.parts().stream().map(DexMatcher.Sample::uuid).toList());
        }
        return matches;
    }

    public void clear() { bindings.clear(); }
}
