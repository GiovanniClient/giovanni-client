package sb.rocket.giovanniclient.client.features.render;

import net.minecraft.entity.Entity;

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

    public static void set(Entity e, int rgb) {
        if (e == null) return;
        COLOR_BY_ENTITY.put(e.getUuid(), rgb & 0xFFFFFF);
    }

    public static void clear(Entity e) {
        if (e == null) return;
        COLOR_BY_ENTITY.remove(e.getUuid());
    }

    public static void clear(UUID uuid) {
        if (uuid == null) return;
        COLOR_BY_ENTITY.remove(uuid);
    }

    public static void clearAll() {
        COLOR_BY_ENTITY.clear();
    }

    public static boolean has(Entity e) {
        return e != null && COLOR_BY_ENTITY.containsKey(e.getUuid());
    }

    public static int getColorOrDefault(Entity e) {
        int fallback = 0xFFFFFF;
        if (e == null) return fallback;
        return COLOR_BY_ENTITY.getOrDefault(e.getUuid(), fallback) & 0xFFFFFF;
    }
}
