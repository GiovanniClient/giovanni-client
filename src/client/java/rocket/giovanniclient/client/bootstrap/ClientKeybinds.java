package rocket.giovanniclient.client.bootstrap;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds {

    private ClientKeybinds() {}

    private static KeyMapping openConfig;
    private static KeyMapping openInvButtonEditor;
    public static KeyMapping toggleFreecam;
    public static KeyMapping switchFreecamControlKey;
    public static KeyMapping[] ALL_KEYS = {null, null, null};

    public static final KeyMapping.Category GIOVANNI = KeyMapping.Category.register(Identifier.parse("giovanni"));
    //public static final String GIOVANNI = "key.categories.giovanni";

    public static final KeyMapping.Category WURST = KeyMapping.Category.register(Identifier.parse("wurst"));
    //public static final String CATEGORY = "key.categories.wi_freecam";

    public static void register() {
        openConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "Open Config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                GIOVANNI
        ));

        openInvButtonEditor = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.giovanniclient.open_inv_button_editor",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                GIOVANNI
        ));

        toggleFreecam = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.wi_freecam.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                WURST
        ));

        switchFreecamControlKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.wi_freecam.switch_control",
                InputConstants.Type.KEYSYM,
                InputConstants.UNKNOWN.getValue(),
                WURST
        ));

        ALL_KEYS = new KeyMapping[]{toggleFreecam, switchFreecamControlKey, openInvButtonEditor, openConfig};
    }

    public static KeyMapping openConfig() {
        return openConfig;
    }

    public static KeyMapping openInvButtonEditor() {
        return openInvButtonEditor;
    }
}
