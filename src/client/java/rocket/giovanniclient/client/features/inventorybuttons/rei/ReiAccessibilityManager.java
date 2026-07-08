package rocket.giovanniclient.client.features.inventorybuttons.rei;

import me.shedaniel.rei.api.client.config.ConfigManager;
import me.shedaniel.rei.api.client.config.ConfigObject;
import rocket.giovanniclient.client.util.Utils;

import java.lang.reflect.Field;

public final class ReiAccessibilityManager {
    private static boolean disableAttempted;

    private ReiAccessibilityManager() {}

    /**
     * REI has this feature that enables clicking the arrow in a crafting GUI, doing so
     * displays all recipes craftable in the current open inventory
     * <p>
     * This feature is ON by default, and it also has a tooltip when hovering the arrow with the mouse.
     * This is kind of annoying and an issue for inventory buttons, so I would like to disable it... but there is no setter
     * for this setting in REI's config. So here's some unorthodox code to disable this feature
     */
    public static void disableClickableRecipeArrowsIfNeeded() {
        if (disableAttempted) {
            return;
        }

        disableAttempted = true;

        if (!areClickableRecipeArrowsEnabled()) {
            Utils.debug("REI Clickable Recipe Arrows is already disabled.");
            return;
        }

        if (disableClickableRecipeArrows()) {
            Utils.debug("[REI] Clickable Recipe Arrows disabled successfully.");
        } else {
            Utils.debug("[REI] Failed to disable Clickable Recipe Arrows.");
        }
    }

    private static boolean disableClickableRecipeArrows() {
        try {
            ConfigObject config = ConfigObject.getInstance();

            Field advancedField = config.getClass().getDeclaredField("advanced");
            advancedField.setAccessible(true);
            Object advanced = advancedField.get(config);

            Field miscField = advanced.getClass().getDeclaredField("miscellaneous");
            miscField.setAccessible(true);
            Object miscellaneous = miscField.get(advanced);

            Field arrowsField = miscellaneous.getClass().getDeclaredField("clickableRecipeArrows");
            arrowsField.setAccessible(true);
            arrowsField.set(miscellaneous, false);

            ConfigManager.getInstance().saveConfig();
            return true;
        } catch (Exception e) {
            Utils.error("ReiAccessibilityManager crashed while disabling clickable recipe arrows.", e);
            return false;
        }
    }

    public static boolean areClickableRecipeArrowsEnabled() {
        return ConfigObject.getInstance().areClickableRecipeArrowsEnabled();
    }
}
