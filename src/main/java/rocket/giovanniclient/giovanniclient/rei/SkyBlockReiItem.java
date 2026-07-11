package rocket.giovanniclient.giovanniclient.rei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SkyBlockReiItem {
    private final String id;
    private final String itemId;
    private final String displayName;
    private final List<String> lore;
    private final String nbtTag;
    private final int count;
    private volatile ItemStack stack;

    public SkyBlockReiItem(String id, String itemId, String displayName, List<String> lore, String nbtTag) {
        this(id, itemId, displayName, lore, nbtTag, 1);
    }

    private SkyBlockReiItem(String id, String itemId, String displayName, List<String> lore, String nbtTag, int count) {
        this.id = id;
        this.itemId = itemId;
        this.displayName = displayName;
        this.lore = List.copyOf(lore);
        this.nbtTag = nbtTag;
        this.count = Math.max(1, count);
    }

    public SkyBlockReiItem copy() {
        return this;
    }

    public SkyBlockReiItem withCount(int count) {
        return new SkyBlockReiItem(id, itemId, displayName, lore, nbtTag, count);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public ItemStack stack() {
        ItemStack current = stack;
        if (current == null) {
            current = SkyBlockReiItemRepository.createStack(id, itemId, displayName, lore, nbtTag);
            current.setCount(count);
            stack = current;
        }
        return current;
    }
}
