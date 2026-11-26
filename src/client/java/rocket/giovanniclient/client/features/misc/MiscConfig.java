package rocket.giovanniclient.client.features.misc;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class MiscConfig {

    // hoppity is NOT fun so it does not belong in the fun config, fuck the eggs!
    @Expose
    @ConfigOption(name = "No egg GUI", desc = "Automatically close chocolate egg GUI during Hoppity's hunt")
    @ConfigEditorBoolean
    public boolean CLOSE_EGGS = false;

    @Expose
    @ConfigOption(name = "No Text Shadows", desc = "Thanks Sciophobia!")
    @ConfigEditorBoolean
    public boolean NO_TEXT_SHADOWS = false;

    @Expose
    @ConfigOption(name = "zsh chat mode", desc = "Use Up/Down to browse sent messages and commands starting with your typed text. Tab still completes commands.")
    @ConfigEditorBoolean
    public boolean CHAT_HISTORY_PREFIX = false;

    @Expose
    @ConfigOption(name = "Ctrl to Walk", desc = "Sprint automatically. Hold your Sprint key (Left Ctrl by default) to walk.")
    @ConfigEditorBoolean
    public boolean CTRL_TO_WALK = false;

    @Expose
    @ConfigOption(name = "AutoReconnect", desc = "Reconnect after 5 seconds")
    @ConfigEditorBoolean
    public boolean AUTO_RECONNECT = false;

    @Expose
    @ConfigOption(name = "Sea Creatures Shutup", desc = "Mute mob hurt and death sounds while holding a fishing rod")
    @ConfigEditorBoolean
    public boolean MUTE_FISHING_MOB_SOUNDS = false;
}
