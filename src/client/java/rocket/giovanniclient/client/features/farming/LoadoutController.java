package rocket.giovanniclient.client.features.farming;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.client.util.InventoryUtils;

import java.util.regex.Pattern;

/** Owns the requested Loadouts menu and waits for chat confirmation before finishing a swap. */
final class LoadoutController {
    private static final Pattern TITLE = Pattern.compile("^(?:\\(1/\\d+\\)\\s*)?Loadouts$");
    private final Selection selection = new Selection();
    private AbstractContainerMenu menu;

    void requested() {
        reset();
        selection.requested();
    }

    PestCycle.MenuState menuState(int button) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == null) return PestCycle.MenuState.WAITING;
        if (!selection.awaitingMenu() || !(client.gui.screen() instanceof ContainerScreen screen)
                || !TITLE.matcher(PestTabParser.clean(screen.getTitle().getString())).matches()
                || client.player.containerMenu != screen.getMenu()) {
            return PestCycle.MenuState.WRONG_MENU;
        }
        boolean refreshed = menu != null && menu != screen.getMenu();
        menu = screen.getMenu();
        // Six chest rows followed by the player's 36 inventory slots.
        if (menu.slots.size() != 90) return PestCycle.MenuState.WRONG_MENU;
        if (refreshed) return PestCycle.MenuState.WAITING;
        ItemStack stack = menu.slots.get(PestTabParser.containerSlot(button)).getItem();
        if (stack.isEmpty()) return PestCycle.MenuState.WAITING;
        if (stack.is(Items.DYE.pick(DyeColor.GRAY)) || stack.is(Items.BARRIER)
                || stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY))
                || stack.is(Items.STAINED_GLASS_PANE.pick(DyeColor.BLACK))) {
            return PestCycle.MenuState.INVALID_BUTTON;
        }
        return PestCycle.MenuState.READY;
    }

    void click(int button) {
        int slot = PestTabParser.containerSlot(button);
        String name = PestTabParser.clean(menu.slots.get(slot).getItem().getHoverName().getString());
        selection.clicked(name);
        InventoryUtils.clickSlot(Minecraft.getInstance(), menu, slot,
                InventoryUtils.MouseButton.LEFT, ContainerInput.PICKUP);
    }

    void onMessage(String text) {
        selection.onMessage(text);
    }

    PestCycle.CloseState close() {
        // An unchanged or closed GUI alone does not prove that the server accepted the swap.
        if (!selection.confirmed()) return PestCycle.CloseState.WAITING;
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() == null && client.player.containerMenu == client.player.inventoryMenu) {
            reset();
            return PestCycle.CloseState.CLOSED;
        }
        // Server-side closing and reopening can arrive as separate updates.
        if (client.gui.screen() == null) return PestCycle.CloseState.WAITING;
        if (!(client.gui.screen() instanceof ContainerScreen screen)) return PestCycle.CloseState.WRONG_MENU;
        if (client.player.containerMenu != screen.getMenu()) return PestCycle.CloseState.WAITING;
        // The matching chat confirmation also permits the server's follow-up container.
        client.player.closeContainer();
        reset();
        return PestCycle.CloseState.CLOSED;
    }

    void reset() {
        menu = null;
        selection.reset();
    }

    /** Tracks the requested loadout through its server chat confirmation. */
    static final class Selection {
        private enum Phase { IDLE, OPENING, WAITING_CONFIRMATION, CONFIRMED }

        private Phase phase = Phase.IDLE;
        private String expectedMessage;

        void requested() {
            expectedMessage = null;
            phase = Phase.OPENING;
        }

        boolean awaitingMenu() {
            return phase == Phase.OPENING;
        }

        void clicked(String name) {
            if (!awaitingMenu()) throw new IllegalStateException("No loadout selection is pending");
            expectedMessage = "You equipped " + name + "!";
            phase = Phase.WAITING_CONFIRMATION;
        }

        void onMessage(String text) {
            if (phase == Phase.WAITING_CONFIRMATION && text.contains(expectedMessage)) phase = Phase.CONFIRMED;
        }

        boolean confirmed() {
            return phase == Phase.CONFIRMED;
        }

        void reset() {
            expectedMessage = null;
            phase = Phase.IDLE;
        }
    }
}
