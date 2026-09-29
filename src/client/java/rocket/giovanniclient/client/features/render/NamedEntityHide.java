package rocket.giovanniclient.client.features.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Makes selected mobs appear like visible invisible teammates and hides their armor stands. */
public class NamedEntityHide extends AbstractFeature {
    private static final int SCAN_EVERY_TICKS = 5;
    private static final Map<UUID, Tracked> MOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, Tracked> STANDS = new ConcurrentHashMap<>();
    private static volatile Set<String> ACTIVE_NAME_KEYS = Set.of();
    private int tickCounter;

    private record Tracked(int entityId, String nameKey) {}

    public static List<String> names() {
        List<String> configured = ConfigManager.getConfig().rc.hiddenEntityNames;
        return configured == null ? List.of() : new ArrayList<>(configured);
    }

    public static boolean addName(String name) {
        String key = normalize(name);
        if (key.isEmpty() || names().stream().anyMatch(existing -> normalize(existing).equals(key))) return false;
        if (ConfigManager.getConfig().rc.hiddenEntityNames == null) {
            ConfigManager.getConfig().rc.hiddenEntityNames = new ArrayList<>();
        }
        ConfigManager.getConfig().rc.hiddenEntityNames.add(name);
        refreshNameKeys();
        ConfigManager.saveConfig();
        return true;
    }

    public static boolean removeName(String name) {
        String key = normalize(name);
        List<String> configured = ConfigManager.getConfig().rc.hiddenEntityNames;
        if (configured == null || !configured.removeIf(existing -> normalize(existing).equals(key))) return false;
        MOBS.entrySet().removeIf(entry -> entry.getValue().nameKey().equals(key));
        STANDS.entrySet().removeIf(entry -> entry.getValue().nameKey().equals(key));
        refreshNameKeys();
        ConfigManager.saveConfig();
        return true;
    }

    public static void clearNames() {
        ConfigManager.getConfig().rc.hiddenEntityNames = new ArrayList<>();
        ACTIVE_NAME_KEYS = Set.of();
        clearTracked();
        ConfigManager.saveConfig();
    }

    public static boolean isHiddenMob(Entity entity) {
        if (entity == null) return false;
        Tracked tracked = MOBS.get(entity.getUUID());
        return tracked != null && tracked.entityId() == entity.getId();
    }

    public static boolean isHiddenStand(ArmorStand stand) {
        if (stand == null) return false;
        Tracked tracked = STANDS.get(stand.getUUID());
        if (tracked != null && tracked.entityId() == stand.getId()) return true;
        if (!stand.hasCustomName() || ACTIVE_NAME_KEYS.isEmpty()) return false;

        String name = normalize(stand.getCustomName().getString());
        if (ACTIVE_NAME_KEYS.stream().anyMatch(name::contains)) return true;
        if (MOBS.isEmpty()) return false;

        // A second named stand (for example a health line) can render even
        // when the original stand is outside the scan used to find the mob.
        Minecraft client = Minecraft.getInstance();
        LivingEntity nearest = client.level == null ? null : NamedMobGlow.findMobUnderNametag(client, stand);
        return isHiddenMob(nearest);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            clearTracked();
            return;
        }
        if (++tickCounter % SCAN_EVERY_TICKS != 0) return;

        List<String> filters = names();
        refreshNameKeys();
        MOBS.entrySet().removeIf(entry -> {
            Entity entity = client.level.getEntity(entry.getValue().entityId());
            return !(entity instanceof LivingEntity living) || !living.isAlive()
                    || !entity.getUUID().equals(entry.getKey())
                    || filters.stream().noneMatch(name -> normalize(name).equals(entry.getValue().nameKey()));
        });
        STANDS.entrySet().removeIf(entry -> {
            Entity entity = client.level.getEntity(entry.getValue().entityId());
            return !(entity instanceof ArmorStand) || !entity.getUUID().equals(entry.getKey())
                    || filters.stream().noneMatch(name -> normalize(name).equals(entry.getValue().nameKey()));
        });

        if (filters.isEmpty()) return;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName()) continue;
            String nametag = normalize(stand.getCustomName().getString());
            for (String name : filters) {
                String key = normalize(name);
                if (key.isEmpty() || !nametag.contains(key)) continue;

                STANDS.put(stand.getUUID(), new Tracked(stand.getId(), key));
                LivingEntity target = NamedMobGlow.findMobUnderNametag(client, stand);
                if (target == null) break;
                MOBS.put(target.getUUID(), new Tracked(target.getId(), key));

                // SkyBlock can use additional unnamed armor stands for a mob's
                // visible equipment or head. Hide only those very close to it.
                AABB accessoryBox = target.getBoundingBox().inflate(0.4, 1.8, 0.4);
                for (Entity nearby : client.level.getEntities(target, accessoryBox,
                        candidate -> candidate instanceof ArmorStand)) {
                    ArmorStand accessory = (ArmorStand) nearby;
                    if (NamedMobGlow.findMobUnderNametag(client, accessory) == target) {
                        STANDS.put(accessory.getUUID(), new Tracked(accessory.getId(), key));
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        clearTracked();
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        clearTracked();
    }

    private static String normalize(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static void refreshNameKeys() {
        Set<String> keys = new HashSet<>();
        for (String name : names()) {
            String key = normalize(name);
            if (!key.isEmpty()) keys.add(key);
        }
        ACTIVE_NAME_KEYS = Set.copyOf(keys);
    }

    private static void clearTracked() {
        MOBS.clear();
        STANDS.clear();
    }
}
