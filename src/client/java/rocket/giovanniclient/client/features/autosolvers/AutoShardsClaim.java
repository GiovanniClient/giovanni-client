package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.InventoryUtils;
import rocket.giovanniclient.client.util.Utils;

import java.util.Random;

import static rocket.giovanniclient.client.util.InventoryUtils.clickSlot;
import static rocket.giovanniclient.client.util.InventoryUtils.findItemByName;

public class AutoShardsClaim extends AbstractFeature {
    private final AutoSolversConfig cfg = ConfigManager.getConfig().asc;
    private final Random rng = new Random();
    private String shardToClaim = cfg.autoFusionAccordtion.NAME_OF_THE_SHARD_TO_CLAIM;
    private boolean shouldClaim = false;
    private long clickDelay = -1;

    @Override
    public void onScreenOpen(Screen screen) {
        if (screen instanceof GenericContainerScreen && cfg.autoFusionAccordtion.AUTOSHARDSCLAIM) {
            String name = screen.getTitle().getString();
            if (name.contains("Hunting Box")) {
                shardToClaim = cfg.autoFusionAccordtion.NAME_OF_THE_SHARD_TO_CLAIM;
                shouldClaim = true;
                Utils.debug("Hunting Box detected");
            }
        } else {
            shouldClaim = false;
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!shouldClaim || !cfg.autoFusionAccordtion.AUTOSHARDSCLAIM || client.player == null)
            return;

        if (!(client.currentScreen instanceof GenericContainerScreen))
            return;

        ScreenHandler handler = client.player.currentScreenHandler;
        int slot = findItemByName(handler, shardToClaim);

        if (slot != -1) {
            long now = System.currentTimeMillis();
            if (clickDelay == -1) {
                clickDelay = now + rng.nextInt(cfg.autoFusionAccordtion.AUTOFUSION_CLICK_DELAY_MAX - cfg.autoFusionAccordtion.AUTOFUSION_CLICK_DELAY_MIN)
                        + cfg.autoFusionAccordtion.AUTOFUSION_CLICK_DELAY_MIN;
                Utils.debug("Click delay: " + (clickDelay - now) + "ms");
            } else if (now > clickDelay) {
                Utils.debug(shardToClaim + " found in slot " + slot);
                clickSlot(client, handler, slot, InventoryUtils.MouseButton.RIGHT, SlotActionType.PICKUP);
                clickDelay = -1;
            }
        }
    }
}
