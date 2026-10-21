package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.misc.GuiScaleCycler;

public final class ClientTicks {

    private ClientTicks() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Keybind: open config
            var kb = ClientKeybinds.openConfig();
            if (kb != null) {
                while (kb.consumeClick()) client.execute(ConfigManager::openConfigScreen);
            }

            var scaleKey = ClientKeybinds.cycleGuiScale();
            if (scaleKey != null) {
                while (scaleKey.consumeClick()) {
                    if (client.player != null && client.gui.screen() == null) GuiScaleCycler.cycle(client);
                }
            }

            // Flag set by command
            if (ConfigManager.shouldOpenFromCommand) {
                ConfigManager.shouldOpenFromCommand = false;
                client.execute(ConfigManager::openConfigScreen);
            }
        });
    }
}
