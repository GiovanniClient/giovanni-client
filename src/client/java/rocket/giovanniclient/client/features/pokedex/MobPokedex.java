package rocket.giovanniclient.client.features.pokedex;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Island-scoped catalog plus the shared index of currently recognised mob assemblies. */
public final class MobPokedex extends AbstractFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient/Pokedex");
    private static final DexStore STORE = new DexStore(FabricLoader.getInstance().getConfigDir().resolve("giovanniclient/pokedex"));
    private static DexStore.Island active;
    private static String loadError;
    private static ClientLevel world;
    private static List<Mob> mobs = List.of();
    private static int ticks;
    private static final DexTracker TRACKER = new DexTracker();
    private static final DexScan.Cache SCANS = new DexScan.Cache();

    public record Part(Entity entity, DexProfile.Role role) {}
    public record Mob(String name, List<Part> parts) {}

    @Override
    public void onTick(Minecraft client) {
        syncIsland(client);
        if (++ticks % 5 == 0) refresh(client);
    }

    @Override
    public void onWorldLoad(Minecraft client) { reset(); }

    @Override
    public void onWorldUnload(Minecraft client) { reset(); }

    private static void reset() {
        active = null;
        loadError = null;
        world = null;
        mobs = List.of();
        TRACKER.clear();
        SCANS.clear();
        ticks = 0;
    }

    private static void syncIsland(Minecraft client) {
        if (world != client.level) {
            reset();
            world = client.level;
        }
        String island = PlayerLocator.getPlayerArea();
        if (client.level == null || client.player == null || island.equals("None") || island.isBlank()) {
            active = null;
            loadError = null;
            mobs = List.of();
            TRACKER.clear();
            SCANS.clear();
            return;
        }
        if (active != null && active.island().equals(island)) return;
        mobs = List.of();
        TRACKER.clear();
        SCANS.clear();
        loadError = null;
        try {
            active = STORE.load(island);
        } catch (IOException exception) {
            active = new DexStore.Island(1, island, List.of());
            loadError = exception.getMessage();
            LOGGER.error("Could not load Pokédex for {}", island, exception);
        }
    }

    public static String island() {
        syncIsland(Minecraft.getInstance());
        return active == null ? null : active.island();
    }

    public static List<DexStore.Entry> entries() {
        syncIsland(Minecraft.getInstance());
        return active == null ? List.of() : active.entries();
    }

    public static boolean hasEntry(String name) {
        return active != null && active.entries().stream().anyMatch(entry -> DexProfile.key(entry.name()).equals(DexProfile.key(name)));
    }

    public static List<Mob> mobs(String name) {
        return mobs.stream().filter(mob -> DexProfile.key(mob.name()).equals(DexProfile.key(name))).toList();
    }

    public static Path file() {
        return active == null ? null : STORE.path(active.island());
    }

    public static String error() { return loadError; }

    public static void reload() throws IOException {
        requireIsland();
        DexStore.Island loaded = STORE.load(active.island());
        active = loaded;
        loadError = null;
        TRACKER.clear();
        refresh(Minecraft.getInstance());
    }

    public static boolean add(String name, DexScan.Capture capture) throws IOException {
        requireWritable();
        DexStore.Island updated = DexStore.withVariant(active, name, capture.profile());
        return save(updated);
    }

    public static boolean addVariant(String name, DexScan.Capture capture) throws IOException {
        requireWritable();
        return save(DexStore.withExistingVariant(active, name, capture.profile()));
    }

    private static boolean save(DexStore.Island updated) throws IOException {
        if (updated == active) return false;
        STORE.save(updated);
        active = updated;
        TRACKER.clear();
        refresh(Minecraft.getInstance());
        return true;
    }

    public static boolean remove(String name) throws IOException {
        requireWritable();
        List<DexStore.Entry> remaining = active.entries().stream()
                .filter(entry -> !DexProfile.key(entry.name()).equals(DexProfile.key(name))).toList();
        if (remaining.size() == active.entries().size()) return false;
        DexStore.Island updated = new DexStore.Island(1, active.island(), remaining);
        STORE.save(updated);
        active = updated;
        TRACKER.clear();
        refresh(Minecraft.getInstance());
        return true;
    }

    private static void requireIsland() {
        syncIsland(Minecraft.getInstance());
        if (active == null) throw new IllegalStateException("Island unknown. Wait for the SkyBlock TAB Area to appear.");
    }

    private static void requireWritable() {
        requireIsland();
        if (loadError != null) throw new IllegalStateException(loadError + ". Fix the file and use /gio dex reload before saving.");
    }

    private static void refresh(Minecraft client) {
        mobs = List.of();
        if (active == null || loadError != null || active.entries().isEmpty() || client.level == null) return;
        List<DexMatcher.Sample> samples = new ArrayList<>();
        Map<Integer, Entity> entities = new HashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!EntityFingerprint.eligible(entity)) continue;
            samples.add(EntityFingerprint.sample(entity));
            entities.put(entity.getId(), entity);
        }
        mobs = TRACKER.resolve(active.entries(), samples).stream().map(match -> {
            List<Part> parts = new ArrayList<>();
            for (int i = 0; i < match.parts().size(); i++) {
                parts.add(new Part(entities.get(match.parts().get(i).id()), match.roles().get(i)));
            }
            return new Mob(match.name(), List.copyOf(parts));
        }).toList();
    }

    public static List<Entity> nearby() {
        return nearby(DexScan.DEFAULT_RADIUS);
    }

    private static List<Entity> nearby(double radius) {
        DexScan.validateRadius(radius);
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) throw new IllegalStateException("Not in-world / not connected.");
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.distanceToSqr(client.player) <= radius * radius && EntityFingerprint.eligible(entity)) entities.add(entity);
        }
        entities.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(client.player)));
        return entities;
    }

    /** Conservative proposal; /capture permits an exact selection in crowded or unusual assemblies. */
    public static DexScan.Capture captureNearby() {
        return captureNearby(nearby());
    }

    private static DexScan.Capture captureNearby(List<Entity> nearby) {
        if (nearby.isEmpty()) throw new IllegalStateException("No mob parts within 7 blocks.");
        Minecraft client = Minecraft.getInstance();
        Entity seed = client.hitResult instanceof EntityHitResult hit && nearby.contains(hit.getEntity()) ? hit.getEntity() : null;
        if (seed == null) {
            seed = nearby.stream().filter(entity -> isBody(entity) || EntityFingerprint.signature(entity).distinctive()).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No mob body, identifying name or model nearby. Use /gio dex scan."));
        }
        Entity selected = seed;
        Entity body = isBody(seed) ? seed : nearby.stream().filter(MobPokedex::isBody)
                .filter(entity -> horizontalDistance(selected, entity) < 0.8 && Math.abs(selected.getY() - entity.getY()) < 3.5)
                .min(Comparator.comparingDouble(selected::distanceToSqr)).orElse(null);
        Entity center = body == null ? seed : body;
        Set<Entity> group = new LinkedHashSet<>();
        group.add(seed);
        group.add(center);
        String seedName = EntityFingerprint.signature(seed).name();
        for (Entity part : nearby) {
            if (part == center || part == seed) continue;
            if (part.isPassengerOfSameVehicle(center)) {
                group.add(part);
                continue;
            }
            if (horizontalDistance(center, part) > 0.65 || Math.abs(center.getY() - part.getY()) > 3.5) continue;
            if (isBody(part)) continue; // Never acquire a second unrelated body merely because it is close.
            String partName = EntityFingerprint.signature(part).name();
            if (!seedName.isEmpty() && !partName.isEmpty() && !seedName.equals(partName)) continue;
            Entity nearestBody = nearby.stream().filter(MobPokedex::isBody)
                    .min(Comparator.comparingDouble(part::distanceToSqr)).orElse(null);
            if (body != null && nearestBody != body) continue;
            group.add(part);
        }
        if (body != null && group.stream().noneMatch(entity -> EntityFingerprint.signature(entity).distinctive())) {
            return DexScan.capture(List.of(snapshot(body)));
        }
        return DexScan.capture(group.stream().map(MobPokedex::snapshot).toList());
    }

    public static DexScan scan(double radius) {
        requireIsland();
        List<Entity> nearby = nearby(radius);
        Set<Integer> proposed;
        try {
            proposed = captureNearby(nearby).entities().stream().map(DexScan.Part::id).collect(java.util.stream.Collectors.toSet());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            proposed = Set.of();
        }
        DexScan scan = new DexScan(active.island(), radius, nearby.stream().map(MobPokedex::snapshot).toList(), proposed);
        SCANS.remember(world, scan);
        return scan;
    }

    public static DexScan.Capture captureIds(String rawIds) {
        requireIsland();
        DexScan scan = SCANS.get(world, active.island());
        if (scan == null) throw new IllegalStateException("Nessuna scan salvata per quest'isola e mondo. Esegui prima /gio dex scan [raggio].");
        return scan.captureIds(rawIds);
    }

    private static DexScan.Part snapshot(Entity entity) {
        return new DexScan.Part(entity.getId(), entity.getX(), entity.getY(), entity.getZ(), EntityFingerprint.signature(entity), EntityFingerprint.role(entity));
    }

    private static boolean isBody(Entity entity) { return entity instanceof LivingEntity && !(entity instanceof ArmorStand); }
    private static double horizontalDistance(Entity a, Entity b) { return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ()); }
}
