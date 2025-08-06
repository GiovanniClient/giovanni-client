package sb.rocket.giovanniclient.client.features.autosolvers;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.*;

public class AutoSolversConfig {

    @Expose
    @ConfigOption(name = "Auto Harp", desc = "Automatically plays Harp songs")
    @ConfigEditorBoolean
    public boolean AUTOMELODY_TOGGLE = false;
//
//    @Expose
//    @ConfigOption(name = "Click Type", desc = "Changes how to interact with the items in the harp.")
//    @ConfigEditorDropdown
//    public transient SlotActionType AUTOMELODY_CLICKTYPE; // good for experiments too
    @Expose
    @Accordion
    @ConfigOption(name = "Auto Experiments", desc = "")
    public AutoExperimentsAccordion autoExperimentsAccordion = new AutoExperimentsAccordion();
    public static class AutoExperimentsAccordion {

        @Expose
        @ConfigOption(name = "Enabled", desc = "Main Toggle")
        @ConfigEditorBoolean
        public boolean AUTOEXPERIMENTS_TOGGLE = false;

        @Expose
        @ConfigOption(name = "Min Click Delay", desc = "Please don't be dumb")
        @ConfigEditorSlider(minValue = 234, maxValue = 1000, minStep = 25)
        public int AUTOEXPERIMENTS_CLICK_DELAY_MIN = 500;

        @Expose
        @ConfigOption(name = "Max Click Delay", desc = "")
        @ConfigEditorSlider(minValue = 678, maxValue = 1000, minStep = 25)
        public int AUTOEXPERIMENTS_CLICK_DELAY_MAX = 700;

        public enum MetaphysicalSerum {
            One,   // ordinal = 0
            Two,   // ordinal = 1
            Three; // ordinal = 2

            /** Returns 1,2,3 instead of 0,1,2 */
            public int toInt() {
                return this.ordinal() + 1;
            }
        }

        @Expose
        @ConfigOption(name = "Metaphysical Serum", desc = "Select how many you've eaten")
        @ConfigEditorDropdown
        public MetaphysicalSerum METAPHYSICAL_SERUM = MetaphysicalSerum.One;

        @Expose
        @ConfigOption(name = "AutoQuit", desc = "Quits the experiments at the end")
        @ConfigEditorBoolean
        public boolean AUTOEXPERIMENTS_AUTOQUIT = false;
    }

    @Expose
    @Accordion
    @ConfigOption(name = "AutoFusion", desc = "Automatically repeats Shard Fusions")
    public AutoFusionAccordtion autoFusionAccordtion = new AutoFusionAccordtion();
    public static class AutoFusionAccordtion {
        @Expose
        @ConfigOption(name = "AutoFusion Toggle", desc = "Repeats last fusion in loop")
        @ConfigEditorBoolean
        public boolean AUTOFUSION = false;

        @Expose
        @ConfigOption(name = "Min Click Delay", desc = "Please don't be dumb")
        @ConfigEditorSlider(minValue = 300, maxValue = 1000, minStep = 50)
        public int AUTOFUSION_CLICK_DELAY_MIN = 400;

        @Expose
        @ConfigOption(name = "Max Click Delay", desc = "")
        @ConfigEditorSlider(minValue = 400, maxValue = 5000, minStep = 50)
        public int AUTOFUSION_CLICK_DELAY_MAX = 800;

        @Expose
        @ConfigOption(name = "Auto Shards Claim", desc = "")
        @ConfigEditorBoolean
        public boolean AUTOSHARDSCLAIM = false;

        @Expose
        @ConfigOption(name = "Shard Name", desc = "The name of the shard to automatically claim")
        @ConfigEditorText
        public String NAME_OF_THE_SHARD_TO_CLAIM = "Wobbuffett";
    }


}