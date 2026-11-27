package rocket.giovanniclient.client.features.dungeons;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DungeonConfig {
    @Expose
    @ConfigOption(name = "Spirit Leap Menu", desc = "Show your dungeon teammates in a pentagon, grouped by class.")
    @ConfigEditorBoolean
    public boolean leapMenu = false;

    @Expose
    @ConfigOption(name = "Announce Leaps", desc = "Tell party chat who you leaped to after the server confirms the teleport.")
    @ConfigEditorBoolean
    public boolean announceLeaps = false;
}
