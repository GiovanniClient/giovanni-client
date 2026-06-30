package rocket.giovanniclient.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rocket.giovanniclient.client.config.DebugConfig;

public class Utils {
    public static final String rocketEmoji = "\uD83D\uDE80";
    public static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient");
    private static DebugConfig debugConfig;

    public static void init(DebugConfig debugConfig) {
        if (Utils.debugConfig != null) {
            LOGGER.warn("Utils.init() called multiple times!");
        }
        Utils.debugConfig = debugConfig;
        LOGGER.info("Utils initialized with DebugConfig.");
    }

    private static void sendFormattedChatMessage(String prefixComponent, ChatFormatting prefixStyle, String messageComponent) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.gui != null && client.gui.getChat() != null) {
            MutableComponent prefix = Component.literal(prefixComponent)
                    .setStyle(Style.EMPTY.withColor(prefixStyle));

            MutableComponent message = Component.literal(messageComponent)
                    .setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE));

            client.gui.getChat().addMessage(prefix.append(message));
        }
    }

    public static void MutableComponentToChat(MutableComponent Component) {
        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.displayClientMessage(Component, false);
    }

    public static void chat(String message) {
        sendFormattedChatMessage("Giovanni > ", ChatFormatting.LIGHT_PURPLE, message);
    }

    public static void debug(String message) {
        LOGGER.debug("DEBUG (Giovanni): {}", message);
        if (debugConfig != null && debugConfig.DEBUG) {
            sendFormattedChatMessage("DEBUG (Giovanni): ", ChatFormatting.RED, message);
        } else if (debugConfig == null) {
            LOGGER.warn("Utils.debug() called before initialization of DebugConfig: {}", message);
        }
    }


    public static void log(String message) {
        LOGGER.info("{}", message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}