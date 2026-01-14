package sb.rocket.giovanniclient.client.features.inventorybuttons;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record InventoryButtonSlot(String id, int relX, int relY) {

    private static final int INV_W = 176;
    private static final int INV_H = 166;

    public static final String DEFAULT_ID = "right0";

    // Build once
    private static final List<InventoryButtonSlot> ALL = buildAll();
    private static final Map<String, InventoryButtonSlot> BY_ID =
            ALL.stream().collect(Collectors.toUnmodifiableMap(
                    s -> canon(s.id),
                    s -> s
            ));

    public static List<InventoryButtonSlot> all() {
        return ALL;
    }

    public static InventoryButtonSlot fromId(String id) {
        if (id == null) return null;
        return BY_ID.get(canon(id));
    }

    public static InventoryButtonSlot fromIdOrDefault(String id) {
        InventoryButtonSlot s = fromId(id);
        return s != null ? s : BY_ID.get(canon(DEFAULT_ID));
    }

    public static InventoryButtonSlot defaultSlot() {
        // Lazy getter avoids static init cycles entirely
        return BY_ID.get(canon(DEFAULT_ID));
    }

    private static String canon(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static List<InventoryButtonSlot> buildAll() {
        var s = new java.util.ArrayList<InventoryButtonSlot>();

        // Below crafting
        s.add(new InventoryButtonSlot("below0", 87, 63));
        s.add(new InventoryButtonSlot("below1", 87 + 21, 63));
        s.add(new InventoryButtonSlot("below2", 87 + 21 * 2, 63));
        s.add(new InventoryButtonSlot("below3", 87 + 21 * 3, 63));

        // Above crafting
        s.add(new InventoryButtonSlot("above0", 87, 5));
        s.add(new InventoryButtonSlot("above1", 87 + 21, 5));
        s.add(new InventoryButtonSlot("above2", 87 + 21 * 2, 5));
        s.add(new InventoryButtonSlot("above3", 87 + 21 * 3, 5));

        // Crafting square
        s.add(new InventoryButtonSlot("craft00", 87, 25));
        s.add(new InventoryButtonSlot("craft10", 87 + 18, 25));
        s.add(new InventoryButtonSlot("craft01", 87, 25 + 18));
        s.add(new InventoryButtonSlot("craft11", 87 + 18, 25 + 18));

        // Crafting result
        s.add(new InventoryButtonSlot("result", 143, 35));

        // Player menu area
        s.add(new InventoryButtonSlot("player0", 60, 8));
        s.add(new InventoryButtonSlot("player1", 60, 60));
        s.add(new InventoryButtonSlot("player2", 26, 8));
        s.add(new InventoryButtonSlot("player3", 26, 60));

        // Right side
        for (int i = 0; i < 8; i++) {
            int y = 2 + 20 * i;
            if (y < 80) s.add(new InventoryButtonSlot("right" + i, INV_W + 2, y));
        }

        // Left side
        for (int i = 0; i < 8; i++) {
            int y = 2 + 20 * i;
            if (y < 80) s.add(new InventoryButtonSlot("left" + i, -19, y));
        }

        // Top side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonSlot("top" + i, 4 + 21 * i, -19));
        }

        // Bottom side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonSlot("bottom" + i, 4 + 21 * i, INV_H + 2));
        }

        return List.copyOf(s);
    }
}
