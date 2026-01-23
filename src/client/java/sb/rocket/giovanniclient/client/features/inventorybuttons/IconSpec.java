package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryOps;
import net.minecraft.util.Identifier;

import java.util.Optional;

public final class IconSpec {
    private IconSpec() {}

    /**
     * Supported formats (user-friendly):
     * - "minecraft:paper" (bare item id, like /give)
     * - "modid:item_name" (bare item id)
     * - "stack:{...}"     (full ItemStack codec SNBT)
     *
     * Backward-compat accepted:
     * - "item:minecraft:paper" / "icon:minecraft:paper"  -> treated as bare item id
     *
     * Removed:
     * - "minecraft:textures/..." (texture paths) -> ignored, fallback to paper
     */
    public enum Kind { ITEM, STACK }

    public static final String DEFAULT_ITEM_ID = "minecraft:paper";

    public static Kind kindOf(String raw) {
        if (raw == null) return Kind.ITEM;
        String s = raw.trim();
        if (s.regionMatches(true, 0, "stack:", 0, "stack:".length())) return Kind.STACK;
        return Kind.ITEM;
    }

    /** Parses item id from raw. Returns minecraft:paper on any failure. */
    public static Item resolveItem(String raw) {
        Identifier id = resolveItemId(raw);
        if (id == null) return Items.PAPER;

        Item item = Registries.ITEM.get(id);
        return (item == Items.AIR) ? Items.PAPER : item;
    }

    /** Parses item identifier from raw. Never returns null; falls back to DEFAULT_ITEM_ID. */
    public static Identifier resolveItemId(String raw) {
        String s = (raw == null) ? "" : raw.trim();
        if (s.isEmpty()) s = DEFAULT_ITEM_ID;

        // Reject legacy texture paths explicitly
        String low = s.toLowerCase();
        if (low.contains("textures/") || low.endsWith(".png")) {
            return Identifier.tryParse(DEFAULT_ITEM_ID);
        }

        // Backward-compat: item:... / icon:...
        if (low.startsWith("item:") || low.startsWith("icon:")) {
            s = s.substring(s.indexOf(':') + 1).trim();
            if (s.isEmpty()) s = DEFAULT_ITEM_ID;
        }

        Identifier id = Identifier.tryParse(s);
        if (id == null) id = Identifier.tryParse(DEFAULT_ITEM_ID);
        return id;
    }

    public static ItemStack resolveStack(String raw) {
        if (raw == null) return new ItemStack(Items.PAPER);

        String s = raw.trim();
        if (!s.regionMatches(true, 0, "stack:", 0, "stack:".length())) {
            return new ItemStack(resolveItem(s));
        }

        String snbt = s.substring("stack:".length()).trim();
        if (snbt.isEmpty()) return new ItemStack(Items.PAPER);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) return new ItemStack(Items.PAPER);

        try {
            NbtCompound nbt = StringNbtReader.readCompound(snbt);
            RegistryOps<NbtElement> ops = mc.world.getRegistryManager().getOps(NbtOps.INSTANCE);

            DataResult<ItemStack> parsed = ItemStack.CODEC.parse(ops, nbt);
            Optional<ItemStack> opt = parsed.result();

            if (opt.isPresent() && !opt.get().isEmpty()) return opt.get();
            return new ItemStack(Items.PAPER);
        } catch (Throwable t) {
            return new ItemStack(Items.PAPER);
        }
    }

    /** Encode in "stack:{...}" (when possible), else bare item id like "minecraft:paper". */
    public static String encodeStackSpec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return DEFAULT_ITEM_ID;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            return (id == null) ? DEFAULT_ITEM_ID : id.toString();
        }

        try {
            RegistryOps<NbtElement> ops = mc.world.getRegistryManager().getOps(NbtOps.INSTANCE);
            DataResult<NbtElement> encoded = ItemStack.CODEC.encodeStart(ops, stack);

            Optional<NbtElement> opt = encoded.result();
            if (opt.isPresent() && opt.get() instanceof NbtCompound c) {
                return "stack:" + c.toString();
            }

            Identifier id = Registries.ITEM.getId(stack.getItem());
            return (id == null) ? DEFAULT_ITEM_ID : id.toString();
        } catch (Throwable t) {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            return (id == null) ? DEFAULT_ITEM_ID : id.toString();
        }
    }

    /** Render 16x16 icon at x,y. */
    public static void renderIcon(DrawContext ctx, String raw, int x, int y) {
        Kind k = kindOf(raw);
        if (k == Kind.STACK) {
            ctx.drawItem(resolveStack(raw), x, y);
            return;
        }

        // ITEM (default)
        ctx.drawItem(new ItemStack(resolveItem(raw)), x, y);
    }
}
