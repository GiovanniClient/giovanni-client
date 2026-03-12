package rocket.giovanniclient.client.features.old_updater;

import net.minecraft.client.MinecraftClient;
import rocket.giovanniclient.client.GiovanniClientClient;
import rocket.giovanniclient.client.features.AbstractFeature;

import java.util.Random;

public class StartupMessageFeature extends AbstractFeature {
    private boolean firstMessageSent = false;
    private final Random random = new Random();

    @Override
    public void onWorldLoad(MinecraftClient client) {
        if (GiovanniClientClient.UPDATE_MANAGER.isUpdateScheduled()) return;

        if (!firstMessageSent) {
            GiovanniClientClient.UPDATE_MANAGER.sendUpdateFoundMessage();
            firstMessageSent = true;
        } else if (random.nextInt(5) == 1) {
            GiovanniClientClient.UPDATE_MANAGER.sendUpdateFoundMessage();
        }
    }
}