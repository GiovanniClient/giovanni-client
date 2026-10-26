package rocket.giovanniclient.client.features.render;

import net.minecraft.world.level.biome.Biome;

import java.util.Locale;

/** Client-only weather selection, kept across worlds until the game exits. */
public final class WeatherOverride {
    private static Mode mode = Mode.OFF;

    private WeatherOverride() {}

    public static Mode mode() {
        return mode;
    }

    public static void setMode(Mode selected) {
        mode = selected;
    }

    public enum Mode {
        OFF(0.0F, 0.0F, Biome.Precipitation.NONE),
        CLEAR(0.0F, 0.0F, Biome.Precipitation.NONE),
        RAIN(1.0F, 0.0F, Biome.Precipitation.RAIN),
        THUNDER(1.0F, 1.0F, Biome.Precipitation.RAIN),
        SNOW(1.0F, 0.0F, Biome.Precipitation.SNOW);

        private final float rainLevel;
        private final float thunderLevel;
        private final Biome.Precipitation precipitation;

        Mode(float rainLevel, float thunderLevel, Biome.Precipitation precipitation) {
            this.rainLevel = rainLevel;
            this.thunderLevel = thunderLevel;
            this.precipitation = precipitation;
        }

        public boolean isActive() {
            return this != OFF;
        }

        public String commandName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public float rainLevel() {
            return rainLevel;
        }

        public float thunderLevel() {
            return thunderLevel;
        }

        public Biome.Precipitation precipitation() {
            return precipitation;
        }
    }
}
