package rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Util;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.util.Utils;

public final class ClientWorldJoinEvents {
    private static final long UPDATE_NOTIFICATION_DELAY_MILLIS = 60_000;
    private static final long DEBUG_UPDATE_NOTIFICATION_DELAY_MILLIS = 4_000;
    private static boolean startupUpdateCheckDone;
    private static LocalPlayer loadedPlayer;
    private static long worldEnteredAt;
    private static Runnable pendingUpdateNotification;

    private ClientWorldJoinEvents() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Utils.debug("Player joined world.");

            if (startupUpdateCheckDone) {
                Utils.log("Startup update check skipped: already checked this session.");
                return;
            }

            startupUpdateCheckDone = true;
            updateCheckOnStartup(client);
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientWorldJoinEvents::tickUpdateNotification);
    }

    private static void tickUpdateNotification(Minecraft client) {
        var connection = client.getConnection();
        LocalPlayer player = client.level != null && connection != null && connection.hasClientLoaded()
                ? client.player : null;

        // Respawn replaces the player even when the warp keeps the same dimension.
        if (player != loadedPlayer) {
            loadedPlayer = player;
            worldEnteredAt = Util.getMillis();
        }

        if (pendingUpdateNotification == null || player == null) {
            return;
        }

        var debug = ConfigManager.getConfig().debugConfig;
        long delay = debug.DEBUG || debug.USE_TEST_UPDATE_REPOSITORY
                ? DEBUG_UPDATE_NOTIFICATION_DELAY_MILLIS : UPDATE_NOTIFICATION_DELAY_MILLIS;
        if (Util.getMillis() - worldEnteredAt < delay) {
            return;
        }

        Runnable notification = pendingUpdateNotification;
        pendingUpdateNotification = null;
        notification.run();
    }

    private static void updateCheckOnStartup(Minecraft client) {
        if (!GiovanniClientClient.isCurrentVersionSupported()) {
            Utils.log("Update check skipped: minecraft version unsupported, message sent.");
            return;
        }

        if (!ConfigManager.getConfig().about.AUTO_CHECK_FOR_UPDATES) {
            Utils.log("Update check skipped: Auto-check disabled in config.");
            return;
        }

        GiovanniClientClient.UPDATE_MANAGER.runUpdateFlow(notification ->
                client.execute(() -> pendingUpdateNotification = notification));
    }
}
