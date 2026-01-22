package sb.rocket.giovanniclient.client.features.render;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RenderConfig {
    @Expose
    @ConfigOption(name = "See every invisible entity", desc = "")
    @ConfigEditorBoolean
    public boolean EVERYTHING_VISIBLE_TOGGLE = false;

    @Expose
    @ConfigOption(name = "Make Fel Visible", desc = "")
    @ConfigEditorBoolean
    public boolean FEL_VISIBLE_TOGGLE = false;

    @Expose
    @ConfigOption(name = "Shadow Assassin ESP", desc = "")
    @ConfigEditorBoolean
    public boolean SHADOW_ASSASSIN_VISIBLE_TOGGLE = false;

    @Expose
    @ConfigOption(name = "Starred Mob ESP", desc = "Glowing on starred mobs (✯) in Dungeons.")
    @ConfigEditorBoolean
    public boolean STARRED_MOB_ESP = false;

}
