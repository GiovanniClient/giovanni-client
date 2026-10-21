package rocket.giovanniclient.client.features.misc;

import net.minecraft.client.Minecraft;
import rocket.giovanniclient.client.util.Utils;

public final class GuiScaleCycler {

    private GuiScaleCycler() {}

    public static void cycle(Minecraft client) {
        var window = client.getWindow();
        boolean enforceUnicode = client.isEnforceUnicode();
        int maximum = window.calculateScale(0, enforceUnicode);
        int current = window.getGuiScale();
        // Auto already renders at the maximum. Wrap to the minimum and skip
        // duplicate sizes when Unicode fonts require even scale factors.
        int next = window.calculateScale(current >= maximum ? 1 : current + 1, enforceUnicode);

        // The vanilla option's change listener resizes the entire GUI immediately.
        client.options.guiScale().set(next);
        client.options.save();
        Utils.chat("GUI scale: " + window.getGuiScale() + "x (maximum: " + maximum + "x).");
    }
}
