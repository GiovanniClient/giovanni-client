package sb.rocket.giovanniclient.client.features.inventorybuttons;

import java.util.ArrayList;
import java.util.List;

public record InventoryButtonSlot(String id, int relX, int relY) {

    public static List<InventoryButtonSlot> all() {
        List<InventoryButtonSlot> s = new ArrayList<>();

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

        // Right side (wrap come NEU)
        for (int i = 0; i < 8; i++) {
            int y = 2 + 20 * i;
            if (y < 80) s.add(new InventoryButtonSlot("right" + i, 176 + 2, 2 + 20 * i));          // fuori a destra
            else        s.add(new InventoryButtonSlot("right" + i, 176 + 2, 2 + 20 * i - 166));     // wrap
        }

        // Left side
        for (int i = 0; i < 8; i++) {
            int y = 2 + 20 * i;
            if (y < 80) s.add(new InventoryButtonSlot("left" + i, -19, 2 + 20 * i));               // fuori a sinistra
            else        s.add(new InventoryButtonSlot("left" + i, -19, 2 + 20 * i - 166));          // wrap
        }

        // Top side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonSlot("top" + i, 4 + 21 * i, -19));                             // fuori sopra
        }

        // Bottom side
        for (int i = 0; i < 8; i++) {
            s.add(new InventoryButtonSlot("bottom" + i, 4 + 21 * i, 166 + 2));                      // fuori sotto
        }

        return s;
    }

    public static InventoryButtonSlot fromId(String id) {
        if (id == null) return null;
        String k = id.toLowerCase();
        for (InventoryButtonSlot s : all()) {
            if (s.id.equalsIgnoreCase(k)) return s;
        }
        return null;
    }
}
