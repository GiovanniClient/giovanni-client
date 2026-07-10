package rocket.giovanniclient.client.features.render;

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
        @ConfigOption(name = "Dungeon ESP", desc = "Highlights fels, shadow assassins, and starred mobs in Dungeons.")
        @ConfigEditorBoolean
        public boolean DUNGEON_ESP = false;

        @Expose
        @ConfigOption(name = "Frozen Corpses ESP", desc = "Highlights corpses in tunnels.")
        @ConfigEditorBoolean
        public boolean MINESHAFT_CORPSES_ESP = false;

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
        @ConfigOption(name = "No Fog Effects", desc = "Removes fog, blindness, and darkness effects.")
        @ConfigEditorBoolean
        public boolean NO_FOG_EFFECTS = false;

        @Expose
        @ConfigOption(name = "No Nausea Effect", desc = "")
        @ConfigEditorBoolean
        public boolean NO_NAUSEA = false;

        @Expose
        @ConfigOption(name = "No Camera Overlays", desc = "Removes fire, in-wall, and pumpkin overlays.")
        @ConfigEditorBoolean
        public boolean NO_CAMERA_OVERLAYS = false;
    }

    @Expose
    @ConfigOption(name = "No Hurtcam", desc = "")
    @ConfigEditorBoolean
    public boolean NO_HURT_CAM = false;
}
