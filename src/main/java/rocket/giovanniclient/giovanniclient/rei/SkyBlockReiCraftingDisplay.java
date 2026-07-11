package rocket.giovanniclient.giovanniclient.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import rocket.giovanniclient.giovanniclient.GiovanniClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SkyBlockReiCraftingDisplay extends BasicDisplay {
    public static final CategoryIdentifier<SkyBlockReiCraftingDisplay> CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_crafting");
    private final String recipeId;

    private SkyBlockReiCraftingDisplay(String recipeId, List<EntryIngredient> inputs, List<EntryIngredient> outputs) {
        super(inputs, outputs, Optional.empty());
        this.recipeId = recipeId;
    }

    public static SkyBlockReiCraftingDisplay of(String[] slots, String outputId, int outputCount) {
        List<EntryIngredient> inputs = new ArrayList<>(9);
        for (String slot : slots) {
            inputs.add(SkyBlockReiIngredients.ingredient(slot));
        }

        List<EntryIngredient> outputs = List.of(SkyBlockReiIngredients.ingredient(outputId, outputCount));
        return new SkyBlockReiCraftingDisplay(guessRecipeId(outputId), inputs, outputs);
    }

    public String recipeId() {
        return recipeId;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public DisplaySerializer<? extends SkyBlockReiCraftingDisplay> getSerializer() {
        return null;
    }

    private static String guessRecipeId(String outputId) {
        int countSeparator = outputId.lastIndexOf(':');
        if (countSeparator > 0 && countSeparator < outputId.length() - 1) {
            boolean suffixIsCount = true;
            for (int i = countSeparator + 1; i < outputId.length(); i++) {
                if (!Character.isDigit(outputId.charAt(i))) {
                    suffixIsCount = false;
                    break;
                }
            }
            if (suffixIsCount) {
                outputId = outputId.substring(0, countSeparator);
            }
        }
        return outputId.contains(";") ? outputId.substring(0, outputId.indexOf(';')) : outputId;
    }
}
