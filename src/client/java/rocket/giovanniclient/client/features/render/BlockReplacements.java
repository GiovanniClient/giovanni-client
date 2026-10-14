package rocket.giovanniclient.client.features.render;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import rocket.giovanniclient.client.config.ConfigManager;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Visual states used only by chunk rendering, never by the actual client world. */
public final class BlockReplacements {
    // Chunk compilation runs on worker threads; publish an immutable snapshot.
    private static volatile Map<BlockState, BlockState> renderStates = Map.of();

    private BlockReplacements() {}

    public static BlockState renderState(BlockState original) {
        return renderStates.getOrDefault(original, original);
    }

    public static Map<String, String> entries() {
        Map<String, String> configured = ConfigManager.getConfig().rc.blockReplacements;
        return configured == null ? Map.of() : new LinkedHashMap<>(configured);
    }

    public static void put(Identifier oldBlock, Identifier newBlock) {
        String error = validationError(oldBlock, newBlock);
        if (error != null) throw new IllegalArgumentException(error);
        Map<String, String> configured = new LinkedHashMap<>(entries());
        configured.put(oldBlock.toString(), newBlock.toString());
        ConfigManager.getConfig().rc.blockReplacements = configured;
        changed();
    }

    public static boolean remove(Identifier oldBlock) {
        Map<String, String> configured = new LinkedHashMap<>(entries());
        if (configured.remove(oldBlock.toString()) == null) return false;
        ConfigManager.getConfig().rc.blockReplacements = configured;
        changed();
        return true;
    }

    public static void clear() {
        ConfigManager.getConfig().rc.blockReplacements = new LinkedHashMap<>();
        changed();
    }

    public static void refresh() {
        Map<BlockState, BlockState> states = new HashMap<>();
        for (Map.Entry<String, String> entry : entries().entrySet()) {
            Identifier oldId = entry.getKey() == null ? null : Identifier.tryParse(entry.getKey());
            Identifier newId = entry.getValue() == null ? null : Identifier.tryParse(entry.getValue());
            if (oldId == null || newId == null
                    || !BuiltInRegistries.BLOCK.containsKey(oldId)
                    || !BuiltInRegistries.BLOCK.containsKey(newId)) continue;
            if (validationError(oldId, newId) != null) continue;

            Block oldBlock = BuiltInRegistries.BLOCK.getValue(oldId);
            Block newBlock = BuiltInRegistries.BLOCK.getValue(newId);
            for (BlockState original : oldBlock.getStateDefinition().getPossibleStates()) {
                BlockState replacement = replacementState(original, newBlock);
                // The render region's fluid lookup still reads the real world.
                // Keep incompatible wet states unchanged instead of hiding water.
                if (!original.getFluidState().equals(replacement.getFluidState())) continue;
                states.put(original, replacement);
            }
        }
        renderStates = Map.copyOf(states);
    }

    public static String validationError(Identifier oldId, Identifier newId) {
        for (Identifier id : new Identifier[]{oldId, newId}) {
            if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return "Unknown block: " + id;
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block instanceof LiquidBlock) return "Fluid blocks are not supported: " + id;
            if (block.defaultBlockState().hasBlockEntity()) return "Blocks with a dedicated block entity renderer are not supported: " + id;
        }
        Block newBlock = BuiltInRegistries.BLOCK.getValue(newId);
        if (!newBlock.defaultBlockState().isAir() && newBlock.defaultBlockState().getRenderShape() != RenderShape.MODEL) {
            return "The replacement block has no regular block model: " + newId;
        }
        return null;
    }

    public static boolean hasUnchangedFluidStates(Identifier oldId, Identifier newId) {
        Block oldBlock = BuiltInRegistries.BLOCK.getValue(oldId);
        Block newBlock = BuiltInRegistries.BLOCK.getValue(newId);
        return oldBlock.getStateDefinition().getPossibleStates().stream()
                .anyMatch(original -> !original.getFluidState().equals(replacementState(original, newBlock).getFluidState()));
    }

    private static BlockState replacementState(BlockState original, Block newBlock) {
        BlockState replacement = newBlock.defaultBlockState();
        for (Property<?> property : original.getProperties()) replacement = copyProperty(original, replacement, property);
        return replacement;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState original, BlockState replacement, Property<T> property) {
        if (!replacement.hasProperty(property)) return replacement;
        return replacement.setValue(property, original.getValue(property));
    }

    private static void changed() {
        refresh();
        ConfigManager.saveConfig();
        ConfigManager.reloadChunks();
    }
}
