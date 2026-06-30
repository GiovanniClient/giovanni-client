package rocket.giovanniclient.client.features.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Unique;
import rocket.giovanniclient.client.config.ConfigManager;
import rocket.giovanniclient.client.features.AbstractFeature;

public class CloseChocolateEggs extends AbstractFeature {
    @Unique
    private final MiscConfig msc = ConfigManager.getConfig().msc;
    private final String[] eggNames = {"Breakfast Egg", "Brunch Egg", "Lunch Egg", "Déjeuner Egg", "Dinner Egg", "Supper Egg"};

    @Override
    public void onScreenOpen(Screen screen) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (msc.CLOSE_EGGS && player != null && screen instanceof ContainerScreen) {
            String title = screen.getTitle().getString();

            for (String egg : eggNames)
                if (title.contains(egg)) screen.onClose();
        }
    }
}
