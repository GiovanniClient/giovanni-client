package sb.rocket.giovanniclient.client.features.fun;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Unique;
import sb.rocket.giovanniclient.client.config.ConfigManager;
import sb.rocket.giovanniclient.client.config.MiscConfig;
import sb.rocket.giovanniclient.client.features.AbstractFeature;

public class CloseChocolateEggs extends AbstractFeature {
    @Unique
    private final MiscConfig msc = ConfigManager.getConfig().msc;
    private final String[] eggNames = {"Breakfast Egg", "Brunch Egg", "Lunch Egg", "Déjeuner Egg", "Dinner Egg", "Supper Egg"};

    @Override
    public void onScreenOpen(Screen screen) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (msc.CLOSE_EGGS && player != null && screen instanceof GenericContainerScreen) {
            String title = screen.getTitle().getString();

            for (String egg : eggNames)
                if (title.contains(egg)) screen.close();
        }
    }
}
