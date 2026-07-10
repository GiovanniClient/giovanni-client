package rocket.giovanniclient.client.features.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class MineshaftArmorStandESP extends AbstractFeature {

    private static final int SCAN_EVERY_TICKS = 10;
    private static final int YOG_RED = 0xFF5555;
    private static final int MINERAL_GRAY = 0xAAAAAA;
    private static final int LAPIS_BLUE = 0x5555FF;

    private final Map<UUID, Integer> appliedColors = new HashMap<>();
    private int tickCounter = 0;

    @Override
    public void onTick(Minecraft client) {
        if (client == null || client.level == null || client.player == null) {
            clearApplied();
            return;
        }

        var rc = ConfigManager.getConfig().rc.renderEntitiesAccordion;

        if (!rc.MINESHAFT_CORPSES_ESP || !PlayerLocator.isPlayerIn("Mineshaft")) {
            clearApplied();
            return;
        }

        tickCounter++;
        if (tickCounter % SCAN_EVERY_TICKS != 0) return;

        Map<UUID, Integer> desiredColors = new HashMap<>();

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand armorStand)) continue;

            Integer color = colorForChestplate(armorStand.getItemBySlot(EquipmentSlot.CHEST));
            if (color == null) continue;

            desiredColors.put(armorStand.getUUID(), color);
            if (!color.equals(appliedColors.get(armorStand.getUUID()))) {
                GlowOverrideManager.set(armorStand, color);
                appliedColors.put(armorStand.getUUID(), color);
            }
        }

        for (UUID uuid : new HashSet<>(appliedColors.keySet())) {
            if (!desiredColors.containsKey(uuid)) {
                GlowOverrideManager.clear(uuid);
                appliedColors.remove(uuid);
            }
        }
    }

    @Override
    public void onWorldLoad(Minecraft client) {
        clearApplied();
    }

    private void clearApplied() {
        for (UUID uuid : appliedColors.keySet()) {
            GlowOverrideManager.clear(uuid);
        }
        appliedColors.clear();
    }

    private static Integer colorForChestplate(ItemStack chestplate) {
        if (chestplate == null || chestplate.isEmpty()) return null;

        CustomData customData = chestplate.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;

        return customData.copyTag().getString("id")
                .map(MineshaftArmorStandESP::colorForSkyBlockId)
                .orElse(null);
    }

    private static Integer colorForSkyBlockId(String id) {
        return switch (id) {
            case "ARMOR_OF_YOG_CHESTPLATE" -> YOG_RED;
            case "MINERAL_CHESTPLATE" -> MINERAL_GRAY;
            case "LAPIS_ARMOR_CHESTPLATE" -> LAPIS_BLUE;
            default -> null;
        };
    }
}
