package rocket.giovanniclient.client.features.slayers.enderman;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.features.slayers.SlayersConfig;
import rocket.giovanniclient.client.util.InventoryUtils;
import rocket.giovanniclient.client.util.PlayerUtils;
import rocket.giovanniclient.client.util.SlayerUtils;
import rocket.giovanniclient.client.util.StatusBarUtils;

public class AutoSoulcry extends AbstractFeature {
    private final SlayersConfig sc = ConfigManager.getConfig().sc;
    private final String[] katanas = {"Voidedge Katana", "Vorpal Katana", "Atomsplit Katana"};
    private long pendingUseAtMillis = -1;
    private long cooldownUntilMillis = -1;


    @Override
    public void onTick(Minecraft client) {
        if (!sc.eman.soulcry.AUTO_SOULCRY) {
            resetTiming();
            return;
        }

        long now = System.currentTimeMillis();
        if (pendingUseAtMillis >= 0) {
            if (now < pendingUseAtMillis) {
                return;
            }

            pendingUseAtMillis = -1;
            if (canUseSoulcry(client)) {
                PlayerUtils.simulateClick();
                cooldownUntilMillis = now + 4_000;
            }
            return;
        }

        if (now < cooldownUntilMillis || !canUseSoulcry(client)) {
            return;
        }

        pendingUseAtMillis = now + Math.max(0, sc.eman.soulcry.ADDITIONAL_DELAY);
    }

    @Override
    public void onWorldUnload(Minecraft client) {
        resetTiming();
    }

    private boolean canUseSoulcry(Minecraft client) {
        return client.player != null
                && client.screen == null
                && isKatanaInHand()
                && StatusBarUtils.getMana() >= sc.eman.soulcry.MINIMAL_MANA
                && SlayerUtils.getCurrentSlayer() == SlayerUtils.Slayer.VOIDGLOOM_SERAPH
                && SlayerUtils.isBossAlive();
    }

    private void resetTiming() {
        pendingUseAtMillis = -1;
        cooldownUntilMillis = -1;
    }

    private boolean isKatanaInHand() {
        ItemStack activeItem = InventoryUtils.getHeldItem();
        if (!activeItem.is(Items.DIAMOND_SWORD) || activeItem.getCustomName() == null) return false;

        for (String katana : katanas) {
            if (activeItem.getCustomName().getString().contains(katana)) return true;
        }

        return false;
    }
}
