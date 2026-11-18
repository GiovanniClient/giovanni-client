package rocket.giovanniclient.client.features.pokedex;

import io.github.notenoughupdates.moulconfig.observer.Property;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.EntityHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;
import rocket.giovanniclient.client.util.Utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Scope-aware catalogs and the shared index of currently recognised mob assemblies. */
public final class MobPokedex extends AbstractFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient/Pokedex");
    private static final DexStore STORE = new DexStore(FabricLoader.getInstance().getConfigDir().resolve("giovanniclient/pokedex"));
    private static final Map<DexCatalog, Catalog> catalogs = new EnumMap<>(DexCatalog.class);
    private static String context;
    private static List<DexStore.Entry> activeEntries = List.of();
    private static Set<String> activeNames = Set.of();
    private static ClientLevel world;
    private static final DexRecognition RECOGNITION = new DexRecognition();
    private static final DexScan.Cache SCANS = new DexScan.Cache();

    public record Part(Entity entity, DexProfile.Role role) {}
    public record Mob(String name, List<Part> parts) {}
    public enum Removal { NOT_FOUND, REMOVED, BUNDLED_ONLY, REMOVED_BUNDLED_REMAINS }

    public MobPokedex() {
        RECOGNITION.registerEntityEvents();
    }

    private record Catalog(DexStore.Island local, DexStore.Island bundled, List<DexStore.Entry> entries, String error) {
        private Catalog(DexStore.Island local, DexStore.Island bundled, String error) {
            this(local, bundled, DexStore.merge(local.island(), local, bundled).entries(), error);
        }
    }

    public static void observeDevSetting(Property<Boolean> setting) {
        setting.addObserver((oldValue, newValue) -> {
            boolean enabled = Boolean.TRUE.equals(newValue);
            if (enabled == Boolean.TRUE.equals(oldValue)) return;

            int imported = 0;
            if (enabled) {
                try {
                    Path bundledDirectory = FabricLoader.getInstance().getModContainer(GiovanniClientClient.MOD_ID)
                            .flatMap(mod -> mod.findPath(DexStore.BUNDLED_DIRECTORY))
                            .orElseThrow(() -> new IOException("Bundled Pokédex directory not found."));
                    imported = STORE.importBundled(bundledDirectory);
                } catch (IOException exception) {
                    setting.set(false);
                    LOGGER.error("Could not import the Pokédex for Dex Dev", exception);
                    Utils.chat("Dex Dev could not be enabled: " + exception.getMessage());
                    return;
                }
            }

            Minecraft client = Minecraft.getInstance();
            syncContext(client);
            loadCatalogs();
            ConfigManager.saveConfig();
            Utils.chat(enabled ? "Dex Dev: imported " + imported + " catalogs. Only -user.json files are active."
                    : "Dex Dev disabled. Bundled and personal catalogs are active.");
        });
    }

    private static boolean isDevMode() {
        var debug = ConfigManager.getConfig().debugConfig;
        return debug != null && debug.DEX_DEV != null && Boolean.TRUE.equals(debug.DEX_DEV.get());
    }

    @Override
    public void onTick(Minecraft client) {
        syncContext(client);
        RECOGNITION.tick();
    }

    @Override
    public void onWorldLoad(Minecraft client) { reset(client.level); }

    @Override
    public void onWorldUnload(Minecraft client) { reset(null); }

    private static void reset(ClientLevel level) {
        context = null;
        catalogs.clear();
        activeEntries = List.of();
        activeNames = Set.of();
        world = level;
        RECOGNITION.worldChanged(level);
        RECOGNITION.setEntries(List.of());
        SCANS.clear();
    }

    private static void syncContext(Minecraft client) {
        if (world != client.level) reset(client.level);
        String next = client.level == null || client.player == null ? null : PlayerLocator.getPokedexArea(client);
        if (Objects.equals(context, next)) return;
        context = next;
        SCANS.clear();
        loadCatalogs();
    }

    private static void loadCatalogs() {
        catalogs.clear();
        for (DexCatalog catalog : DexCatalog.activeFor(context)) catalogs.put(catalog, loadCatalog(catalog.id(context)));
        rebuildEntries();
    }

    private static Catalog loadCatalog(String id) {
        DexStore.Island empty = new DexStore.Island(1, id, List.of());
        DexStore.Island bundled = empty;
        DexStore.Island local = empty;
        String error = null;
        if (!isDevMode()) {
            try {
                bundled = STORE.loadBundled(id);
            } catch (IOException exception) {
                LOGGER.error("Could not load bundled Pokédex for {}", id, exception);
            }
        }
        try {
            local = STORE.load(id);
        } catch (IOException exception) {
            error = exception.getMessage();
            LOGGER.error("Could not load local Pokédex for {}", id, exception);
        }
        return new Catalog(local, bundled, error);
    }

    private static void rebuildEntries() {
        // EnumMap iterates in catalog priority order: current area, then shared groups.
        var entries = catalogs.values().stream().flatMap(catalog -> catalog.entries().stream()).toList();
        activeEntries = DexStore.mergeEntries(entries, List.of());
        var names = new HashSet<String>();
        for (DexStore.Entry entry : activeEntries) names.add(DexProfile.key(entry.name()));
        activeNames = Set.copyOf(names);
        RECOGNITION.setEntries(activeEntries);
    }

    public static String island() {
        syncContext(Minecraft.getInstance());
        return context;
    }

    public static List<DexStore.Entry> entries() {
        syncContext(Minecraft.getInstance());
        return activeEntries;
    }

    public static List<DexStore.Entry> entries(DexCatalog scope) {
        Catalog catalog = catalog(scope);
        return catalog == null ? List.of() : catalog.entries();
    }

    public static String catalogName(DexCatalog scope) {
        if (scope == DexCatalog.CURRENT) return island();
        return catalog(scope) == null ? null : scope.displayName();
    }

    private static Catalog catalog(DexCatalog scope) {
        syncContext(Minecraft.getInstance());
        return catalogs.get(scope);
    }

    public static boolean hasEntry(String name) {
        return activeNames.contains(DexProfile.key(name));
    }

    public static List<Mob> mobs(String name) {
        return RECOGNITION.mobs(name, Minecraft.getInstance().level);
    }

    public static Path file(DexCatalog scope) {
        Catalog catalog = catalog(scope);
        return catalog == null ? null : STORE.path(catalog.local().island());
    }

    public static String error(DexCatalog scope) {
        Catalog catalog = catalog(scope);
        return catalog == null ? null : catalog.error();
    }

    public static void reload() {
        requireContext();
        loadCatalogs();
    }

    public static boolean add(String name, DexScan.Capture capture, DexCatalog scope) throws IOException {
        return saveVariant(name, capture.profile(), scope, false);
    }

    public static boolean addVariant(String name, DexScan.Capture capture, DexCatalog scope) throws IOException {
        return saveVariant(name, capture.profile(), scope, true);
    }

    private static boolean saveVariant(String name, DexProfile profile, DexCatalog scope, boolean existingOnly) throws IOException {
        Catalog target = requireWritable(scope);
        DexStore.Entry existing = findEntry(target.entries(), name);
        if (existing == null && existingOnly) {
            throw new IllegalArgumentException("Mob not in this catalog: " + name + ". Register it with add or capture first.");
        }
        if (existing != null && existing.variants().stream().anyMatch(variant -> DexStore.sameVariant(variant, profile))) return false;
        DexStore.Island updated = DexStore.withVariant(target.local(), existing == null ? name : existing.name(), profile);
        save(target, updated, scope);
        return true;
    }

    public static Removal remove(String name, DexCatalog scope) throws IOException {
        Catalog target = requireWritable(scope);
        boolean bundled = findEntry(target.bundled().entries(), name) != null;
        List<DexStore.Entry> remaining = target.local().entries().stream()
                .filter(entry -> !DexProfile.key(entry.name()).equals(DexProfile.key(name))).toList();
        if (remaining.size() == target.local().entries().size()) return bundled ? Removal.BUNDLED_ONLY : Removal.NOT_FOUND;
        save(target, new DexStore.Island(1, target.local().island(), remaining), scope);
        return bundled ? Removal.REMOVED_BUNDLED_REMAINS : Removal.REMOVED;
    }

    private static DexStore.Entry findEntry(List<DexStore.Entry> entries, String name) {
        String key = DexProfile.key(name);
        return entries.stream().filter(entry -> DexProfile.key(entry.name()).equals(key)).findFirst().orElse(null);
    }

    private static void save(Catalog target, DexStore.Island updated, DexCatalog scope) throws IOException {
        STORE.save(updated);
        Catalog replacement = new Catalog(updated, target.bundled(), null);
        catalogs.put(scope, replacement);
        rebuildEntries();
    }

    private static void requireContext() {
        syncContext(Minecraft.getInstance());
        if (context == null) throw new IllegalStateException("SkyBlock location unknown. Wait for the TAB Area or Catacombs scoreboard.");
    }

    private static Catalog requireWritable(DexCatalog scope) {
        requireContext();
        Catalog target = catalogs.get(scope);
        if (target == null) throw new IllegalStateException(scope.displayName() + " is only available in a SkyBlock island context.");
        if (target.error() != null) throw new IllegalStateException(target.error() + ". Fix the file and use /gio dex reload before saving.");
        return target;
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
        requireContext();
        List<Entity> nearby = nearby(radius);
        Set<Integer> proposed;
        try {
            proposed = captureNearby(nearby).entities().stream().map(DexScan.Part::id).collect(java.util.stream.Collectors.toSet());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            proposed = Set.of();
        }
        DexScan scan = new DexScan(context, radius, nearby.stream().map(MobPokedex::snapshot).toList(), proposed);
        SCANS.remember(world, scan);
        return scan;
    }

    public static DexScan.Capture captureIds(String rawIds) {
        requireContext();
        DexScan scan = SCANS.get(world, context);
        if (scan == null) throw new IllegalStateException("No saved scan for this island and world. Run /gio dex scan [radius] first.");
        return scan.captureIds(rawIds);
    }

    private static DexScan.Part snapshot(Entity entity) {
        return new DexScan.Part(entity.getId(), entity.getX(), entity.getY(), entity.getZ(), EntityFingerprint.signature(entity),
                EntityFingerprint.role(entity), EntityFingerprint.baseMaxHealth(entity), EntityFingerprint.size(entity));
    }

    private static boolean isBody(Entity entity) { return entity instanceof LivingEntity && !(entity instanceof ArmorStand); }
    private static double horizontalDistance(Entity a, Entity b) { return Math.hypot(a.getX() - b.getX(), a.getZ() - b.getZ()); }
}
