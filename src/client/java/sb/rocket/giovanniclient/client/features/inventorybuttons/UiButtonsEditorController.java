package sb.rocket.giovanniclient.client.features.inventorybuttons;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;

public final class UiButtonsEditorController {

    private UiButtonsEditorController() {}

    private static boolean openPending = false;
    private static int reopenTicks = 0;

    private static void requestOpen(MinecraftClient client, Runnable feedback) {
        if (client == null || client.player == null) return;

        UiButtonsEditorState.setEditMode(true);
        openPending = true;
        reopenTicks = 1;

        if (feedback != null) feedback.run();
    }

    public static void requestOpenFromKeybind(MinecraftClient client) {
        requestOpen(client, () -> client.player.sendMessage(Text.literal("Inventory Buttons Editor: ON"), false));
    }

    public static void requestOpenFromCommand(FabricClientCommandSource source) {
        if (source == null) return;

        MinecraftClient client = source.getClient();
        if (client == null || client.player == null) {
            source.sendError(Text.literal("Player not available."));
            return;
        }

        requestOpen(client, () -> source.sendFeedback(Text.literal("Inventory Buttons Editor: ON")));
    }

    public static void tickAutoDisableIfNotInInventory(MinecraftClient client) {
        if (client == null) return;
        if (!UiButtonsEditorState.isEditMode()) return;
        if (openPending) return;

        if (!(client.currentScreen instanceof InventoryScreen)) {
            UiButtonsEditorState.setEditMode(false);
        }
    }

    public static void tickPendingOpenFlow(MinecraftClient client) {
        if (client == null || client.player == null) return;
        if (!openPending) return;

        if (!UiButtonsEditorState.isEditMode()) {
            openPending = false;
            return;
        }

        if (client.currentScreen instanceof ChatScreen) return;

        if (!(client.currentScreen instanceof InventoryScreen)) {
            client.execute(() -> client.setScreen(new InventoryScreen(client.player)));
            return;
        }

        if (reopenTicks-- > 0) {
            client.execute(() -> client.setScreen(new InventoryScreen(client.player)));
            return;
        }

        openPending = false;
    }
}
