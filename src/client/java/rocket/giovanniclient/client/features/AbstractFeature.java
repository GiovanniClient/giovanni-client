package rocket.giovanniclient.client.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public abstract class AbstractFeature {

    /**
     * Called when any screen is opened.
     * Features that need to react to screen openings should override this method.
     * Default implementation does nothing.
     *
     * @param screen The screen that was opened.
     */
    public void onScreenOpen(Screen screen) {
        // Default empty implementation
    }

    /**
     * Called at the end of each client tick.
     * Features that need continuous updates should override this method.
     * Default implementation does nothing.
     *
     * @param client The Minecraft instance.
     */
    public void onTick(Minecraft client) {
        // Default empty implementation
    }

    /**
     * Called when the client successfully joins a new world/server.
     * This method is named onWorldLoad based on your error message.
     * Features that need to perform actions on world join should override this method.
     * Default implementation does nothing.
     *
     * @param client The Minecraft instance.
     */
    public void onWorldLoad(Minecraft client) { // Changed method name to match error message
        // Default empty implementation
    }
}