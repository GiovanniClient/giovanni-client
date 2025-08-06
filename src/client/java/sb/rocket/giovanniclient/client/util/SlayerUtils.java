package sb.rocket.giovanniclient.client.util;

import net.minecraft.client.MinecraftClient;
import sb.rocket.giovanniclient.client.features.AbstractFeature;

public class SlayerUtils extends AbstractFeature {
    public enum Slayer {
        REVENANT_HORROR(1),
        TARANTULA_BROODFATHER(2),
        SVEN_PACKMASTER(3),
        VOIDGLOOM_SERAPH(4),
        RIFTSTALKER_BLOODFIEND(5),
        INFERNO_DEMONLORD(6),
        NULL(-1);

        public int tier;
        public int value;

        Slayer(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return switch (this.value) {
                case 1 -> "Revenant Horror";
                case 2 -> "Tarantula Broodfather";
                case 3 -> "Sven Packmaster";
                case 4 -> "Voidgloom Seraph";
                case 5 -> "Riftstalker Bloodfiend";
                case 6 -> "Inferno Demondlord";

                default -> "None";
            };
        }
    }

    private int tick = 0;
    private static Slayer CURRENT_SLAYER;

    private final Slayer[] slayers = {
        Slayer.REVENANT_HORROR,
        Slayer.TARANTULA_BROODFATHER,
        Slayer.SVEN_PACKMASTER,
        Slayer.VOIDGLOOM_SERAPH,
        Slayer.RIFTSTALKER_BLOODFIEND,
        Slayer.INFERNO_DEMONLORD
    };

    private final String[] prefixes = {
        "Revenant Horror ",
        "Tarantula Broodfather ",
        "Sven Packmaster ",
        "Voidgloom Seraph ",
        "Riftstalker Bloodfiend ",
        "Inferno Demondlord "
    };

    private int parseRomanNumerals(String numeral) {
        return switch (numeral) {
            case "I" -> 1;
            case "II" -> 2;
            case "III" -> 3;
            case "IV" -> 4;
            case "V" -> 5;
            default -> 0;
        };
    }

    private Slayer parseSlayerString(String raw) {
        for (int i = 0; i < slayers.length; i++) {
            String clear = raw.replaceAll(prefixes[i], "");
            if (!clear.equals(raw)) {
                int tier = parseRomanNumerals(clear);
                if (tier == 0) return Slayer.NULL;

                Slayer slayer = slayers[i];
                slayer.tier = tier;
                return slayer;
            }
        }

        return Slayer.NULL;
    }

    @Override
    public void onTick(MinecraftClient client) {
        tick++;

        if (tick % 60 == 0) {
            int slayerQuest = ScoreboardUtils.getLineIndexOfString("Slayer Quest");
            if (slayerQuest > 0) {
                String currentSlayer = ScoreboardUtils.getStringAtIndex(slayerQuest + 1);
                if (!currentSlayer.isEmpty()) {
                    CURRENT_SLAYER = parseSlayerString(currentSlayer);
                }
            }
        }
    }

    public static Slayer getCurrentSlayer() {
        return CURRENT_SLAYER;
    }
}
