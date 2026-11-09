package rocket.giovanniclient.client.features.farming;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.features.farming.FarmingConfig.JellybeanHiderMode;
import rocket.giovanniclient.client.features.pokedex.DexProfile;
import rocket.giovanniclient.client.util.PlayerLocator;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class MagicJellybeanHider extends AbstractFeature {
    private static final String JELLYBEAN_TEXTURE = "c526a56b80f56a6870f891d1d46fa7f8c71494cad24e94326da84b3829417b81";
    private static final int COPPER_CHAIN_HEIGHT = 72;
    private static final int SEMI_HIDDEN_HEIGHT = 75;
    private static final RenderSnapshot EMPTY = new RenderSnapshot(null, false, Set.of(), Map.of());

    // Chunk compilation can read the snapshot from worker threads.
    private static volatile RenderSnapshot snapshot = EMPTY;
    private final Map<String, Boolean> textureMatches = new WeakHashMap<>();

    @Override
    public void onTick(Minecraft client) {
        ClientLevel level = client.level;
        FarmingConfig farming = ConfigManager.getConfig().farming;
        JellybeanHiderMode mode = farming == null ? null : farming.MAGIC_JELLYBEAN_HIDER;
        if (level == null || mode == null || mode == JellybeanHiderMode.DISABLED
                || !"Garden".equals(PlayerLocator.getPlayerArea())) {
            publish(client, EMPTY);
            return;
        }

        boolean copperChain = mode == JellybeanHiderMode.COPPER_CHAIN;
        BlockState replacement = copperChain ? Blocks.COPPER_CHAIN.weathering().oxidized().defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        Set<ArmorStand> jellybeans = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<BlockPos, BlockState> cane = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !isJellybean(stand)) continue;
            jellybeans.add(stand);
            markCane(level, stand.blockPosition(), copperChain, replacement, cane);
            markHeadCane(level, stand, copperChain, replacement, cane);
        }
        publish(client, new RenderSnapshot(level, copperChain,
                Collections.unmodifiableSet(jellybeans), Map.copyOf(cane)));
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        publish(client, EMPTY);
        textureMatches.clear();
    }

    public static boolean shouldHide(Entity entity) {
        RenderSnapshot current = snapshot;
        return entity instanceof ArmorStand stand && stand.level() == current.level()
                && (current.copperChain() ? stand.getY() > COPPER_CHAIN_HEIGHT : stand.getY() >= SEMI_HIDDEN_HEIGHT)
                && current.jellybeans().contains(stand);
    }

    public static BlockState renderCane(ClientLevel level, BlockState original, int x, int y, int z) {
        RenderSnapshot current = snapshot;
        if (level != current.level() || current.cane().isEmpty() || !original.is(Blocks.SUGAR_CANE)) return original;
        return current.cane().getOrDefault(new BlockPos(x, y, z), original);
    }

    private static void markHeadCane(ClientLevel level, ArmorStand stand, boolean copperChain,
                                     BlockState replacement, Map<BlockPos, BlockState> cane) {
        // Use the ArmorStandModel and CustomHeadLayer transforms. Marker stands
        // still render the skull even when their hitbox is empty.
        var pose = stand.getHeadPose();
        Vector3f up = new Quaternionf().rotationZYX(pose.z() * Mth.DEG_TO_RAD, pose.y() * Mth.DEG_TO_RAD,
                pose.x() * Mth.DEG_TO_RAD).transformInverse(0, -1, 0, new Vector3f());
        boolean small = stand.isSmall();
        double scale = stand.getScale();
        double headPartScale = small ? 0.75 : 1;
        double headPivotPixels = (small ? 17 : 1) * headPartScale;
        double skullScale = scale * headPartScale * 1.1875;
        double pivotY = stand.getY() + scale * (1.501 - headPivotPixels / 16);
        double centerY = pivotY - up.y * skullScale * (4.0 / 16);
        double radiusY = (Math.abs(up.x) + Math.abs(up.y) + Math.abs(up.z)) * skullScale * (4.25 / 16);

        BlockPos feet = stand.blockPosition();
        int firstY = Math.max(feet.getY() + 1, Mth.floor(centerY - radiusY));
        int endY = Mth.ceil(centerY + radiusY);
        for (int y = firstY; y < endY; y++) {
            markCane(level, new BlockPos(feet.getX(), y, feet.getZ()), copperChain, replacement, cane);
        }
    }

    private static void markCane(ClientLevel level, BlockPos pos, boolean copperChain,
                                 BlockState replacement, Map<BlockPos, BlockState> cane) {
        if ((copperChain || pos.getY() > SEMI_HIDDEN_HEIGHT) && level.getBlockState(pos).is(Blocks.SUGAR_CANE)) {
            cane.put(pos, replacement);
        }
    }

    private boolean isJellybean(ArmorStand stand) {
        var head = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(Items.PLAYER_HEAD)) return false;
        var profile = head.get(DataComponents.PROFILE);
        if (profile == null) return false;
        for (var texture : profile.partialProfile().properties().get("textures")) {
            if (textureMatches.computeIfAbsent(texture.value(), value -> JELLYBEAN_TEXTURE.equals(DexProfile.texture(value)))) {
                return true;
            }
        }
        return false;
    }

    private static void publish(Minecraft client, RenderSnapshot next) {
        RenderSnapshot previous = snapshot;
        if (next == previous) return;
        snapshot = next;

        if (previous.level() == client.level) {
            for (BlockPos pos : previous.cane().keySet()) {
                if (!next.cane().containsKey(pos)) rebuildAt(client, pos);
            }
        }
        next.cane().forEach((pos, state) -> {
            if (previous.level() != next.level() || previous.cane().get(pos) != state) rebuildAt(client, pos);
        });
    }

    private static void rebuildAt(Minecraft client, BlockPos pos) {
        client.levelExtractor.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
    }

    private record RenderSnapshot(ClientLevel level, boolean copperChain, Set<ArmorStand> jellybeans, Map<BlockPos, BlockState> cane) {}
}
