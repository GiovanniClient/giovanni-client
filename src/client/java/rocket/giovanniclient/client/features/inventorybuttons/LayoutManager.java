package rocket.giovanniclient.client.features.inventorybuttons;

import rocket.giovanniclient.client.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public record LayoutManager(String id, int relX, int relY) {
    public static final int INV_W = 176;
    public static final int INV_H = 166;

    public static List<LayoutManager> getAvailableSlots() {
        List<LayoutManager> slots = new ArrayList<>();
        var equipment = ConfigManager.getConfig().ibc.EQUIPMENT.get();

        // 1. Right side
        for (int i = 0; i < 8; i++) {
            slots.add(new LayoutManager("right" + i, INV_W + 2, 4 + (21 * i)));
        }

        // 2. Left Side - Conditional based on equipment mod like firmament
        if (equipment == InventoryButtonsConfig.EquipmentSide.None || equipment == InventoryButtonsConfig.EquipmentSide.Right) {
            for (int i = 0; i < 8; i++) {
                slots.add(new LayoutManager("left" + i, -19, 4 + (21 * i)));
            }
        } else if (equipment == InventoryButtonsConfig.EquipmentSide.Left) {
            // Shift the first 4 slots further left to avoid equipment HUD
            for (int i = 0; i < 4; i++) slots.add(new LayoutManager("left" + i, -42, 4 + (21 * i)));
            for (int i = 4; i < 8; i++) slots.add(new LayoutManager("left" + i, -19, 4 + (21 * i)));
        }

        // 3. Horizontal Bars (Top/Bottom)
        for (int i = 0; i < 8; i++) {
            slots.add(new LayoutManager("top" + i, 4 + (21 * i), -19));
            slots.add(new LayoutManager("bottom" + i, 4 + (21 * i), INV_H + 2));
        }

        // 4. Vanilla Overlays (Crafting & Player)
        // Only add 'below0' and 'above0' if the right side isn't blocked by equipment
        boolean rightBlocked = (equipment == InventoryButtonsConfig.EquipmentSide.Right);

        addOffsetRow(slots, "above", 87, 5, rightBlocked);
        addOffsetRow(slots, "below", 87, 63, rightBlocked);

        slots.add(new LayoutManager("result", 143, 35));
        slots.add(new LayoutManager("craft00", 97, 25));
        slots.add(new LayoutManager("craft10", 115, 25));
        slots.add(new LayoutManager("craft01", 97, 43));
        slots.add(new LayoutManager("craft11", 115, 43));

        return slots;
    }

    private static void addOffsetRow(List<LayoutManager> list, String prefix, int startX, int y, boolean skipFirst) {
        for (int i = (skipFirst ? 1 : 0); i < 4; i++) {
            list.add(new LayoutManager(prefix + i, startX + (21 * i), y));
        }
    }

    public static LayoutManager getSlotById(String id) {
        for (LayoutManager slot : getAvailableSlots()) {
            if (slot.id().equals(id)) {
                return slot;
            }
        }
        return null;
    }
}