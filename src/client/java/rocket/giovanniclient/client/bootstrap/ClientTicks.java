package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.wimods.freecam.WiFreecam;
import rocket.giovanniclient.client.config.ConfigManager;

import static rocket.giovanniclient.client.bootstrap.ClientKeybinds.switchFreecamControlKey;
import static rocket.giovanniclient.client.bootstrap.ClientKeybinds.toggleFreecam;

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
            // 1. Toggle Freecam
            // Make sure 'toggleFreecam' is defined in ClientKeybinds
            while (toggleFreecam.wasPressed()) {
                WiFreecam.INSTANCE.setEnabled(!WiFreecam.INSTANCE.isEnabled());
            }

            // 3. Cycle Control Mode (Camera vs Player)
            while (switchFreecamControlKey.wasPressed()) {
                ConfigManager.getConfig().freecamConfig.cycleInputMode();
            }
        });
    }
}
