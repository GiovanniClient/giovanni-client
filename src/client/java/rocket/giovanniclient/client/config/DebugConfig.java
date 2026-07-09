package rocket.giovanniclient.client.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;
import io.github.notenoughupdates.moulconfig.observer.Property;

public class DebugConfig {
    @Expose
    @ConfigOption(name = "Toggle Debug Mode", desc = "rip 2 your chat")
    @ConfigEditorBoolean
    public boolean DEBUG = false;

    @Expose
    @ConfigOption(name = "See every invisible entity", desc = "")
    @ConfigEditorBoolean
    public boolean EVERYTHING_VISIBLE_TOGGLE = false;

    @Expose
    @ConfigOption(name = "See invis armor stands", desc = "")
    @ConfigEditorBoolean
    public boolean SEE_INVISIBLE_ARMOR_STANDS = false;

    @Expose
    @ConfigOption(name = "Yggdrasil", desc = "")
    @ConfigEditorBoolean
    public Property<Boolean> YGGDRASIL = Property.of(false);
}
