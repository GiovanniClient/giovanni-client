package rocket.giovanniclient.client.features.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import rocket.giovanniclient.client.features.AbstractFeature;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Highlights mobs whose nearby SkyBlock nametag contains a chosen name. */
public class NamedMobGlow extends AbstractFeature {
    private static final String OWNER = "named-mob-glow";
    private static final int[] COLORS = {
            0xFF3333, // red
            0x33FF33, // green
            0x3333FF, // blue
            0xFFFF33, // yellow
            0xFF33FF, // magenta
            0x33FFFF  // cyan
    };
    private static final int SCAN_EVERY_TICKS = 10;
    private static final int MISSING_NAMETAG_GRACE_TICKS = 20;

    private static final Map<String, NameColor> NAMES = new LinkedHashMap<>();
    private static final Map<UUID, TrackedMob> TRACKED = new HashMap<>();
    private static final Map<UUID, TrackedNametag> NAMETAGS = new HashMap<>();
    private long tickCounter;

    public enum AddResult { ADDED, ALREADY_PRESENT, FULL }

    public record NameColor(String name, int color) {}

    private record TrackedMob(int entityId, String nameKey) {}

    private record TrackedNametag(int standId, int mobId, UUID mobUuid, String nameKey, long lastSeenTick) {
        private TrackedNametag seenAt(long tick) {
            return new TrackedNametag(standId, mobId, mobUuid, nameKey, tick);
        }
    }

    public static AddResult addName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (NAMES.containsKey(key)) return AddResult.ALREADY_PRESENT;
        if (NAMES.size() == COLORS.length) return AddResult.FULL;

        Set<Integer> usedColors = new HashSet<>();
        for (NameColor entry : NAMES.values()) usedColors.add(entry.color());
        for (int color : COLORS) {
            if (usedColors.contains(color)) continue;
            NAMES.put(key, new NameColor(name, color));
            return AddResult.ADDED;
        }
        return AddResult.FULL;
    }

    public static boolean removeName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (NAMES.remove(key) == null) return false;
        NAMETAGS.values().removeIf(nametag -> nametag.nameKey().equals(key));
        for (UUID uuid : new HashSet<>(TRACKED.keySet())) {
            if (TRACKED.get(uuid).nameKey().equals(key)) {
                GlowOverrideManager.clear(OWNER, uuid);
                TRACKED.remove(uuid);
            }
        }
        return true;
    }

    public static List<NameColor> names() {
        return new ArrayList<>(NAMES.values());
    }

    public static void clearNames() {
        NAMES.clear();
        clearTracked();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level == null || client.player == null) {
            clearTracked();
            return;
        }
        if (++tickCounter % SCAN_EVERY_TICKS != 0) return;

        if (NAMES.isEmpty()) return;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !stand.isAlive()) continue;
            String nameKey = matchingNameKey(stand);
            if (nameKey == null) continue;

            TrackedNametag tracked = NAMETAGS.get(stand.getUUID());
            if (tracked == null || tracked.standId() != stand.getId() || !tracked.nameKey().equals(nameKey)) {
                // Include dying mobs when binding: a lingering nametag must not
                // skip its dead owner and select a nearby living entity instead.
                LivingEntity target = findMobUnderNametag(client, stand, true);
                if (target == null) continue;
                tracked = new TrackedNametag(stand.getId(), target.getId(), target.getUUID(), nameKey, tickCounter);
            }
            NAMETAGS.put(stand.getUUID(), tracked.seenAt(tickCounter));
        }

        Map<UUID, TrackedMob> desired = new HashMap<>();
        var iterator = NAMETAGS.entrySet().iterator();
        while (iterator.hasNext()) {
            var association = iterator.next();
            TrackedNametag tracked = association.getValue();
            if (!NAMES.containsKey(tracked.nameKey()) || tickCounter - tracked.lastSeenTick() >= MISSING_NAMETAG_GRACE_TICKS) {
                iterator.remove();
                continue;
            }

            Entity tagEntity = client.level.getEntity(tracked.standId());
            ArmorStand stand = tagEntity instanceof ArmorStand tag && tag.getUUID().equals(association.getKey()) ? tag : null;
            if (stand != null && (!stand.isAlive() || !tracked.nameKey().equals(matchingNameKey(stand)))) {
                iterator.remove();
                continue;
            }

            Entity target = client.level.getEntity(tracked.mobId());
            if (!(target instanceof LivingEntity living) || !living.isAlive() || !target.getUUID().equals(tracked.mobUuid())) {
                // Keep the association until its nametag disappears. Rebinding
                // this same stand after death/despawn would transfer its glow.
                continue;
            }
            if (stand != null && !nametagSearchBox(stand).intersects(living.getBoundingBox())) continue;
            desired.putIfAbsent(target.getUUID(), new TrackedMob(target.getId(), tracked.nameKey()));
        }

        for (UUID uuid : TRACKED.keySet()) {
            if (!desired.containsKey(uuid)) GlowOverrideManager.clear(OWNER, uuid);
        }
        TRACKED.clear();
        TRACKED.putAll(desired);
        for (var entry : desired.entrySet()) {
            Entity target = client.level.getEntity(entry.getValue().entityId());
            GlowOverrideManager.set(OWNER, target, NAMES.get(entry.getValue().nameKey()).color());
        }
    }

    static LivingEntity findMobUnderNametag(Minecraft client, ArmorStand stand) {
        return findMobUnderNametag(client, stand, false);
    }

    private static LivingEntity findMobUnderNametag(Minecraft client, ArmorStand stand, boolean includeDying) {
        AABB box = nametagSearchBox(stand);
        LivingEntity closest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.getEntities(stand, box,
                candidate -> candidate instanceof LivingEntity && (includeDying || candidate.isAlive())
                        && !(candidate instanceof ArmorStand) && candidate != client.player)) {
            double distance = stand.distanceToSqr(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = (LivingEntity) entity;
            }
        }
        return closest;
    }

    private static AABB nametagSearchBox(ArmorStand stand) {
        return stand.getBoundingBox().inflate(1.0, 3.0, 1.0).move(0.0, -1.2, 0.0);
    }

    private static String matchingNameKey(ArmorStand stand) {
        if (!stand.hasCustomName()) return null;
        String nametag = stand.getCustomName().getString().toLowerCase(Locale.ROOT);
        for (String key : NAMES.keySet()) {
            if (nametag.contains(key)) return key;
        }
        return null;
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        clearTracked();
        tickCounter = 0;
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        clearTracked();
        tickCounter = 0;
    }

    private static void clearTracked() {
        for (UUID uuid : TRACKED.keySet()) GlowOverrideManager.clear(OWNER, uuid);
        TRACKED.clear();
        NAMETAGS.clear();
    }
}
