package rocket.giovanniclient.client.features.render;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
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

        Map<UUID, ArmorStand> found = new HashMap<>();
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand stand && isRatArmorStand(stand)) {
                found.put(stand.getUUID(), stand);
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

    public static List<RatRenderData> getRenderData(float tickProgress) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return List.of();

        return RATS.keySet().stream()
                .map(uuid -> findRat(client, uuid))
                .filter(Objects::nonNull)
                .filter(ArmorStand::isAlive)
                .filter(RatReplacer::isRatArmorStand)
                .map(stand -> renderData(stand, tickProgress))
                .toList();
    }

    private static RatRenderData renderData(ArmorStand stand, float tickProgress) {
        return new RatRenderData(stand.getPosition(tickProgress), Mth.lerp(tickProgress, stand.yRotO, stand.getYRot()));
    }

    private static ArmorStand findRat(Minecraft client, UUID uuid) {
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof ArmorStand stand && stand.getUUID().equals(uuid)) {
                return stand;
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

        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty() || head.getItem() != Items.PLAYER_HEAD) return false;

        ResolvableProfile profile = head.get(DataComponents.PROFILE);
        if (profile == null) return hasNearbyRatNametag(stand);

        for (Property property : profile.partialProfile().properties().get("textures")) {
            if (isRatTexture(property.value())) return true;
        }
        return hasNearbyRatNametag(stand);
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

    private static boolean isRatNameArmorStand(Entity entity) {
        if (!(entity instanceof ArmorStand armorStand) || !armorStand.hasCustomName()) return false;
        String name = armorStand.getName().getString();
        return name.contains("Rat") && name.contains("Lv");
    }

    public record RatRenderData(Vec3 position, float yRot) {}
}
