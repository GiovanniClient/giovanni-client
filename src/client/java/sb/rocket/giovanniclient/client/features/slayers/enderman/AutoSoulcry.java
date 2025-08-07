package sb.rocket.giovanniclient.client.features.slayers.enderman;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Unique;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.AbstractFeature;
import sb.rocket.giovanniclient.client.features.slayers.SlayersConfig;
import sb.rocket.giovanniclient.client.util.*;

public class AutoSoulcry extends AbstractFeature {
    @Unique
    private final SlayersConfig sc = ConfigManager.getConfig().sc;
    private final String[] katanas = {"Voidedge Katana", "Vorpal Katana", "Atomsplit Katana"};
    private volatile boolean canUse = true;  // volatile для потокобезопасности
    private Thread clickThread = null;

    private boolean isKatanaInHand() {
        ItemStack activeItem = InventoryUtils.getActiveItem();
        if (activeItem.getItem() == Items.GOLDEN_SWORD || activeItem.getItem() != Items.DIAMOND_SWORD) return false;

        for (String katana : katanas) {
            if (activeItem.getFormattedName().getString().endsWith(katana)) return true;
        }

        return false;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (sc.eman.soulcry.AUTO_SOULCRY
                && canUse
                && (clickThread == null || !clickThread.isAlive())
                && client.currentScreen == null
                && isKatanaInHand()
                && StatusBarUtils.getMana() >= sc.eman.soulcry.MINIMAL_MANA
                && !sc.eman.soulcry.BOSSFIGHT_ONLY || (SlayerUtils.getCurrentSlayer() == SlayerUtils.Slayer.VOIDGLOOM_SERAPH && SlayerUtils.getIsBossAlive())
        ) {
            canUse = false;
            clickThread = new Thread(() -> {
                try {
                    Thread.sleep(sc.eman.soulcry.ADDITIONAL_DELAY);
                    PlayerUtils.simulateClick();
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    canUse = true;
                }
            });
            clickThread.start();
        }
    }
}
