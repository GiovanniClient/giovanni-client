package sb.rocket.giovanniclient.client.features.rift;
// Dreadfarm, Around Colosseum, West Village, Colosseum, Village Plaza, Infested House

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RiftConfig {
    @Expose
    @ConfigOption(name = "Auto Agaricus Cap", desc = "The annoying mushrooms")
    @ConfigEditorBoolean
    public boolean AUTOAGARICUS_CAP_TOGGLE = false;
}
