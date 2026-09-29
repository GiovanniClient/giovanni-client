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
    private static final int SCAN_EVERY_TICKS = 5;

    private static final Map<String, NameColor> NAMES = new LinkedHashMap<>();
    private static final Map<UUID, TrackedMob> TRACKED = new HashMap<>();
    private int tickCounter;

    public enum AddResult { ADDED, ALREADY_PRESENT, FULL }

    public record NameColor(String name, int color) {}

    private record TrackedMob(int entityId, String nameKey) {}

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

        // Keep a known mob highlighted while it remains loaded, even if its
        // separate SkyBlock nametag armor stand has left rendering range.
        for (UUID uuid : new HashSet<>(TRACKED.keySet())) {
            TrackedMob tracked = TRACKED.get(uuid);
            Entity entity = client.level.getEntity(tracked.entityId());
            NameColor entry = NAMES.get(tracked.nameKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()
                    || !entity.getUUID().equals(uuid) || entry == null) {
                GlowOverrideManager.clear(OWNER, uuid);
                TRACKED.remove(uuid);
                continue;
            }
            GlowOverrideManager.set(OWNER, living, entry.color());
        }

        if (NAMES.isEmpty()) return;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName()) continue;
            String nametag = stand.getCustomName().getString().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, NameColor> match : NAMES.entrySet()) {
                if (!nametag.contains(match.getKey())) continue;
                LivingEntity target = findMobUnderNametag(client, stand);
                if (target != null && !TRACKED.containsKey(target.getUUID())) {
                    TRACKED.put(target.getUUID(), new TrackedMob(target.getId(), match.getKey()));
                    GlowOverrideManager.set(OWNER, target, match.getValue().color());
                }
                break;
            }
        }
    }

    static LivingEntity findMobUnderNametag(Minecraft client, ArmorStand stand) {
        AABB box = stand.getBoundingBox().inflate(1.0, 3.0, 1.0).move(0.0, -1.2, 0.0);
        LivingEntity closest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : client.level.getEntities(stand, box,
                candidate -> candidate.isAlive() && candidate instanceof LivingEntity
                        && !(candidate instanceof ArmorStand) && candidate != client.player)) {
            double distance = stand.distanceToSqr(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = (LivingEntity) entity;
            }
        }
        return closest;
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        clearTracked();
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        clearTracked();
    }

    private static void clearTracked() {
        for (UUID uuid : TRACKED.keySet()) GlowOverrideManager.clear(OWNER, uuid);
        TRACKED.clear();
    }
}
