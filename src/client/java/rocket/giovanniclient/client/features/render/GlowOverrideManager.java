package rocket.giovanniclient.client.features.render;

import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stato client-side per forzare glow e colore su entità specifiche.
 * I mixin leggono da qui.
 */
public final class GlowOverrideManager {

    private GlowOverrideManager() {}

    // UUID -> packed RGB (0xRRGGBB)
    private static final Map<UUID, Integer> COLOR_BY_ENTITY = new ConcurrentHashMap<>();
    private static final Map<String, Map<UUID, Integer>> COLOR_BY_OWNER = new ConcurrentHashMap<>();

    public static void set(Entity e, int rgb) {
        if (e == null) return;
        COLOR_BY_ENTITY.put(e.getUUID(), rgb & 0xFFFFFF);
    }

    public static void clear(Entity e) {
        if (e == null) return;
        COLOR_BY_ENTITY.remove(e.getUUID());
    }

    public static void clear(UUID uuid) {
        if (uuid == null) return;
        COLOR_BY_ENTITY.remove(uuid);
    }

    public static void clearAll() {
        COLOR_BY_ENTITY.clear();
        COLOR_BY_OWNER.clear();
    }

    public static void set(String owner, Entity entity, int rgb) {
        if (owner == null || entity == null) return;
        COLOR_BY_OWNER.computeIfAbsent(owner, ignored -> new ConcurrentHashMap<>())
                .put(entity.getUUID(), rgb & 0xFFFFFF);
    }

    public static void clear(String owner, UUID uuid) {
        if (owner == null || uuid == null) return;
        Map<UUID, Integer> colors = COLOR_BY_OWNER.get(owner);
        if (colors != null) colors.remove(uuid);
    }

    public static boolean has(Entity e) {
        if (e == null) return false;
        if (COLOR_BY_ENTITY.containsKey(e.getUUID())) return true;
        return COLOR_BY_OWNER.values().stream().anyMatch(colors -> colors.containsKey(e.getUUID()));
    }

    public static int getColorOrDefault(Entity e) {
        int fallback = 0xFFFFFF;
        if (e == null) return fallback;
        for (Map<UUID, Integer> colors : COLOR_BY_OWNER.values()) {
            Integer color = colors.get(e.getUUID());
            if (color != null) return color & 0xFFFFFF;
        }
        return COLOR_BY_ENTITY.getOrDefault(e.getUUID(), fallback) & 0xFFFFFF;
    }
}
