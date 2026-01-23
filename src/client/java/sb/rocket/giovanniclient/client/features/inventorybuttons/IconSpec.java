package sb.rocket.giovanniclient.client.features.inventorybuttons;

import com.mojang.serialization.DataResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
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

    public enum Kind { ITEM, STACK, TEXTURE }

    public static final String DEFAULT_TEXTURE = "minecraft:textures/item/paper.png";
    public static final String DEFAULT_ITEM = "item:minecraft:paper";

    private static final Identifier PAPER_TEXTURE = Identifier.of("minecraft", "textures/item/paper.png");

    public static Kind kindOf(String raw) {
        if (raw == null) return Kind.TEXTURE;
        String s = raw.trim().toLowerCase();
        if (s.startsWith("stack:")) return Kind.STACK;
        if (s.startsWith("item:") || s.startsWith("icon:")) return Kind.ITEM;
        return Kind.TEXTURE;
    }

    public static Item resolveItem(String raw) {
        if (raw == null) return Items.PAPER;

        String s = raw.trim();
        String low = s.toLowerCase();
        if (!(low.startsWith("item:") || low.startsWith("icon:"))) return Items.PAPER;

        String idStr = s.substring(s.indexOf(':') + 1).trim(); // dopo item:/icon:
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) return Items.PAPER;

        Item item = Registries.ITEM.get(id);
        return (item == Items.AIR) ? Items.PAPER : item;
    }

    public static Identifier resolveTexture(String raw) {
        if (raw == null || raw.isBlank()) return PAPER_TEXTURE;
        Identifier parsed = Identifier.tryParse(raw.trim());
        return parsed != null ? parsed : PAPER_TEXTURE;
    }

    public static ItemStack resolveStack(String raw) {
        if (raw == null) return new ItemStack(Items.PAPER);

        String s = raw.trim();
        if (!s.toLowerCase().startsWith("stack:")) {
            return new ItemStack(resolveItem(DEFAULT_ITEM));
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

    /** Encode completo (components/NBT) in formato "stack:{...}" usando ItemStack.CODEC */
    public static String encodeStackSpec(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return DEFAULT_ITEM;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null) {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            return "item:" + (id == null ? "minecraft:paper" : id.toString());
        }

        try {
            RegistryOps<NbtElement> ops = mc.world.getRegistryManager().getOps(NbtOps.INSTANCE);
            DataResult<NbtElement> encoded = ItemStack.CODEC.encodeStart(ops, stack);

            Optional<NbtElement> opt = encoded.result();
            if (opt.isPresent() && opt.get() instanceof NbtCompound c) {
                return "stack:" + c.toString();
            }

            Identifier id = Registries.ITEM.getId(stack.getItem());
            return "item:" + (id == null ? "minecraft:paper" : id.toString());
        } catch (Throwable t) {
            Identifier id = Registries.ITEM.getId(stack.getItem());
            return "item:" + (id == null ? "minecraft:paper" : id.toString());
        }
    }

    /** Render 16x16 icon at x,y. */
    public static void renderIcon(DrawContext ctx, String raw, int x, int y) {
        Kind k = kindOf(raw);

        if (k == Kind.STACK) {
            ctx.drawItem(resolveStack(raw), x, y);
            return;
        }

        if (k == Kind.ITEM) {
            ctx.drawItem(new ItemStack(resolveItem(raw)), x, y);
            return;
        }

        Identifier tex = resolveTexture(raw);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, 16, 16, 16, 16);
    }
}
