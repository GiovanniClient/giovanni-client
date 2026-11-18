package rocket.giovanniclient.client.features.pokedex;

import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.TreeMap;

/** Extracts stable identifying data from an entity. */
final class EntityFingerprint {
    private EntityFingerprint() {}

    static boolean supports(Entity entity) {
        return entity instanceof LivingEntity || entity instanceof Display || entity instanceof FallingBlockEntity;
    }

    static boolean eligible(Entity entity) {
        if (!supports(entity) || !entity.isAlive()) return false;
        if (entity instanceof Player player) {
            Minecraft client = Minecraft.getInstance();
            // Hypixel's fake players generally have v2/v3 UUIDs. Never learn real players.
            return player != client.player && player.getUUID().version() != 4
                    && (client.getConnection() == null || client.getConnection().getPlayerInfo(player.getUUID()) == null);
        }
        return true;
    }

    static DexMatcher.Sample sample(Entity entity) {
        return new DexMatcher.Sample(entity.getId(), entity.getUUID(), entity.getX(), entity.getY(), entity.getZ(),
                signature(entity), baseMaxHealth(entity), size(entity));
    }

    static Double baseMaxHealth(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity instanceof ArmorStand) return null;
        double value = living.getAttributeBaseValue(Attributes.MAX_HEALTH);
        return Double.isFinite(value) && value > 0 ? value : null;
    }

    static DexProfile.Size size(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return null;
        // Standing dimensions include scale and age without changing with the current pose.
        var dimensions = living.getDimensions(Pose.STANDING);
        double width = roundDimension(dimensions.width());
        double height = roundDimension(dimensions.height());
        return width > 0 && height > 0 ? new DexProfile.Size(width, height) : null;
    }

    private static double roundDimension(float value) {
        return Float.isFinite(value) ? Math.round(value * 1000.0) / 1000.0 : 0;
    }

    static DexProfile.Signature signature(Entity entity) {
        String name = entity.hasCustomName() ? entity.getCustomName().getString()
                : entity instanceof Player ? entity.getName().getString() : "";
        Map<String, String> equipment = new TreeMap<>();
        if (entity instanceof LivingEntity living) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = living.getItemBySlot(slot);
                if (!stack.isEmpty()) equipment.put(slot.getName(), item(stack));
            }
        }
        String appearance = "";
        if (entity instanceof Display.ItemDisplay display) appearance = item(display.getSlot(0).get());
        if (entity instanceof Display.BlockDisplay display && display.blockRenderState() != null) {
            appearance = display.blockRenderState().blockState().toString();
        }
        if (entity instanceof Display.TextDisplay display && display.textRenderState() != null) {
            name = display.textRenderState().text().getString();
        }
        if (entity instanceof FallingBlockEntity block) appearance = block.getBlockState().toString();
        return new DexProfile.Signature(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),
                DexProfile.normalizeName(name), equipment, appearance);
    }

    static DexProfile.Role role(Entity entity) {
        if (entity instanceof Display.TextDisplay) return DexProfile.Role.LABEL;
        if (entity instanceof ArmorStand stand) {
            return signature(stand).equipment().isEmpty() ? DexProfile.Role.LABEL : DexProfile.Role.VISUAL;
        }
        return entity instanceof LivingEntity ? DexProfile.Role.BODY : DexProfile.Role.VISUAL;
    }

    private static String item(ItemStack stack) {
        if (stack.isEmpty()) return "";
        StringBuilder result = new StringBuilder(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        var profile = stack.get(DataComponents.PROFILE);
        if (profile != null) {
            profile.partialProfile().properties().get("textures").stream().map(Property::value)
                    .map(DexProfile::texture).sorted().forEach(value -> result.append("|skin=").append(value));
        }
        var model = stack.get(DataComponents.ITEM_MODEL);
        if (model != null) result.append("|model=").append(model);
        var customModel = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (customModel != null) result.append("|custom=").append(customModel);
        var dye = stack.get(DataComponents.DYED_COLOR);
        if (dye != null) result.append("|dye=").append(dye);
        return result.toString();
    }

}
