package rocket.giovanniclient.client.features.inventorybuttons.icons;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.client.util.Utils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class IconStackCodec {
    private static final String STACK_PREFIX = "stack:";
    private static final ItemStack FALLBACK = new ItemStack(Items.PAPER);
    private static final Map<String, ItemStack> STACK_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ItemStack> eldest) {
            return size() > 128;
        }
    };

    private IconStackCodec() {}

    public static String encode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "minecraft:paper";
        }

        RegistryOps<JsonElement> ops = registryOps();
        if (ops == null) {
            return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        }

        return ItemStack.CODEC.encodeStart(ops, stack)
                .resultOrPartial(error -> Utils.LOGGER.warn("Failed to encode inventory button icon: {}", error))
                .map(json -> STACK_PREFIX + json)
                .orElseGet(() -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    public static ItemStack decode(String icon) {
        if (icon == null || icon.isBlank()) {
            return FALLBACK.copy();
        }

        if (icon.startsWith(STACK_PREFIX)) {
            ItemStack cached = STACK_CACHE.get(icon);
            if (cached != null) {
                return cached.copy();
            }

            ItemStack decoded = decodeStack(icon.substring(STACK_PREFIX.length()));
            if (!decoded.isEmpty()) {
                STACK_CACHE.put(icon, decoded.copy());
            }
            return decoded.isEmpty() ? FALLBACK.copy() : decoded;
        }

        return decodeItemId(icon);
    }

    private static ItemStack decodeStack(String jsonText) {
        RegistryOps<JsonElement> ops = registryOps();
        if (ops == null) {
            return ItemStack.EMPTY;
        }

        try {
            JsonElement json = JsonParser.parseString(jsonText);
            return ItemStack.CODEC.parse(ops, json)
                    .resultOrPartial(error -> Utils.LOGGER.warn("Failed to decode inventory button icon: {}", error))
                    .orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            Utils.error("Failed to parse inventory button icon JSON.", e);
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack decodeItemId(String icon) {
        Identifier id = Identifier.tryParse(icon);
        if (id == null) {
            return FALLBACK.copy();
        }

        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }

    private static RegistryOps<JsonElement> registryOps() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return null;
        }

        return client.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
    }
}
