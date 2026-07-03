package rocket.giovanniclient.client.features.rift;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;
import rocket.giovanniclient.client.util.Utils;

import java.util.concurrent.ThreadLocalRandom;

public final class AutoAgaricusCap extends AbstractFeature {

    private final RiftConfig cfg = ConfigManager.getConfig().riftconfig;

    private BlockPos trackedPos = null;
    private boolean waitingForRed = false;

    private int hitDelayTicks = -1;

    @Override
    public void onTick(Minecraft client) {
        if (!cfg.AUTOAGARICUS_CAP_TOGGLE) return;
        if (client == null || client.player == null || client.level == null || client.gameMode == null) return;

        // if in Area: The Rift
        if (!PlayerLocator.isPlayerIn("The Rift")) {
            reset();
            return;
        }

        // Main hand: golden hoe + "Wand of Farming"
        if (!isHoldingWandOfFarming(client.player.getMainHandItem())) {
            reset();
            return;
        }

        HitResult hr = client.hitResult;
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) {
            // if waiting, keep waiting without requesting hit
            tickWaitForRed(client, null);
            return;
        }

        BlockPos pos = bhr.getBlockPos();
        BlockState state = client.level.getBlockState(pos);

        // keep waiting and check what we're looking at
        if (waitingForRed) {
            tickWaitForRed(client, bhr);
            return;
        }

        // start tracking
        if (state.is(Blocks.BROWN_MUSHROOM)) {
            trackedPos = pos.immutable();
            waitingForRed = true;
        }
    }

    private void tickWaitForRed(Minecraft client, BlockHitResult bhr) {
        if (!waitingForRed || trackedPos == null) return;

        BlockState now = client.level.getBlockState(trackedPos);
        if (!now.is(Blocks.RED_MUSHROOM)) return;

        if (bhr == null || !bhr.getBlockPos().equals(trackedPos)) return;

        // init random delay ONCE
        if (hitDelayTicks < 0) {
            hitDelayTicks = ThreadLocalRandom.current().nextInt(1, 3); // 1–4 ticks
            Utils.debug("delay: " + hitDelayTicks);
            return;
        }

        if (hitDelayTicks-- > 0) return;

        // swing + break exactly once
        client.player.swing(InteractionHand.MAIN_HAND);
        client.gameMode.startDestroyBlock(trackedPos, bhr.getDirection());

        reset();
    }

    private boolean isHoldingWandOfFarming(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!stack.is(Items.GOLDEN_HOE)) return false;
        String name = stack.getHoverName().getString();
        return name != null && name.contains("Wand of Farming");
    }

    private void reset() {
        trackedPos = null;
        waitingForRed = false;
        hitDelayTicks = -1;
    }
}
