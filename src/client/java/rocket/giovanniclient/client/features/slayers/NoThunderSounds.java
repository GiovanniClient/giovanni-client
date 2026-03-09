package rocket.giovanniclient.client.features.slayers;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Unique;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;
import rocket.giovanniclient.client.util.PlayerLocator;

public class NoThunderSounds extends AbstractFeature {
    @Unique
    private final SlayersConfig sc = ConfigManager.getConfig().sc;

    @Override
    public void onTick(MinecraftClient client) {
        if (sc.NO_THUNDER_SOUNDS) {
            if (PlayerLocator.isPlayerIn("The Wasteland")) {

            }
        }

    }
}
