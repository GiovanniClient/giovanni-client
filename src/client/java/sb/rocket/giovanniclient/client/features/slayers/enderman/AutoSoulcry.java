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
    private boolean canUse = true;

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
        // TODO: Add boosfight check
        if(sc.eman.soulcry.AUTO_SOULCRY && client.currentScreen == null && isKatanaInHand() && canUse && StatusBarUtils.getMana() > sc.eman.soulcry.MINIMAL_MANA) new Thread(() -> {
            canUse = false;

            try {
                Thread.sleep(sc.eman.soulcry.ADDITIONAL_DELAY);
            } catch (InterruptedException ignored) {}

            PlayerUtils.simulateClick();
            try {
                Thread.sleep(3900);
            } catch (InterruptedException ignored) {}

            canUse = true;
        }).start();
    }
}
