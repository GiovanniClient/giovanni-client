package sb.rocket.giovanniclient.client.features.render;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class RenderConfig {

    @Expose
    @Accordion
    @ConfigOption(name = "Entities", desc = "Hide/Show/Make glow entities")
    public RenderEntitiesAccordion renderEntitiesAccordion = new RenderEntitiesAccordion();
    public static class RenderEntitiesAccordion {
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

        @Expose
        @ConfigOption(name = "No Lightning Bolts", desc = "No Lightning bolts rendered, but you can still hear the thunders.")
        @ConfigEditorBoolean
        public boolean NO_LIGHTNING_BOLTS = false;
    }

    @Expose
    @Accordion
    @ConfigOption(name = "Camera", desc = "how you see things")
    public CameraAccordion cameraAccordion = new CameraAccordion();
    public static class CameraAccordion {
        @Expose
        @ConfigOption(name = "No Blindness Effect", desc = "")
        @ConfigEditorBoolean
        public boolean NO_BLINDNESS = false;

        @Expose
        @ConfigOption(name = "No Darkness Effect", desc = "")
        @ConfigEditorBoolean
        public boolean NO_DARKNESS = false;

        @Expose
        @ConfigOption(name = "No Nausea Effect", desc = "")
        @ConfigEditorBoolean
        public boolean NO_NAUSEA = false;

        @Expose
        @ConfigOption(name = "No Fire Overlay", desc = "i can't see sh*t when i'm on fire!")
        @ConfigEditorBoolean
        public boolean NO_FIRE_OVERLAY = false;

        @Expose
        @ConfigOption(name = "No In-Wall overlay", desc = "stop rendering a flat texture when you've got a block on your head")
        @ConfigEditorBoolean
        public boolean NO_BLOCK_OVERLAY = false;

        @Expose
        @ConfigOption(name = "No Pumpkin Overlay", desc = "")
        @ConfigEditorBoolean
        public boolean NO_PUMPKIN_OVERLAY = false;
    }

    @Expose
    @ConfigOption(name = "No Fog", desc = "also makes you see better underwater/lava and when you've got blindness/darkness")
    @ConfigEditorBoolean
    public boolean NO_FOG = false;

    @Expose
    @ConfigOption(name = "No Hurtcam", desc = "")
    @ConfigEditorBoolean
    public boolean NO_HURT_CAM = false;
}
