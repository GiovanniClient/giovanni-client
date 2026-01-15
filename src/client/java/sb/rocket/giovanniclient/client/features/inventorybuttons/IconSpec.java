package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class IconSpec {

    private IconSpec() {}

    public enum Kind { ITEM, TEXTURE }

    public static final String DEFAULT_TEXTURE = "minecraft:textures/item/paper.png";
    public static final String DEFAULT_ITEM = "item:minecraft:paper";

    private static final Identifier PAPER_TEXTURE = Identifier.of("minecraft", "textures/item/paper.png");

    public static Kind kindOf(String raw) {
        if (raw == null) return Kind.TEXTURE;
        String s = raw.trim().toLowerCase();
        return s.startsWith("item:") ? Kind.ITEM : Kind.TEXTURE;
    }

    public static Item resolveItem(String raw) {
        if (raw == null) return Items.PAPER;

        String s = raw.trim();
        if (!s.toLowerCase().startsWith("item:")) return Items.PAPER;

        String idStr = s.substring("item:".length()).trim();
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) return Items.PAPER;

        Item item = Registries.ITEM.get(id);
        return (item == null || item == Items.AIR) ? Items.PAPER : item;
    }

    public static Identifier resolveTexture(String raw) {
        if (raw == null || raw.isBlank()) return PAPER_TEXTURE;
        Identifier parsed = Identifier.tryParse(raw.trim());
        return parsed != null ? parsed : PAPER_TEXTURE;
    }

    /** Render 16x16-ish icon at x,y. Works for 18x18 button inner area. */
    public static void renderIcon(DrawContext ctx, String raw, int x, int y) {
        if (kindOf(raw) == Kind.ITEM) {
            Item item = resolveItem(raw);
            ctx.drawItem(new ItemStack(item), x, y);
            return;
        }

        Identifier tex = resolveTexture(raw);
        // 16x16 render into typical slot area
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, 16, 16, 16, 16);
    }
}
