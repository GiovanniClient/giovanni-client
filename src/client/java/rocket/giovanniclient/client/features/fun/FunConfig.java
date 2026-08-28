package rocket.giovanniclient.client.features.fun;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FunConfig {
    @Expose
    @ConfigOption(name = "Fake Ironman", desc = "Tony Stark")
    @ConfigEditorBoolean
    public boolean FAKE_IRONMAN_TOGGLE = false;

    @Expose
    @ConfigOption(name = "Troll Features", desc = "Keeps skyblock fun")
    @ConfigEditorBoolean
    public boolean TROLL_FEATURES = true;

    @Expose
    @ConfigOption(name = "3D Rats", desc = "Replaces Hypixel rat armor stand heads with a small 3D rat model.")
    @ConfigEditorBoolean
    public boolean RENDER_3D_RATS = true;

    @Expose
    @ConfigOption(name = "SBO AFK Timeout patch", desc = "iykyk.")
    @ConfigEditorBoolean
    public boolean SBO_ONE_SECOND_AFK_TIMEOUT = false;
}
