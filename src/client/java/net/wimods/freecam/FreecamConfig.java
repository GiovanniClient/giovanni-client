package net.wimods.freecam;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static net.wimods.freecam.WiFreecam.MC;

public class FreecamConfig {
    @Expose
    @ConfigOption(name = "Apply input to...", desc = "")
    @ConfigEditorDropdown
    public Property<InputEnum> APPLY_INPUT_TO = Property.of(InputEnum.Camera);

    @Expose
    @ConfigOption(name = "Horizontal Speed", desc = "")
    @ConfigEditorSlider(minValue = 1, maxValue = 100, minStep = 1)
    public int HORIZONTAL_SPEED = 20; // ADDED PUBLIC HERE

    @Expose
    @ConfigOption(name = "Vertical Speed", desc = "")
    @ConfigEditorSlider(minValue = 1, maxValue = 50, minStep = 1)
    public int VERTICAL_SPEED = 20; // ADDED PUBLIC HERE

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
            public Vec3d getOffset() {
                return Vec3d.ZERO;
            }
        },

        IN_FRONT("In Front") {
            @Override
            public Vec3d getOffset() {
                double distance = 0.55 * MC.player.getScale();
                float yawRad = MC.player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
                double offsetX = -MathHelper.sin(yawRad) * distance;
                double offsetZ = MathHelper.cos(yawRad) * distance;
                return new Vec3d(offsetX, 0, offsetZ);
            }
        },

        ABOVE("Above") {
            @Override
            public Vec3d getOffset() {
                double distance = 0.55 * MC.player.getScale();
                return new Vec3d(0, distance, 0);
            }
        };

        public final String name;

        private InitialPosition(String name) {
            this.name = name;
        }

        public abstract Vec3d getOffset();

        @Override
        public String toString() {
            return name;
        }
    }

    public double getActualVerticalSpeed() {
        return MathHelper.clamp(HORIZONTAL_SPEED * VERTICAL_SPEED, 0.05, 10);
    }

    public void increaseSpeed() {
        if (HORIZONTAL_SPEED + 10 <= 100)
            HORIZONTAL_SPEED += 10;
        if (VERTICAL_SPEED + 5 <= 50)
            VERTICAL_SPEED += 5;
    }

    public void decreaseSpeed() {
        if (HORIZONTAL_SPEED - 10 >= 0)
            HORIZONTAL_SPEED -= 10;
        if (VERTICAL_SPEED - 5 >= 0)
            VERTICAL_SPEED -= 5;
    }

    public void cycleInputMode() {
        InputEnum current = APPLY_INPUT_TO.get();
        InputEnum[] values = InputEnum.values();
        InputEnum next = values[(current.ordinal() + 1) % values.length];
        APPLY_INPUT_TO.set(next);
    }
}