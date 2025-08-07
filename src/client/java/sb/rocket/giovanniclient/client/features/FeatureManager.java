package sb.rocket.giovanniclient.client.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import sb.rocket.giovanniclient.client.features.autosolvers.AutoExperiments;
import sb.rocket.giovanniclient.client.features.autosolvers.AutoFusion;
import sb.rocket.giovanniclient.client.features.autosolvers.AutoMelody;
import sb.rocket.giovanniclient.client.features.autosolvers.AutoShardsClaim;
import sb.rocket.giovanniclient.client.features.fun.CloseChocolateEggs;
import sb.rocket.giovanniclient.client.features.slayers.enderman.AutoSoulcry;
import sb.rocket.giovanniclient.client.features.updater.StartupMessageFeature;
import sb.rocket.giovanniclient.client.util.PlayerLocator;
import sb.rocket.giovanniclient.client.util.SlayerUtils;

import java.util.ArrayList;
import java.util.List;

public class FeatureManager {
    private static final List<AbstractFeature> FEATURES = new ArrayList<>();

    public static void register(AbstractFeature feature) {
        FEATURES.add(feature);
    }

    public static void registerAll() {
        register(new StartupMessageFeature());

        register(new PlayerLocator());
        register(new SlayerUtils());

        register(new AutoMelody());
        register(new AutoShardsClaim());
        register(new AutoExperiments());
        register(new AutoFusion());
        register(new AutoSoulcry());
        register(new CloseChocolateEggs());

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            for (AbstractFeature f : FEATURES)
                f.onScreenOpen(screen);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (AbstractFeature f : FEATURES)
                f.onTick(client);
        });

        // Register the new event for when the player joins a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            for (AbstractFeature f : FEATURES)
                f.onWorldLoad(client);
        });
    }
}
