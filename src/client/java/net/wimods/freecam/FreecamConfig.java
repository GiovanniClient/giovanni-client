package net.wimods.freecam;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import rocket.giovanniclient.client.GiovanniClientClient;

public class FreecamConfig {
    @Expose
    @ConfigOption(name = "Apply input to...", desc = "")
    @ConfigEditorDropdown
    public Property<InputEnum> APPLY_INPUT_TO = Property.of(InputEnum.Camera);

    @Expose
    @ConfigOption(name = "Horizontal Speed", desc = "")
    @ConfigEditorSlider(minValue = 100, maxValue = 1000, minStep = 50)
    public int HORIZONTAL_SPEED = 100;

    @Expose
    @ConfigOption(name = "Vertical Speed", desc = "")
    @ConfigEditorSlider(minValue = 100, maxValue = 500, minStep = 50)
    public int VERTICAL_SPEED = 100;

    @Expose
    @ConfigOption(name = "Scroll to change speed", desc = "")
    @ConfigEditorBoolean
    public boolean SCROLL_TO_CHANGE_SPEED = true;

    @Expose
    @ConfigOption(name = "Print speed to chat", desc = "")
    @ConfigEditorBoolean
    public boolean PRINT_SPEED_TO_CHAT = false;

    @Expose
    @ConfigOption(name = "Freecam Spawn Position", desc = "where the camera spawns when you enable freecam")
    @ConfigEditorDropdown
    public Property<InitialPosition> CAMERA_SPAWN_POSITION = Property.of(InitialPosition.INSIDE);

    @Expose
    @ConfigOption(name = "Tracer", desc = "Draw a line between your camera and your player")
    @ConfigEditorBoolean
    public boolean FREECAM_TRACER = false;

    @Expose
    @ConfigOption(name = "Tracer Color", desc = "")
    @ConfigEditorColour
    public ChromaColour FREECAM_TRACER_COLOR = ChromaColour.fromRGB(0, 0, 0, 0, 255);

    @Expose
    @ConfigOption(name = "Hide Hand", desc = "")
    @ConfigEditorBoolean
    public boolean HIDE_HAND = true;

    @Expose
    @ConfigOption(name = "Disable on damage", desc = "")
    @ConfigEditorBoolean
    public boolean DISABLE_ON_DAMAGE = true;

    public enum InputEnum {
        Player,
        Camera
    }

    public enum InitialPosition {
        INSIDE("Inside") {
            @Override
            public Vec3 getOffset() {
                return Vec3.ZERO;
            }
        },

        IN_FRONT("In Front") {
            @Override
            public Vec3 getOffset() {
                double distance = 0.55 * GiovanniClientClient.mc.player.getScale();
                float yawRad = GiovanniClientClient.mc.player.getYRot() * Mth.DEG_TO_RAD;
                double offsetX = -Mth.sin(yawRad) * distance;
                double offsetZ = Mth.cos(yawRad) * distance;
                return new Vec3(offsetX, 0, offsetZ);
            }
        },

        ABOVE("Above") {
            @Override
            public Vec3 getOffset() {
                double distance = 0.55 * GiovanniClientClient.mc.player.getScale();
                return new Vec3(0, distance, 0);
            }
        };

        public final String name;

        private InitialPosition(String name) {
            this.name = name;
        }

        public abstract Vec3 getOffset();

        @Override
        public String toString() {
            return name;
        }
    }

    public void increaseHorizontalSpeed() {
        if (HORIZONTAL_SPEED + 5 <= 1000)  // Fixed: was 100
            HORIZONTAL_SPEED += 5;
    }

    public void decereaseHorizontalSpeed() {
        if (HORIZONTAL_SPEED - 5 >= 100)
            HORIZONTAL_SPEED -= 5;
    }

    public void increaseVerticalSpeed() {
        if (VERTICAL_SPEED + 5 <= 500)   // Fixed: was 50
            VERTICAL_SPEED += 5;
    }

    public void decreaseVerticalSpeed() {
        if (VERTICAL_SPEED - 5 >= 100)
            VERTICAL_SPEED -= 5;
    }

    public double getActualVerticalSpeed() {
        return Mth.clamp(HORIZONTAL_SPEED / 200.0 * VERTICAL_SPEED / 200.0,0.05, 10);
    }

    public void cycleInputMode() {
        InputEnum current = APPLY_INPUT_TO.get();
        InputEnum[] values = InputEnum.values();
        InputEnum next = values[(current.ordinal() + 1) % values.length];
        APPLY_INPUT_TO.set(next);
    }
}