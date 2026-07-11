package rocket.giovanniclient.client.features.render;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;

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
    public void onTick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            clearPreviouslyApplied();
            desired.clear();
            previouslyApplied.clear();
            return;
        }

        var rc = ConfigManager.getConfig().rc.renderEntitiesAccordion;

        if (!rc.DUNGEON_ESP || !isInCatacombs()) {
            clearPreviouslyApplied();
            desired.clear();
            previouslyApplied.clear();
            return;
        }

        tickCounter++;
        if (tickCounter % SCAN_EVERY_TICKS != 0) return;

        desired.clear();

        // Find starred armor stands
        for (Entity e : client.level.entitiesForRendering()) {
            if (!(e instanceof ArmorStand as)) continue;
            if (!as.hasCustomName()) continue;

            if (!containsAnyStar(getPlainName(as.getCustomName()))) continue;

            Entity target = findClosestLivingUnderArmorStand(client, as);
            if (target instanceof LivingEntity living && living != client.player) {
                desired.add(living.getUUID());
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

    private static Entity findClosestLivingUnderArmorStand(Minecraft client, ArmorStand stand) {
        AABB box = stand.getBoundingBox().inflate(1.0, 3.0, 1.0).move(0.0, -1.2, 0.0);

        Entity closest = null;
        double best = Double.MAX_VALUE;

        for (Entity e : client.level.getEntities(stand, box, StarredMobESP::isValidStarredMobTarget)) {
            double d = stand.distanceToSqr(e);
            if (d < best) {
                best = d;
                closest = e;
            }
        }
        return closest;
    }

    private static boolean isValidStarredMobTarget(Entity entity) {
        return entity.isAlive()
                && entity instanceof LivingEntity
                && (entity instanceof Mob || entity instanceof Player);
    }

    private static boolean containsAnyStar(String s) {
        if (s == null || s.isEmpty()) return false;
        for (String star : STAR_MARKERS) {
            if (s.contains(star)) return true;
        }
        return false;
    }

    private static String getPlainName(Component t) {
        return Objects.requireNonNullElse(t, Component.empty()).getString();
    }

    private static Entity findEntityByUuid(Minecraft client, UUID uuid) {
        if (client.level == null) return null;

        for (Entity e : client.level.entitiesForRendering()) {
            if (e.getUUID().equals(uuid)) return e;
        }

        return null;
    }
}
