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
    @ConfigOption(name = "Dex Dev", desc = "Copy the Pokédex from the jar into your editable user files, then use only those files while enabled.")
    @ConfigEditorBoolean
    public Property<Boolean> DEX_DEV = Property.of(false);

    @Expose
    @ConfigOption(name = "Use Test Update Repository", desc = "Check GiovanniClient/test-gc-updates-repo for updates.")
    @ConfigEditorBoolean
    public boolean USE_TEST_UPDATE_REPOSITORY = false;

    @Expose
    @ConfigOption(name = "See every invisible entity", desc = "")
    @ConfigEditorBoolean
    public boolean EVERYTHING_VISIBLE_TOGGLE = false;

    @Expose
    @ConfigOption(name = "See invis armor stands", desc = "")
    @ConfigEditorBoolean
    public boolean SEE_INVISIBLE_ARMOR_STANDS = false;

    @Expose
    @ConfigOption(name = "Yggdrasil", desc = "Enable Yggdrasil debug handling.")
    @ConfigEditorBoolean
    public Property<Boolean> YGGDRASIL = Property.of(false);
}
