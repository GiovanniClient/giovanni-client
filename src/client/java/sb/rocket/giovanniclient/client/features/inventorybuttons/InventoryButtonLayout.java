package sb.rocket.giovanniclient.client.features.inventorybuttons;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public record InventoryButtonLayout(String id, int relX, int relY) {

    private static final int INV_W = 176;
    private static final int INV_H = 166;

    public static final String DEFAULT_ID = "right0";

    // Build once
    private static final List<InventoryButtonLayout> ALL = buildAll();
    private static final Map<String, InventoryButtonLayout> BY_ID =
            ALL.stream().collect(Collectors.toUnmodifiableMap(
                    s -> canon(s.id),
                    s -> s
            ));

    public static List<InventoryButtonLayout> all() {
        return ALL;
    }

    public static InventoryButtonLayout fromId(String id) {
        if (id == null) return null;
        return BY_ID.get(canon(id));
    }

    public static InventoryButtonLayout fromIdOrDefault(String id) {
        InventoryButtonLayout s = fromId(id);
        return s != null ? s : BY_ID.get(canon(DEFAULT_ID));
    }

    public static InventoryButtonLayout defaultSlot() {
        // Lazy getter avoids static init cycles entirely
        return BY_ID.get(canon(DEFAULT_ID));
    }

    private static String canon(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static List<InventoryButtonLayout> buildAll() {
        var s = new java.util.ArrayList<InventoryButtonLayout>();

        // Below crafting
        //s.add(new InventoryButtonLayout("below0", 87, 63));
        s.add(new InventoryButtonLayout("below1", 87 + 21, 63));
        s.add(new InventoryButtonLayout("below2", 87 + 21 * 2, 63));
        s.add(new InventoryButtonLayout("below3", 87 + 21 * 3, 63));

        // Above crafting
        //s.add(new InventoryButtonLayout("above0", 87, 5));
        s.add(new InventoryButtonLayout("above1", 87 + 21, 5));
        s.add(new InventoryButtonLayout("above2", 87 + 21 * 2, 5));
        s.add(new InventoryButtonLayout("above3", 87 + 21 * 3, 5));

        // Crafting square
        s.add(new InventoryButtonLayout("craft00", 97, 25));
        s.add(new InventoryButtonLayout("craft10", 97 + 18, 25));
        s.add(new InventoryButtonLayout("craft01", 97, 25 + 18));
        s.add(new InventoryButtonLayout("craft11", 97 + 18, 25 + 18));

        // Crafting result
        s.add(new InventoryButtonLayout("result", 143, 35));

        // Player menu area
        s.add(new InventoryButtonLayout("player0", 56, 8));
        s.add(new InventoryButtonLayout("player1", 56, 60));
        s.add(new InventoryButtonLayout("player2", 26, 8));
        s.add(new InventoryButtonLayout("player3", 26, 60));

        // Right side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonLayout("right" + i, INV_W + 2, 4 + 21 * i));
        }

        // Left side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonLayout("left" + i, -19, 4 + 21 * i));
        }

        // Top side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonLayout("top" + i, 4 + 21 * i, -19));
        }

        // Bottom side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonLayout("bottom" + i, 4 + 21 * i, INV_H + 2));
        }

        return List.copyOf(s);
    }
}
