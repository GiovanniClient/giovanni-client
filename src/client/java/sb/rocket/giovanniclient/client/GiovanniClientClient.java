package sb.rocket.giovanniclient.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.wimods.freecam.WiFreecam;
import sb.rocket.giovanniclient.client.bootstrap.*;
import sb.rocket.giovanniclient.client.features.updater.UpdateManagerV2;

import java.util.List;

public final class GiovanniClientClient implements ClientModInitializer {

    public static final MinecraftClient mc = MinecraftClient.getInstance();

    public static final String MODID = "giovanniclient";
    public static final String MOD_VERSION_NAME = "V1.1 for mc1.21.10";
    public static final int MOD_VERSION_CODE = 10011;

    public static final List<String> SUPPORTED_VERSIONS = List.of(
            // ALSO UPDATE GiovanniMixinPlugin.java!
            // can't call this list from there because it crashes in safemode
            "1.21.10"
    );
    public static final UpdateManagerV2 UPDATE_MANAGER = new UpdateManagerV2();

    @Override
    public void onInitializeClient() {
        // Gatekeeper Pattern
        // Allows the mod to run on any minecraft version, but loads the features only on supported ones
        // This way we can run the updater on versions the mod was not compiled for
        String currentMcVersion = getCurrentMcVersion();
        UPDATE_MANAGER.prepare();

        if (isCurrentVersionSupported()) {
            runFullInit();
        } else {
            // If the version we're currently running is not supported, DO NOT LOAD ANYTHING but the updater
            runSafeModeInit(currentMcVersion);
        }
    }

    private void runFullInit() {
        ClientLifecycle.registerShutdownHook();
        ClientKeybinds.register();
        ClientTicks.register();
        ClientCommands.register();
        ClientResources.register();
        ClientLifecycle.registerClientStarted();

        WiFreecam.INSTANCE.initialize();
    }

    private void runSafeModeInit(String version) {
        System.out.println("[Giovanni] Running on untested version: " + version);
        System.out.println("[Giovanni] Functional features disabled for safety (avoid crash).");

        ClientLifecycle.registerShutdownHook();
        SafemodeUpdaterBootstrap.register();
        ClientCommands.registerSafemode();
        ClientLifecycle.registerClientStarted();
    }

    public static String getCurrentMcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
    }

    public static boolean isCurrentVersionSupported() {
        String currentMcVersion = getCurrentMcVersion();
        return SUPPORTED_VERSIONS.contains(currentMcVersion);
    }


}
