package rocket.giovanniclient.client.features.render;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RatReplacer extends AbstractFeature {
    private static final String RAT_TEXTURE_HASH = "a8abb471db0ab78703011979dc8b40798a941f3a4dec3ec61cbeec2af8cffe8";
    private static final Map<UUID, Boolean> RATS = new ConcurrentHashMap<>();

    private int tickCounter = 0;

    @Override
    public void onTick(Minecraft client) {
        if (client == null || client.level == null || !isEnabled()) {
            RATS.clear();
            return;
        }

        tickCounter++;
        if (tickCounter % 20 != 0) return;

        Map<UUID, Entity> found = new HashMap<>();
        for (var entity : client.level.entitiesForRendering()) {
            if (isRatEntity(entity)) {
                found.put(entity.getUUID(), entity);
            }
        }

        found.keySet().forEach(uuid -> RATS.put(uuid, Boolean.TRUE));
        for (UUID uuid : new HashSet<>(RATS.keySet())) {
            if (!found.containsKey(uuid)) {
                RATS.remove(uuid);
            }
        }
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        RATS.clear();
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        RATS.clear();
    }

    public static boolean shouldReplace(ArmorStand stand) {
        boolean shouldReplace = isEnabled() && isRatArmorStand(stand);
        if (shouldReplace) {
            RATS.put(stand.getUUID(), Boolean.TRUE);
        }
        return shouldReplace;
    }

    public static boolean shouldReplace(Display.ItemDisplay display) {
        boolean shouldReplace = isEnabled() && isRatItemDisplay(display);
        if (shouldReplace) {
            RATS.put(display.getUUID(), Boolean.TRUE);
        }
        return shouldReplace;
    }

    public static List<RatRenderData> getRenderData(float tickProgress) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return List.of();

        return RATS.keySet().stream()
                .map(uuid -> findRat(client, uuid))
                .filter(Objects::nonNull)
                .filter(Entity::isAlive)
                .filter(RatReplacer::isRatEntity)
                .map(entity -> renderData(entity, tickProgress))
                .toList();
    }

    private static RatRenderData renderData(Entity entity, float tickProgress) {
        float yOffset = entity instanceof Display.ItemDisplay ? 0.0f : 1.38f;
        return new RatRenderData(
                entity.getPosition(tickProgress),
                Mth.lerp(tickProgress, entity.yRotO, entity.getYRot()),
                yOffset
        );
    }

    private static Entity findRat(Minecraft client, UUID uuid) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

    private static boolean isEnabled() {
        var config = ConfigManager.getConfig();
        return config != null
                && config.rc != null
                && config.rc.renderEntitiesAccordion != null
                && config.fc.RENDER_3D_RATS;
    }

    private static boolean isRatArmorStand(ArmorStand stand) {
        if (stand == null || !stand.isInvisible()) return false;

        // Some rat variants are represented directly by an invisible, marker
        // armor stand whose custom name contains the level and health.  They
        // have no player-head item, so detect that representation before
        // checking the usual textured-head stand.
        // A normal Hypixel rat is represented by two armor stands: one carries
        // the player head and the other carries the name/health label. Render
        // the label stand only for variants that genuinely have no head stand,
        // otherwise the same rat would be submitted twice at two heights.
        if (isRatNameArmorStand(stand)) return !hasNearbyRatHead(stand);

        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty() || head.getItem() != Items.PLAYER_HEAD) return false;

        ResolvableProfile profile = head.get(DataComponents.PROFILE);
        if (profile == null) return hasNearbyRatNametag(stand);

        for (Property property : profile.partialProfile().properties().get("textures")) {
            if (isRatTexture(property.value())) return true;
        }
        return hasNearbyRatNametag(stand);
    }

    private static boolean isRatEntity(Entity entity) {
        if (entity instanceof ArmorStand stand) return isRatArmorStand(stand);
        return entity instanceof Display.ItemDisplay display && isRatItemDisplay(display);
    }

    private static boolean isRatItemDisplay(Display.ItemDisplay display) {
        if (display == null) return false;
        return hasRatTexture(display.getItemStack());
    }

    private static boolean hasRatTexture(ItemStack head) {
        if (head.isEmpty() || head.getItem() != Items.PLAYER_HEAD) return false;

        ResolvableProfile profile = head.get(DataComponents.PROFILE);
        if (profile == null) return false;
        for (Property property : profile.partialProfile().properties().get("textures")) {
            if (isRatTexture(property.value())) return true;
        }
        return false;
    }

    private static boolean isRatTexture(String value) {
        if (value == null || value.isEmpty()) return false;
        if (value.contains(RAT_TEXTURE_HASH)) return true;

        try {
            String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            return decoded.contains(RAT_TEXTURE_HASH);
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean hasNearbyRatNametag(ArmorStand headStand) {
        if (headStand.level() == null) return false;

        return !headStand.level()
                .getEntities(headStand, headStand.getBoundingBox().inflate(0.35, 2.4, 0.35), RatReplacer::isRatNameArmorStand)
                .isEmpty();
    }

    private static boolean hasNearbyRatHead(ArmorStand nameStand) {
        if (nameStand.level() == null) return false;

        return !nameStand.level()
                .getEntities(nameStand, nameStand.getBoundingBox().inflate(0.35, 2.4, 0.35), RatReplacer::isRatHeadCarrier)
                .isEmpty();
    }

    private static boolean isRatHeadCarrier(Entity entity) {
        if (entity instanceof Display.ItemDisplay display) return isRatItemDisplay(display);
        if (!(entity instanceof ArmorStand armorStand) || !armorStand.isInvisible()) return false;
        ItemStack head = armorStand.getItemBySlot(EquipmentSlot.HEAD);
        // Pet rats can use a profile that does not expose the known wild-rat
        // texture hash. The nearby rat nametag already identifies the pair, so
        // any player head on its companion armor stand is enough to dedupe it.
        return !head.isEmpty() && head.getItem() == Items.PLAYER_HEAD;
    }

    private static boolean isRatNameArmorStand(Entity entity) {
        if (!(entity instanceof ArmorStand armorStand) || !armorStand.hasCustomName()) return false;
        String name = armorStand.getName().getString();
        return name.contains("Rat") && name.contains("Lv");
    }

    public record RatRenderData(Vec3 position, float yRot, float yOffset) {}
}
