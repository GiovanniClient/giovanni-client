package sb.rocket.giovanniclient.client.features.rift;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.AbstractFeature;
import sb.rocket.giovanniclient.client.util.PlayerLocator;
import sb.rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoAgaricusCap extends AbstractFeature {

    private final RiftConfig cfg = ConfigManager.getConfig().riftconfig;

    private BlockPos trackedPos = null;
    private boolean waitingForRed = false;

    private int hitDelayTicks = -1;

    @Override
    public void onTick(MinecraftClient client) {
        if (!cfg.AUTOAGARICUS_CAP_TOGGLE) return;
        if (client == null || client.player == null || client.world == null || client.interactionManager == null) return;

        // if in Area: The Rift
        if (PlayerLocator.isPlayerIn("The Rift")) {
            reset();
            return;
        }

        // Main hand: golden hoe + "Wand of Farming"
        if (!isHoldingWandOfFarming(client.player.getMainHandStack())) {
            reset();
            return;
        }

        HitResult hr = client.crosshairTarget;
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) {
            // if waiting, keep waiting without requesting hit
            tickWaitForRed(client, null);
            return;
        }

        BlockPos pos = bhr.getBlockPos();
        BlockState state = client.world.getBlockState(pos);

        // keep waiting and check what we're looking at
        if (waitingForRed) {
            tickWaitForRed(client, bhr);
            return;
        }

        // start tracking
        if (state.isOf(Blocks.BROWN_MUSHROOM)) {
            trackedPos = pos.toImmutable();
            waitingForRed = true;
        }
    }

    private void tickWaitForRed(MinecraftClient client, BlockHitResult bhr) {
        if (!waitingForRed || trackedPos == null) return;

        BlockState now = client.world.getBlockState(trackedPos);
        if (!now.isOf(Blocks.RED_MUSHROOM)) return;

        if (bhr == null || !bhr.getBlockPos().equals(trackedPos)) return;

        // init random delay ONCE
        if (hitDelayTicks < 0) {
            hitDelayTicks = ThreadLocalRandom.current().nextInt(1, 7); // 1–4 ticks
            Utils.debug("delay: " + hitDelayTicks);
            return;
        }

        if (hitDelayTicks-- > 0) return;

        // swing + break exactly once
        client.player.swingHand(Hand.MAIN_HAND);
        client.interactionManager.attackBlock(trackedPos, bhr.getSide());

        reset();
    }

    private boolean isHoldingWandOfFarming(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.isOf(Items.GOLDEN_HOE)) return false;
        String name = stack.getName().getString();
        return name != null && name.contains("Wand of Farming");
    }

    private void reset() {
        trackedPos = null;
        waitingForRed = false;
        hitDelayTicks = -1;
    }
}
