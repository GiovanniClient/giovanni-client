package rocket.giovanniclient.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class InventoryUtils {

    public enum MouseButton {
        LEFT(0),
        RIGHT(1),
        MIDDLE(2);

        private final int value;

        MouseButton(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * Clicks a slot in the inventory GUI.
     *
     * @param client    The Minecraft client instance.
     * @param handler   The screen handler (inventory container).
     * @param slot      The slot index to click.
     * @param button    The mouse button to use for the click.
     * @param actionType The type of click action.
     */
    public static void clickSlot(Minecraft client, AbstractContainerMenu handler, int slot,
                                 MouseButton button, ClickType actionType) {
        assert client.gameMode != null;
        client.gameMode.handleInventoryMouseClick(
                handler.containerId,
                slot,
                button.getValue(),
                actionType,
                client.player
        );
    }

    /**
     * Takes a "snapshot" of all non-empty item stacks in a container (e.g., a chest or crafting table)
     * and returns a list of their item types (not the full ItemStacks, just the Item).
     *
     * @param handler The container handler (like a GUI inventory).
     * @return A list of Items currently in the container (ignoring empty slots).
     */
    public static List<Item> snapshotItems(AbstractContainerMenu handler) {
        return handler.slots.stream()
                .map(Slot::getItem)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::getItem)
                .toList();
    }

    /**
     * Searches the container slots (excluding player inventory) for an item with a matching display name.
     *
     * @param handler The inventory or GUI container.
     * @param name    The display name to search for.
     * @return The index of the first matching slot, or -1 if not found.
     */
    public static int findItemByName(AbstractContainerMenu handler, String name) {
        int chestSize = Math.max(0, handler.slots.size() - 36); // 36 = 27 inventory + 9 hotbar (player inventory)
        for (int i = 0; i < chestSize; i++) {
            if (handler.slots.get(i).getItem().getItemName().getString().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack getHeldItem() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        Inventory playerInventory = player.getInventory();
        return playerInventory.getItem(playerInventory.getSelectedSlot());
    }
}
