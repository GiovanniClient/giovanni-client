package rocket.giovanniclient.client.features.pokedex;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Samples entities on the client thread and matches their snapshots on a single worker. */
final class DexRecognition {
    private static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient/Pokedex");
    private static final int REFRESH_INTERVAL = 5;
    private static final int MAX_ENTITIES_PER_TICK = 128;
    private static final long SAMPLE_BUDGET_NANOS = 1_000_000;

    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon().name("Giovanni Pokédex").factory());
    private ClientLevel world;
    private LinkedHashSet<Entity> pending = new LinkedHashSet<>();
    private LinkedHashSet<Entity> next = new LinkedHashSet<>();
    private List<DexMatcher.Sample> samples;
    private List<DexStore.Entry> entries = List.of();
    private Set<EntityType<?>> types = Set.of();
    private DexTracker tracker = new DexTracker();
    private Future<Map<String, List<DexMatcher.Match>>> job;
    private Map<String, List<DexMatcher.Match>> matches = Map.of();
    private int cooldown;

    void registerEntityEvents() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!EntityFingerprint.supports(entity)) return;
            worldChanged(level);
            next.add(entity);
        });
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (world != level) return;
            pending.remove(entity);
            next.remove(entity);
        });
    }

    void worldChanged(ClientLevel level) {
        if (world == level) return;
        world = level;
        pending = new LinkedHashSet<>();
        next = new LinkedHashSet<>();
        setEntries(List.of());
    }

    void setEntries(List<DexStore.Entry> entries) {
        this.entries = entries;
        Set<EntityType<?>> activeTypes = new HashSet<>();
        for (DexStore.Entry entry : entries) {
            for (DexProfile profile : entry.variants()) {
                for (DexProfile.Part part : profile.parts()) {
                    Identifier id = Identifier.tryParse(part.signature().type());
                    if (id != null) BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(activeTypes::add);
                }
            }
        }
        types = activeTypes;
        if (job != null) job.cancel(true);
        job = null;
        if (!pending.isEmpty()) {
            next.addAll(pending);
            pending.clear();
        }
        samples = null;
        matches = Map.of();
        tracker = new DexTracker();
        cooldown = 0;
    }

    void tick() {
        if (world == null || types.isEmpty()) return;
        if (cooldown > 0) cooldown--;
        if (job != null) {
            if (!job.isDone()) return;
            try {
                matches = job.get();
            } catch (ExecutionException exception) {
                LOGGER.error("Could not match Pokédex entities", exception.getCause());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                job = null;
            }
        }
        if (samples == null) {
            if (cooldown > 0) return;
            if (pending.isEmpty()) {
                var empty = pending;
                pending = next;
                next = empty;
            }
            samples = new ArrayList<>();
            cooldown = REFRESH_INTERVAL;
        }

        long started = System.nanoTime();
        for (int i = 0; i < MAX_ENTITIES_PER_TICK && !pending.isEmpty(); i++) {
            Entity entity = pending.removeFirst();
            next.add(entity);
            if (types.contains(entity.getType()) && EntityFingerprint.eligible(entity)) {
                samples.add(EntityFingerprint.sample(entity));
            }
            if (System.nanoTime() - started >= SAMPLE_BUDGET_NANOS) break;
        }
        if (!pending.isEmpty()) return;

        // These values belong exclusively to this job after submission.
        var snapshot = samples;
        var catalog = entries;
        var bindings = tracker;
        samples = null;
        job = worker.submit(() -> {
            Map<String, List<DexMatcher.Match>> byName = new HashMap<>();
            for (DexMatcher.Match match : bindings.resolve(catalog, snapshot)) {
                byName.computeIfAbsent(DexProfile.key(match.name()), ignored -> new ArrayList<>()).add(match);
            }
            return byName;
        });
    }

    List<MobPokedex.Mob> mobs(String name, ClientLevel level) {
        if (world != level || level == null) return List.of();
        List<DexMatcher.Match> named = matches.get(DexProfile.key(name));
        if (named == null) return List.of();
        List<MobPokedex.Mob> result = new ArrayList<>();
        for (DexMatcher.Match match : named) {
            List<MobPokedex.Part> parts = new ArrayList<>(match.parts().size());
            for (int i = 0; i < match.parts().size(); i++) {
                DexMatcher.Sample sample = match.parts().get(i);
                Entity entity = level.getEntity(sample.id());
                if (entity == null || !entity.isAlive() || !entity.getUUID().equals(sample.uuid())) break;
                parts.add(new MobPokedex.Part(entity, match.roles().get(i)));
            }
            if (parts.size() == match.parts().size()) {
                result.add(new MobPokedex.Mob(match.name(), List.copyOf(parts)));
            }
        }
        return result;
    }
}
