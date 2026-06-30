package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.InventoryUtils;

import java.util.ArrayList;
import java.util.List;

import static rocket.giovanniclient.client.util.InventoryUtils.clickSlot;

public class AutoMelody extends AbstractFeature {
    private boolean inHarp = false;
    private final List<Item> lastInventory = new ArrayList<>();
    private int counter = 0;

    @Override
    public void onScreenOpen(Screen screen) {
        if (screen instanceof ContainerScreen gui &&
                gui.getTitle().getString().startsWith("Harp -")) {
            lastInventory.clear();
            inHarp = true;
        }

    }

    @Override
    public void onTick(Minecraft client) {
        if (!ConfigManager.getConfig().asc.AUTOMELODY_TOGGLE || client.player == null || ++counter % 2 == 0)
            return;

        if (!inHarp || !(client.screen instanceof ContainerScreen gui) ||
                !gui.getTitle().getString().startsWith("Harp -")) {
            inHarp = false;
            return;
        }

        AbstractContainerMenu handler = client.player.containerMenu;
        List<Item> currentInventory = InventoryUtils.snapshotItems(handler);

        if (!lastInventory.equals(currentInventory)) {
            for (int i = 0; i < handler.slots.size(); i++) {
                if (handler.slots.get(i).getItem().is(Items.QUARTZ_BLOCK)) {
                    clickSlot(client, handler, i, InventoryUtils.MouseButton.MIDDLE, ClickType.CLONE);
                    break;
                }
            }
        }

        lastInventory.clear();
        lastInventory.addAll(currentInventory);
    }
}