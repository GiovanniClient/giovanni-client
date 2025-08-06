package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

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
    public static void clickSlot(MinecraftClient client, ScreenHandler handler, int slot,
                                 MouseButton button, SlotActionType actionType) {
        assert client.interactionManager != null;
        client.interactionManager.clickSlot(
                handler.syncId,
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
    public static List<Item> snapshotItems(ScreenHandler handler) {
        return handler.slots.stream()
                .map(Slot::getStack)
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
    public static int findItemByName(ScreenHandler handler, String name) {
        int chestSize = Math.max(0, handler.slots.size() - 36); // 36 = 27 inventory + 9 hotbar (player inventory)
        for (int i = 0; i < chestSize; i++) {
            if (handler.slots.get(i).getStack().getName().getString().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public static ItemStack getActiveItem() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return ItemStack.EMPTY;

        PlayerInventory playerInventory = player.getInventory();
        int selectedSlot = playerInventory.getSelectedSlot();
        return playerInventory.getStack(selectedSlot);
    }
}
