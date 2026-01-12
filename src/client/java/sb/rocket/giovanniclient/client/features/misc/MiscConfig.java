package sb.rocket.giovanniclient.client.features.misc;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiscConfig {

    // hoppity is NOT fun so it does not belong in the fun config, fuck the eggs!
    @Expose
    @ConfigOption(name = "No egg GUI", desc = "Automatically close chocolate egg GUI during Hoppity's hunt")
    @ConfigEditorBoolean
    public boolean CLOSE_EGGS = false;
}
