package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.InventoryButtonEditorFlow;

public final class ClientTicks {

    private ClientTicks() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Keybind: open config
            var kb = ClientKeybinds.openConfig();
            if (kb != null) {
                while (kb.wasPressed()) client.execute(ConfigManager::openConfigScreen);
            }

            // Flag set by command
            if (ConfigManager.shouldOpenFromCommand) {
                ConfigManager.shouldOpenFromCommand = false;
                client.execute(ConfigManager::openConfigScreen);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Keybind: open inventory button editor
            var kb = ClientKeybinds.openInvButtonEditor();
            if (kb != null) {
                while (kb.wasPressed()) InventoryButtonEditorFlow.requestOpenFromKeybind(client);
            }
        });

        // Edit-mode auto close policy
        ClientTickEvents.END_CLIENT_TICK.register(InventoryButtonEditorFlow::tickAutoDisableIfNotInInventory);

        // Pending flow that safely forces InventoryScreen init/reopen while editMode=true
        ClientTickEvents.END_CLIENT_TICK.register(InventoryButtonEditorFlow::tickPendingOpenFlow);
    }
}
