package sb.rocket.giovanniclient.client.features.updater;

import net.minecraft.client.MinecraftClient;
import sb.rocket.giovanniclient.client.GiovanniClientClient;
import sb.rocket.giovanniclient.client.features.AbstractFeature;

import java.util.Random;

public class StartupMessageFeature extends AbstractFeature {
    private boolean messageSent = false;
    private final Random random = new Random();

    @Override
    public void onWorldLoad(MinecraftClient client) {
        if (GiovanniClientClient.UPDATE_MANAGER.isUpdateScheduled()) return;

        if (!messageSent) {
            GiovanniClientClient.UPDATE_MANAGER.sendUpdateFoundMessage();
            messageSent = true;
        } else if (random.nextInt(5) == 1) {
            GiovanniClientClient.UPDATE_MANAGER.sendUpdateFoundMessage();
        }
    }
}