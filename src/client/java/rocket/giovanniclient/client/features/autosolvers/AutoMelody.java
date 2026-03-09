package rocket.giovanniclient.client.features.autosolvers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
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
        if (screen instanceof GenericContainerScreen gui &&
                gui.getTitle().getString().startsWith("Harp -")) {
            lastInventory.clear();
            inHarp = true;
        }

    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!ConfigManager.getConfig().asc.AUTOMELODY_TOGGLE || client.player == null || ++counter % 2 == 0)
            return;

        if (!inHarp || !(client.currentScreen instanceof GenericContainerScreen gui) ||
                !gui.getTitle().getString().startsWith("Harp -")) {
            inHarp = false;
            return;
        }

        ScreenHandler handler = client.player.currentScreenHandler;
        List<Item> currentInventory = InventoryUtils.snapshotItems(handler);

        if (!lastInventory.equals(currentInventory)) {
            for (int i = 0; i < handler.slots.size(); i++) {
                if (handler.slots.get(i).getStack().isOf(Items.QUARTZ_BLOCK)) {
                    clickSlot(client, handler, i, InventoryUtils.MouseButton.MIDDLE, SlotActionType.CLONE);
                    break;
                }
            }
        }

        lastInventory.clear();
        lastInventory.addAll(currentInventory);
    }
}