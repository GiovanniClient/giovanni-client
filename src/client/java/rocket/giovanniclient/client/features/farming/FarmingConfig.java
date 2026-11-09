package rocket.giovanniclient.client.features.farming;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class FarmingConfig {
    @Expose
    @ConfigOption(name = "Magic Jellybean Hider", desc = "Hide Magic Jellybean armor stands and change the appearance of their sugar cane in the Garden.")
    @ConfigEditorDropdown
    public JellybeanHiderMode MAGIC_JELLYBEAN_HIDER = JellybeanHiderMode.DISABLED;

    public enum JellybeanHiderMode {
        DISABLED("Disabled"),
        COPPER_CHAIN("Copper Chain"),
        SEMI_HIDDEN("Semi-Hidden");

        private final String label;

        JellybeanHiderMode(String label) { this.label = label; }

        @Override
        public String toString() { return label; }
    }

    @ConfigOption(name = "Auto Pest Cycle", desc = "Toggle with /gio farming for this Garden visit. Always starts disabled. Farming and killing pests remain manual.")
    @ConfigEditorButton(buttonText = "§lToggle!")
    public Runnable toggle = AutoPestCycle::toggle;

    @Expose
    @ConfigOption(name = "Jingles", desc = "Play the opening and closing melodies for automatic commands and loadout changes.")
    @ConfigEditorBoolean
    public boolean JINGLES = true;

    @Expose
    @ConfigOption(name = "Return With 1 Pest", desc = "ON: return to farming when Alive <= 1. OFF: wait until Alive = 0.")
    @ConfigEditorBoolean
    public boolean RETURN_WITH_ONE_PEST = true;

    @Expose
    @Accordion
    @ConfigOption(name = "Loadout", desc = "Choose the setup and loadout slot for each phase.")
    public Loadouts loadouts = new Loadouts();

    public static class Loadouts {
        @Expose
        @Accordion
        @ConfigOption(name = "Farming", desc = "Selected after returning to the Garden, while waiting for the internal cooldown.")
        public LoadoutConfig farming = new LoadoutConfig(LoadoutSlot.TEN);

        @Expose
        @Accordion
        @ConfigOption(name = "Spawning Pests", desc = "Starts switching 5-12 seconds before the captured internal cooldown ends.")
        public LoadoutConfig spawn = new LoadoutConfig(LoadoutSlot.ELEVEN);

        @Expose
        @Accordion
        @ConfigOption(name = "Killing Pests", desc = "Selected after the new spawn cooldown is captured, before teleporting to its plot.")
        public LoadoutConfig kill = new LoadoutConfig(LoadoutSlot.TWELVE);
    }

    public static class LoadoutConfig {
        @Expose
        @ConfigOption(name = "Change Loadout", desc = "Disable to skip both /loadouts and the click for this phase.")
        @ConfigEditorBoolean
        public boolean ENABLED = true;

        @Expose
        @ConfigOption(name = "Slot", desc = "Page 1 of /loadouts: count left to right, top to bottom. Rows: 1-3, 4-6, 7-9, 10-12.")
        @ConfigEditorDropdown
        public LoadoutSlot BUTTON = LoadoutSlot.ONE;

        public LoadoutConfig() {}

        public LoadoutConfig(LoadoutSlot slot) {
            BUTTON = slot;
        }

        PestCycle.Loadout settings() {
            return new PestCycle.Loadout(ENABLED, BUTTON == null ? 0 : BUTTON.button());
        }
    }

    public enum LoadoutSlot {
        @SerializedName("1") ONE(1),
        @SerializedName("2") TWO(2),
        @SerializedName("3") THREE(3),
        @SerializedName("4") FOUR(4),
        @SerializedName("5") FIVE(5),
        @SerializedName("6") SIX(6),
        @SerializedName("7") SEVEN(7),
        @SerializedName("8") EIGHT(8),
        @SerializedName("9") NINE(9),
        @SerializedName("10") TEN(10),
        @SerializedName("11") ELEVEN(11),
        @SerializedName("12") TWELVE(12);

        private final int button;

        LoadoutSlot(int button) { this.button = button; }

        public int button() { return button; }

        @Override
        public String toString() { return Integer.toString(button); }
    }

    @Expose
    @Accordion
    @ConfigOption(name = "Action Delays", desc = "A fresh random delay between commands and clicks, like Auto Experiments.")
    public DelayConfig delays = new DelayConfig();

    public static class DelayConfig {
        @Expose
        @ConfigOption(name = "Min Action Delay", desc = "Minimum delay in milliseconds. If minimum exceeds maximum, the two values are swapped.")
        @ConfigEditorSlider(minValue = 1200, maxValue = 1800, minStep = 100)
        public int MIN = 1200;

        @Expose
        @ConfigOption(name = "Max Action Delay", desc = "Maximum delay in milliseconds. Random delays can include any millisecond inside the selected range.")
        @ConfigEditorSlider(minValue = 1200, maxValue = 1800, minStep = 100)
        public int MAX = 1800;
    }

    PestCycle.Settings settings() {
        return new PestCycle.Settings(loadouts.kill.settings(), loadouts.farming.settings(), loadouts.spawn.settings(), delays.MIN, delays.MAX, RETURN_WITH_ONE_PEST);
    }
}
