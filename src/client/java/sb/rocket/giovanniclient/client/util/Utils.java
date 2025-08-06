package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sb.rocket.giovanniclient.client.config.DebugConfig;

public class Utils {
    public static final Logger LOGGER = LoggerFactory.getLogger("GiovanniClient");
    private static DebugConfig debugConfig;

    public static void init(DebugConfig debugConfig) {
        if (Utils.debugConfig != null) {
            LOGGER.warn("Utils.init() called multiple times!");
        }
        Utils.debugConfig = debugConfig;
        LOGGER.info("Utils initialized with DebugConfig.");
    }

    private static void sendFormattedChatMessage(String prefixText, Formatting prefixStyle, String messageText) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.inGameHud != null && client.inGameHud.getChatHud() != null) {
            MutableText prefix = Text.literal(prefixText)
                    .setStyle(Style.EMPTY.withColor(prefixStyle));

            MutableText message = Text.literal(messageText)
                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE));

            client.inGameHud.getChatHud().addMessage(prefix.append(message));
        }
    }

    public static void chat(String message) {
        sendFormattedChatMessage("Giovanni > ", Formatting.LIGHT_PURPLE, message);
    }

    public static void debug(String message) {
        LOGGER.debug("DEBUG (Giovanni): {}", message);
        if (debugConfig != null && debugConfig.DEBUG) {
            sendFormattedChatMessage("Giovanni > ", Formatting.BLUE, message);
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