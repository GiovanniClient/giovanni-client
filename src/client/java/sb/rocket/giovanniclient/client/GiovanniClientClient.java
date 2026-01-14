package sb.rocket.giovanniclient.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import sb.rocket.giovanniclient.client.bootstrap.*;
import sb.rocket.giovanniclient.client.features.updater.UpdateManager;

public final class GiovanniClientClient implements ClientModInitializer {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final String MODID = "giovanniclient";
    public static final String MOD_VERSION_NAME = "1.0 (beta)";
    public static final int MOD_VERSION_CODE = 10001;

    public static final UpdateManager UPDATE_MANAGER = new UpdateManager();

    @Override
    public void onInitializeClient() {
        ClientLifecycle.registerShutdownHook();
        ClientKeybinds.register();
        ClientTicks.register();
        ClientCommands.register();
        ClientResources.register();
        ClientLifecycle.registerClientStarted();
    }
}
