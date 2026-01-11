package sb.rocket.giovanniclient.client.features.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.AbstractFeature;
import sb.rocket.giovanniclient.client.util.PlayerLocator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Highlights starred dungeon mobs (client-side only).
 * Active only in The Catacombs.
 */
public class StarredMobESP extends AbstractFeature {

    private static final String[] STAR_MARKERS = {"✯", "✰", "★", "☆"};
    private static final int STAR_YELLOW = 0xFFFF55;
    private static final int SCAN_EVERY_TICKS = 10;

    private final Set<UUID> desired = new HashSet<>();
    private final Set<UUID> previouslyApplied = new HashSet<>();

    private int tickCounter = 0;

    @Override
    public void onTick(MinecraftClient client) {
        if (client == null || client.world == null) return;

        var rc = ConfigManager.getConfig().rc;

        if (!rc.STARRED_MOB_ESP || !isInCatacombs()) {
            clearPreviouslyApplied();
            desired.clear();
            previouslyApplied.clear();
            return;
        }

        tickCounter++;
        if (tickCounter % SCAN_EVERY_TICKS != 0) return;

        desired.clear();

        // Find starred armor stands
        for (Entity e : client.world.getEntities()) {
            if (!(e instanceof ArmorStandEntity as)) continue;
            if (!as.hasCustomName()) continue;

            if (!containsAnyStar(getPlainName(as.getCustomName()))) continue;

            Entity target = findClosestLivingUnderArmorStand(client, as);
            if (target instanceof LivingEntity living && living != client.player) {
                desired.add(living.getUuid());
            }
        }

        // Remove no-longer-needed glow
        for (UUID old : new HashSet<>(previouslyApplied)) {
            if (!desired.contains(old)) {
                GlowOverrideManager.clear(old);
                previouslyApplied.remove(old);
            }
        }

        // Apply glow
        for (UUID id : desired) {
            if (previouslyApplied.contains(id)) continue;

            Entity ent = findEntityByUuid(client, id);
            if (ent instanceof LivingEntity living) {
                GlowOverrideManager.set(living, STAR_YELLOW);
                previouslyApplied.add(id);
            }
        }
    }

    private static boolean isInCatacombs() {
        String loc = PlayerLocator.getPlayerLocation();
        return loc != null && loc.contains("The Catacombs");
    }

    private void clearPreviouslyApplied() {
        for (UUID old : previouslyApplied) {
            GlowOverrideManager.clear(old);
        }
    }

    private static Entity findClosestLivingUnderArmorStand(MinecraftClient client, ArmorStandEntity stand) {
        Box box = stand.getBoundingBox().expand(1.0, 3.0, 1.0).offset(0.0, -1.2, 0.0);

        Entity closest = null;
        double best = Double.MAX_VALUE;

        for (Entity e : client.world.getOtherEntities(stand, box)) {
            if (!(e instanceof LivingEntity)) continue;
            if (!(e instanceof MobEntity) && !(e instanceof PlayerEntity)) continue;

            double d = stand.squaredDistanceTo(e);
            if (d < best) {
                best = d;
                closest = e;
            }
        }
        return closest;
    }

    private static boolean containsAnyStar(String s) {
        if (s == null || s.isEmpty()) return false;
        for (String star : STAR_MARKERS) {
            if (s.contains(star)) return true;
        }
        return false;
    }

    private static String getPlainName(Text t) {
        return Objects.requireNonNullElse(t, Text.empty()).getString();
    }

    private static Entity findEntityByUuid(MinecraftClient client, UUID uuid) {
        if (client.world == null) return null;
        for (Entity e : client.world.getEntities()) {
            if (e.getUuid().equals(uuid)) return e;
        }
        return null;
    }
}
