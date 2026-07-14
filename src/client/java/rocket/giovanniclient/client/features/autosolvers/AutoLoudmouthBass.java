package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.InventoryUtils;
import rocket.giovanniclient.client.util.Utils;

import java.util.Locale;
import java.util.Random;

import static rocket.giovanniclient.client.util.InventoryUtils.clickSlot;

public class AutoLoudmouthBass extends AbstractFeature {
    private static final int MIN_DELAY_TICKS = 3;
    private static final int MAX_DELAY_TICKS = 10;

    private final AutoSolversConfig cfg = ConfigManager.getConfig().asc;
    private final Random rng = new Random();

    private boolean loudmouthBassScreen = false;
    private int clickDelayTicks = -1;

    @Override
    public void onScreenOpen(Screen screen) {
        clickDelayTicks = -1;

        if (!cfg.LOUDMOUTH_BASS || !(screen instanceof ContainerScreen)) {
            loudmouthBassScreen = false;
            return;
        }

        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        loudmouthBassScreen = title.contains("duel") && title.contains("bass");

        if (loudmouthBassScreen) {
            Utils.debug("Loudmouth Bass duel inventory detected");
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (!cfg.LOUDMOUTH_BASS
                || !loudmouthBassScreen
                || client.player == null
                || !(client.screen instanceof ContainerScreen)) {
            loudmouthBassScreen = false;
            clickDelayTicks = -1;
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        int slot = findGreenTerracotta(handler);
        if (slot == -1) {
            clickDelayTicks = -1;
            return;
        }

        if (clickDelayTicks == -1) {
            clickDelayTicks = rng.nextInt(MAX_DELAY_TICKS - MIN_DELAY_TICKS + 1) + MIN_DELAY_TICKS;
            Utils.debug("Loudmouth Bass click delay: " + clickDelayTicks + " ticks");
            return;
        }

        if (clickDelayTicks-- <= 0) {
            clickSlot(client, handler, slot, InventoryUtils.MouseButton.LEFT, ContainerInput.PICKUP);
            loudmouthBassScreen = false;
            clickDelayTicks = -1;
        }
    }

    private int findGreenTerracotta(AbstractContainerMenu handler) {
        int chestSize = Math.max(0, handler.slots.size() - 36);
        for (int i = 0; i < chestSize; i++) {
            Slot slot = handler.slots.get(i);
            if (slot.getItem().getItem().toString().equals("minecraft:green_terracotta")) {
                return i;
            }
        }

        return -1;
    }
}
