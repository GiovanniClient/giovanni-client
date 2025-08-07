package sb.rocket.giovanniclient.client.features.fun;

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
    @ConfigOption(name = "No egg GUI", desc = "Automatically close chocolate egg GUI during Hoppity's hunt")
    @ConfigEditorBoolean
    public boolean CLOSE_EGGS = false;
}
