package sb.rocket.giovanniclient.client.features.slayers;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SlayersConfig {
    @Expose
    @Accordion
    @ConfigOption(name = "Voidgloom Seraph", desc = "")
    public EndermanAccordion eman = new EndermanAccordion();
    public static class EndermanAccordion {
        @Expose
        @Accordion
        @ConfigOption(name = "Auto Soulcry", desc = "")
        public AutoSoulcry soulcry = new AutoSoulcry();
        public static class AutoSoulcry {
            @Expose
            @ConfigOption(name = "Enable", desc = "Automatically uses the slayer's katana's Soulcry ability during boss fights")
            @ConfigEditorBoolean
            public boolean AUTO_SOULCRY = false;

            @Expose
            @ConfigOption(name = "Additional delay", desc = "Adds an additional delay (in milliseconds) before using an ability")
            @ConfigEditorSlider(minValue = 0, maxValue = 5000, minStep = 1)
            public int ADDITIONAL_DELAY = 0;

            @Expose
            @ConfigOption(name = "Minimal mana", desc = "The minimum amount of mana a player needs to use an ability")
            @ConfigEditorSlider(minValue = 200, maxValue = 3000, minStep = 25)
            public int MINIMAL_MANA = 400;

            @Expose
            @ConfigOption(name = "Only in bossfight", desc = "Uses ability only when boss is alive")
            @ConfigEditorBoolean
            public boolean BOSSFIGHT_ONLY = false;
        }
    }
}
