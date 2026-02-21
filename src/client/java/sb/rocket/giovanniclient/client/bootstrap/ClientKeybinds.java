package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds {

    private ClientKeybinds() {}

    private static KeyBinding openConfig;
    private static KeyBinding openInvButtonEditor;
    public static KeyBinding toggleFreecam;
    public static KeyBinding switchFreecamControlKey;
    public static KeyBinding[] ALL_KEYS = {null, null, null};

    public static final KeyBinding.Category GIOVANNI = KeyBinding.Category.create(Identifier.of("giovanni"));
    //public static final String GIOVANNI = "key.categories.giovanni";

    public static final KeyBinding.Category WURST = KeyBinding.Category.create(Identifier.of("wurst"));
    //public static final String CATEGORY = "key.categories.wi_freecam";

    public static void register() {
        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                GIOVANNI
        ));

        openInvButtonEditor = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.giovanniclient.open_inv_button_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                GIOVANNI
        ));

        toggleFreecam = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wi_freecam.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                WURST
        ));

        switchFreecamControlKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.wi_freecam.switch_control",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(),
                WURST
        ));

        ALL_KEYS = new KeyBinding[]{toggleFreecam, switchFreecamControlKey};
    }

    public static KeyBinding openConfig() {
        return openConfig;
    }

    public static KeyBinding openInvButtonEditor() {
        return openInvButtonEditor;
    }
}
