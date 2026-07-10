package rocket.giovanniclient.client.features;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import rocket.giovanniclient.client.features.autosolvers.AutoExperiments;
import rocket.giovanniclient.client.features.autosolvers.AutoFusion;
import rocket.giovanniclient.client.features.autosolvers.AutoMelody;
import rocket.giovanniclient.client.features.autosolvers.AutoShardsClaim;
import rocket.giovanniclient.client.features.misc.CloseChocolateEggs;
import rocket.giovanniclient.client.features.render.MineshaftArmorStandESP;
import rocket.giovanniclient.client.features.render.StarredMobESP;
import rocket.giovanniclient.client.features.rift.AutoAgaricusCap;
import rocket.giovanniclient.client.features.slayers.blaze.BlazeShieldHighlight;
import rocket.giovanniclient.client.features.slayers.enderman.AutoSoulcry;
import rocket.giovanniclient.client.util.PlayerLocator;
import rocket.giovanniclient.client.util.SlayerUtils;

import java.util.ArrayList;
import java.util.List;

public class FeatureManager {
    private static final List<AbstractFeature> FEATURES = new ArrayList<>();

    public static void register(AbstractFeature feature) {
        FEATURES.add(feature);
    }

    public static void registerAll() {
        register(new PlayerLocator());
        register(new SlayerUtils());

        register(new AutoMelody());
        register(new AutoShardsClaim());
        register(new AutoExperiments());
        register(new AutoFusion());
        register(new AutoSoulcry());
        register(new BlazeShieldHighlight());
        register(new CloseChocolateEggs());

        register(new AutoAgaricusCap());

        register(new StarredMobESP());
        register(new MineshaftArmorStandESP());

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
