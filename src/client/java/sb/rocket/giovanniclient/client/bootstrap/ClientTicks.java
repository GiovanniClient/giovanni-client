package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.wimods.freecam.WiFreecam;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.features.inventorybuttons.UiButtonsEditorController;

import static sb.rocket.giovanniclient.client.bootstrap.ClientKeybinds.*;

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
                while (kb.wasPressed()) UiButtonsEditorController.requestOpenFromKeybind(client);
            }
        });

        // Edit-mode auto close policy
        ClientTickEvents.END_CLIENT_TICK.register(UiButtonsEditorController::tickAutoDisableIfNotInInventory);

        // Pending flow that safely forces InventoryScreen init/reopen while editMode=true
        ClientTickEvents.END_CLIENT_TICK.register(UiButtonsEditorController::tickPendingOpenFlow);

        // Inside ClientTicks.java -> register() method

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 1. Toggle Freecam
            // Make sure 'toggleFreecam' is defined in ClientKeybinds
            while (toggleFreecam.wasPressed()) {
                WiFreecam.INSTANCE.setEnabled(!WiFreecam.INSTANCE.isEnabled());
            }

            // 2. Open Settings
            while (openFreecamSettings.wasPressed()) {
                // You'll need to implement a way to open the config screen
                // Usually, you can call ConfigManager or a specific GUI class
                client.execute(ConfigManager::openConfigScreen);
            }

            // 3. Cycle Control Mode (Camera vs Player)
            while (switchFreecamControlKey.wasPressed()) {
                ConfigManager.getConfig().freecamConfig.cycleInputMode();
            }
        });
    }
}
