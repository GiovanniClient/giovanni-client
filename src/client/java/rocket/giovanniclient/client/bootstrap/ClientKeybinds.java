package rocket.giovanniclient.client.bootstrap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class ClientKeybinds {

    private ClientKeybinds() {}

    private static KeyMapping openConfig;
    private static KeyMapping openInvButtonEditor;
    private static KeyMapping cycleGuiScale;
    private static final Map<String, String> KEY_NAMES = new HashMap<>();

    public static final KeyMapping.Category GIOVANNI = KeyMapping.Category.register(Identifier.parse("giovanni"));
    //public static final String GIOVANNI = "key.categories.giovanni";

    public static final KeyMapping.Category WURST = KeyMapping.Category.register(Identifier.parse("wurst"));
    //public static final String CATEGORY = "key.categories.wi_freecam";

    public static void register() {
        openConfig = key("Open Config", "Open Config", GLFW.GLFW_KEY_K);
        openInvButtonEditor = key("key.giovanniclient.open_inv_button_editor", "Open Inventory Button Editor", GLFW.GLFW_KEY_O);
        // The main keyboard's '+' key on the Italian layout (GLFW uses US key positions).
        cycleGuiScale = key("key.giovanniclient.cycle_gui_scale", "Cycle GUI Scale", GLFW.GLFW_KEY_RIGHT_BRACKET);
    }

    private static KeyMapping key(String name, String displayName, int defaultKey) {
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                name, InputConstants.Type.KEYSYM, defaultKey, GIOVANNI));
        KEY_NAMES.put(name, displayName);
        return key;
    }

    public static boolean isOwnKeybind(String name) {
        return KEY_NAMES.containsKey(name);
    }

    public static String getDisplayName(String name) {
        // Keep these labels out of Minecraft's server-accessible translation table.
        return KEY_NAMES.get(name);
    }

    public static KeyMapping cycleGuiScale() {
        return cycleGuiScale;
    }

    public static KeyMapping openConfig() {
        return openConfig;
    }

    public static KeyMapping openInvButtonEditor() {
        return openInvButtonEditor;
    }
}
