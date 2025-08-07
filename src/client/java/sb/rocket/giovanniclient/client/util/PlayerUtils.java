package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.Hand;

public class PlayerUtils {
    public static void simulateClick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerInteractionManager im = client.interactionManager;

        if (im != null) {
            im.interactItem(
                client.player,
                Hand.MAIN_HAND
            );
        }
    }
}
