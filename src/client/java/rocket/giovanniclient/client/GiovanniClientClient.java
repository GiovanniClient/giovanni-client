package rocket.giovanniclient.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.wimods.freecam.WiFreecam;
import rocket.giovanniclient.client.bootstrap.*;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.updater.UpdateManagerV3;
import rocket.giovanniclient.client.mixin.GiovanniMixinPlugin;

import java.util.List;

public final class GiovanniClientClient implements ClientModInitializer {

    public static final Minecraft mc = Minecraft.getInstance();

    public static final String MOD_ID = "giovanniclient";
    public static final String MOD_VERSION = getModVersion();
    public static final String RELEASES_URL = "https://github.com/GiovanniClient/giovanni-client/releases/latest";
    private static boolean unsupportedVersionWarningShown;

    /**
     * ALSO UPDATE {@link GiovanniMixinPlugin}
     * can't call this list from there because it crashes in safemode
     */
    public static final List<String> SUPPORTED_VERSIONS = List.of("26.1.2");
    public static final UpdateManagerV3 UPDATE_MANAGER = new UpdateManagerV3(() -> ConfigManager.getConfig().about);

    @Override
    public void onInitializeClient() {
        // Gatekeeper Pattern
        // Allows the mod to run on any minecraft version, but loads the features only on supported ones
        // This way we can run the updater on versions the mod was not compiled for
        String currentMcVersion = getMcVersion();

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
        ClientCustomCommands.register();
        ClientResources.register();
        ClientWorldJoinEvents.register();
        ClientLifecycle.registerClientStarted();

        WiFreecam.INSTANCE.initialize();
    }

    private void runSafeModeInit(String version) {
        System.out.println("[Giovanni] Running on untested version: " + version);
        System.out.println("[Giovanni] Functional features disabled for safety (avoid crash).");

        ClientLifecycle.registerShutdownHook();

        // inform the user they are running on unsupported minecraft version - pops up when they join a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (unsupportedVersionWarningShown) return;
            unsupportedVersionWarningShown = true;

            MutableComponent warningMessage = Component.literal("\n\n\n===== ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal("GIOVANNI CLIENT").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                    .append(Component.literal(" =====\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                    .append(Component.literal("\nUNSUPPORTED MC VERSION: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)).append(Component.literal(getMcVersion()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("\nFeatures are disabled to prevent crashing.\n").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("\n[CLICK TO CHECK FOR UPDATES]").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD, ChatFormatting.UNDERLINE).withStyle(s -> s.withClickEvent(new ClickEvent.SuggestCommand("/giovanni-check-update"))))
                    .append(Component.literal("\n\n=========================").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

            assert client.player != null;
            client.gui.getChat().addClientSystemMessage(warningMessage);
        });

        ClientCustomCommands.registerSafemode();
        ClientLifecycle.registerClientStarted();
    }

    public static String getMcVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString();
    }

    public static String getModVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID).map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse("0.1");
    }

    public static boolean isCurrentVersionSupported() {
        return SUPPORTED_VERSIONS.contains(getMcVersion());
    }
}
