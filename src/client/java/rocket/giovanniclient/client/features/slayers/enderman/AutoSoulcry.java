package rocket.giovanniclient.client.features.slayers.enderman;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Unique;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.features.slayers.SlayersConfig;
import rocket.giovanniclient.client.util.InventoryUtils;
import rocket.giovanniclient.client.util.PlayerUtils;
import rocket.giovanniclient.client.util.SlayerUtils;
import rocket.giovanniclient.client.util.StatusBarUtils;

public class AutoSoulcry extends AbstractFeature {
    @Unique
    private final SlayersConfig sc = ConfigManager.getConfig().sc;
    private final String[] katanas = {"Voidedge Katana", "Vorpal Katana", "Atomsplit Katana"};
    private volatile boolean canUse_thead_safety = true;
    private Thread clickThread = null;


    @Override
    public void onTick(Minecraft client) {
        if (sc.eman.soulcry.AUTO_SOULCRY
                && canUse_thead_safety
                && (clickThread == null || !clickThread.isAlive())
                && client.screen == null
                && isKatanaInHand()
                && StatusBarUtils.getMana() >= sc.eman.soulcry.MINIMAL_MANA
                && (SlayerUtils.getCurrentSlayer() == SlayerUtils.Slayer.VOIDGLOOM_SERAPH && SlayerUtils.isBossAlive())
        ) {
            canUse_thead_safety = false;
            clickThread = new Thread(() -> {
                try {
                    Thread.sleep(sc.eman.soulcry.ADDITIONAL_DELAY);
                    PlayerUtils.simulateClick();
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    canUse_thead_safety = true;
                }
            });
            clickThread.start();
        }
    }

    private boolean isKatanaInHand() {
        ItemStack activeItem = InventoryUtils.getHeldItem();
        if (activeItem.getItem() == Items.GOLDEN_SWORD || activeItem.getItem() != Items.DIAMOND_SWORD) return false;

        for (String katana : katanas) {
            if (activeItem.getCustomName().getString().contains(katana)) return true;
        }

        return false;
    }
}
