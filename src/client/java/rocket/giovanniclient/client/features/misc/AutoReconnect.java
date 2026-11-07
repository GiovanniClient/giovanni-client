package rocket.giovanniclient.client.features.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;
import rocket.giovanniclient.client.config.ConfigManager;

public final class AutoReconnect {
    private static final long DELAY_MILLIS = 5_000;
    private static final Component BACK_TO_SERVER_LIST = Component.translatable("gui.toMenu");
    private static @Nullable Attempt pending;

    private AutoReconnect() {}

    public static void schedule(DisconnectedScreen screen, @Nullable Screen parent, ServerData server) {
        if (ConfigManager.getConfig().miscConfig.AUTO_RECONNECT) {
            clearPending();
            pending = new Attempt(screen, parent, server, Util.getMillis() + DELAY_MILLIS);
        }
    }

    public static void tick(Minecraft client) {
        if (pending == null) return;
        if (!ConfigManager.getConfig().miscConfig.AUTO_RECONNECT || !client.allowsMultiplayer()) {
            clearPending();
            return;
        }

        Attempt attempt = pending;
        Screen screen = client.gui.screen();
        if (screen != attempt.screen()) {
            clearPending();
            if (attempt.screen() instanceof ConnectScreen && screen instanceof DisconnectedScreen disconnected) {
                schedule(disconnected, attempt.parent(), attempt.server());
            }
            return;
        }
        if (!(screen instanceof DisconnectedScreen disconnected)) return;

        long remainingMillis = attempt.retryAt() - Util.getMillis();
        if (remainingMillis > 0) {
            setBackButtonCountdown(disconnected, Math.ceilDiv(remainingMillis, 1_000));
            return;
        }

        Screen parent = attempt.parent();
        if (parent == null) parent = new JoinMultiplayerScreen(new TitleScreen());

        clearPending();
        ServerData server = attempt.server();
        ConnectScreen.startConnecting(parent, client, ServerAddress.parseString(server.ip), server, false, null);
        if (client.gui.screen() instanceof ConnectScreen connecting) {
            pending = new Attempt(connecting, parent, server, 0);
        }
    }

    private static void clearPending() {
        if (pending != null && pending.screen() instanceof DisconnectedScreen disconnected) {
            setBackButtonCountdown(disconnected, 0);
        }
        pending = null;
    }

    private static void setBackButtonCountdown(DisconnectedScreen screen, long seconds) {
        for (var child : screen.children()) {
            if (child instanceof Button button && button.getMessage().getContents().equals(BACK_TO_SERVER_LIST.getContents())) {
                Component label = seconds > 0 ? BACK_TO_SERVER_LIST.copy().append(" (" + seconds + "s)") : BACK_TO_SERVER_LIST;
                if (!label.equals(button.getMessage())) button.setMessage(label);
                return;
            }
        }
    }

    private record Attempt(Screen screen, @Nullable Screen parent, ServerData server, long retryAt) {}
}
