package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;

public final class InventoryButtonEditorFlow {

    private InventoryButtonEditorFlow() {}

    private static boolean openPending = false;
    private static int reopenTicks = 0;

    public static void requestOpenFromKeybind(MinecraftClient client) {
        if (client == null || client.player == null) return;

        UiButtonsConfigManager.setEditMode(true);
        openPending = true;
        reopenTicks = 1;

        client.player.sendMessage(Text.literal("Inventory Buttons Editor: ON"), false);
    }

    public static void requestOpenFromCommand(FabricClientCommandSource source) {
        if (source == null) return;

        MinecraftClient client = source.getClient();
        if (client == null || client.player == null) {
            source.sendError(Text.literal("Player not available."));
            return;
        }

        UiButtonsConfigManager.setEditMode(true);
        openPending = true;
        reopenTicks = 1;

        source.sendFeedback(Text.literal("Inventory Buttons Editor: ON"));
    }

    public static void tickAutoDisableIfNotInInventory(MinecraftClient client) {
        if (client == null) return;
        if (!UiButtonsConfigManager.isEditMode()) return;
        if (openPending) return;

        if (!(client.currentScreen instanceof InventoryScreen)) {
            UiButtonsConfigManager.setEditMode(false);
        }
    }

    public static void tickPendingOpenFlow(MinecraftClient client) {
        if (client == null) return;
        if (!openPending) return;
        if (client.player == null) return;

        if (!UiButtonsConfigManager.isEditMode()) {
            openPending = false;
            return;
        }

        if (client.currentScreen instanceof ChatScreen) return;

        if (!(client.currentScreen instanceof InventoryScreen)) {
            client.execute(() -> client.setScreen(new InventoryScreen(client.player)));
            return;
        }

        if (reopenTicks > 0) {
            reopenTicks--;
            client.execute(() -> client.setScreen(new InventoryScreen(client.player)));
            return;
        }

        openPending = false;
    }
}
