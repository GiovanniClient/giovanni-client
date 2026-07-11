package rocket.giovanniclient.giovanniclient.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import rocket.giovanniclient.giovanniclient.GiovanniClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SkyBlockReiMobDropDisplay extends BasicDisplay {
    public static final CategoryIdentifier<SkyBlockReiMobDropDisplay> CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_mob_drops");

    private final String mobName;
    private final List<String> infoLines;
    private final List<Drop> drops;

    private SkyBlockReiMobDropDisplay(String mobName, List<String> infoLines, List<Drop> drops) {
        super(List.of(EntryIngredient.empty()), drops.stream().map(Drop::ingredient).toList(), Optional.empty());
        this.mobName = mobName;
        this.infoLines = List.copyOf(infoLines);
        this.drops = List.copyOf(drops);
    }

    public static SkyBlockReiMobDropDisplay of(String mobName, List<String> infoLines, List<Drop> drops) {
        return new SkyBlockReiMobDropDisplay(mobName, infoLines, drops);
    }

    public String mobName() {
        return mobName;
    }

    public List<String> infoLines() {
        return infoLines;
    }

    public List<Drop> drops() {
        return drops;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public DisplaySerializer<? extends SkyBlockReiMobDropDisplay> getSerializer() {
        return null;
    }

    public record Drop(String ingredientId, String chance, List<String> extraLines) {
        private EntryIngredient ingredient() {
            return SkyBlockReiIngredients.ingredient(ingredientId);
        }

        public List<String> tooltipLines() {
            List<String> lines = new ArrayList<>(extraLines);
            if (chance != null && !chance.isBlank()) {
                lines.add("Chance: " + chance);
            }
            return lines;
        }
    }
}
