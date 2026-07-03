package rocket.giovanniclient.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.InteractionHand;

public class PlayerUtils {
    public static void simulateClick() {
        Minecraft client = Minecraft.getInstance();
        MultiPlayerGameMode im = client.gameMode;

        if (im != null) {
            assert client.player != null;
            im.useItem(
                client.player,
                    InteractionHand.MAIN_HAND
            );
        }
    }
}
