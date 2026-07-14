package rocket.giovanniclient.client.features.slayers.blaze;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.phys.AABB;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class BlazeShieldHighlight extends AbstractFeature {
    private static final int SCAN_EVERY_TICKS = 5;
    private static final Pattern TIMER_PATTERN = Pattern.compile(".*\\d\\d:\\d\\d.*");
    private static final Map<UUID, HighlightColors> HIGHLIGHTS = new ConcurrentHashMap<>();

    private int tickCounter = 0;

    @Override
    public void onTick(Minecraft client) {
        if (client == null || client.level == null || client.player == null || ConfigManager.getConfig() == null) {
            clearApplied();
            return;
        }

        var blazeConfig = ConfigManager.getConfig().sc.blaze;
        if (blazeConfig == null || !blazeConfig.BLAZE_SHIELD_HIGHLIGHT || !PlayerLocator.isPlayerIn("Smoldering Tomb")) {
            clearApplied();
            return;
        }

        tickCounter++;
        if (tickCounter % SCAN_EVERY_TICKS != 0) return;

        Map<UUID, HighlightColors> desiredHighlights = new HashMap<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !stand.hasCustomName()) continue;

            String name = plainName(stand.getCustomName());
            HighlightColor color = colorForAttunement(attunementFromTimerName(name));
            if (color == null) continue;

            Entity boss = findNametagOwner(client, stand);
            if (!(boss instanceof LivingEntity living)) continue;

            desiredHighlights.put(living.getUUID(), new HighlightColors(color.outlineArgb(), color.fillArgb()));
        }

        HIGHLIGHTS.putAll(desiredHighlights);
        for (UUID uuid : new HashSet<>(HIGHLIGHTS.keySet())) {
            if (!desiredHighlights.containsKey(uuid)) {
                HIGHLIGHTS.remove(uuid);
            }
        }
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        clearApplied();
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        clearApplied();
    }

    private void clearApplied() {
        HIGHLIGHTS.clear();
    }

    public static List<RenderedBox> getRenderedBoxes(float tickProgress) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return List.of();

        return HIGHLIGHTS.entrySet().stream()
                .map(entry -> {
                    Entity entity = findEntityByUuid(client, entry.getKey());
                    if (!(entity instanceof LivingEntity living) || !living.isAlive()) return null;

                    HighlightColors colors = entry.getValue();
                    return new RenderedBox(getLerpedBox(living, tickProgress), colors.outlineArgb(), colors.fillArgb());
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private static Entity findNametagOwner(Minecraft client, ArmorStand stand) {
        AABB box = stand.getBoundingBox().inflate(1.0, 3.0, 1.0).move(0.0, -1.2, 0.0);

        Entity closest = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity entity : client.level.getEntities(stand, box, BlazeShieldHighlight::isBlazeBossEntity)) {
            double distance = stand.distanceToSqr(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                closest = entity;
            }
        }

        return closest;
    }

    private static boolean isBlazeBossEntity(Entity entity) {
        return entity.isAlive()
                && (entity instanceof Blaze
                || entity instanceof ZombifiedPiglin
                || entity instanceof WitherSkeleton);
    }

    private static String attunementFromTimerName(String name) {
        if (name == null || !TIMER_PATTERN.matcher(name).matches()) return "";
        int firstSpace = name.indexOf(' ');
        return firstSpace > 0 ? name.substring(0, firstSpace) : "";
    }

    private static HighlightColor colorForAttunement(String attunement) {
        return switch (attunement) {
            case "ASHEN" -> new HighlightColor(0xff000000, 0x55000000);
            case "SPIRIT" -> new HighlightColor(0xffffffff, 0x55ffffff);
            case "AURIC" -> new HighlightColor(0xffffff00, 0x55ffff00);
            case "CRYSTAL" -> new HighlightColor(0xff00ffff, 0x5500ffff);
            default -> null;
        };
    }

    private static AABB getLerpedBox(Entity entity, float tickProgress) {
        return entity.getDimensions(Pose.STANDING).makeBoundingBox(entity.getPosition(tickProgress));
    }

    private static Entity findEntityByUuid(Minecraft client, UUID uuid) {
        if (client.level == null) return null;

        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) return entity;
        }

        return null;
    }

    private static String plainName(Component component) {
        return Objects.requireNonNullElse(component, Component.empty()).getString();
    }

    private record HighlightColor(int outlineArgb, int fillArgb) {}

    private record HighlightColors(int outlineArgb, int fillArgb) {}

    public record RenderedBox(AABB box, int outlineArgb, int fillArgb) {}
}
