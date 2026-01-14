package sb.rocket.giovanniclient.client.bootstrap;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds {

    private ClientKeybinds() {}

    private static KeyBinding openConfig;
    private static KeyBinding openInvButtonEditor;

    public static void register() {
        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open Config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "GiovanniClient"
        ));

        openInvButtonEditor = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.giovanniclient.open_inv_button_editor",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                "category.giovanniclient"
        ));
    }

    public static KeyBinding openConfig() {
        return openConfig;
    }

    public static KeyBinding openInvButtonEditor() {
        return openInvButtonEditor;
    }
}
