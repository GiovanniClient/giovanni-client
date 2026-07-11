package rocket.giovanniclient.giovanniclient.rei;

import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import rocket.giovanniclient.giovanniclient.GiovanniClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SkyBlockReiSimpleRecipeDisplay extends BasicDisplay {
    public static final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> FORGE_CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_forge");
    public static final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> KAT_CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_kat");
    public static final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> SHOP_CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_shop");
    public static final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> TRADE_CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_trade");
    public static final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> ESSENCE_CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_essence");
    public static final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> REFORGE_CATEGORY =
            CategoryIdentifier.of(GiovanniClient.MOD_ID, "skyblock_reforge");

    private final CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> category;
    private final List<String> infoLines;

    private SkyBlockReiSimpleRecipeDisplay(
            CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> category,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs,
            List<String> infoLines
    ) {
        super(inputs, outputs, Optional.empty());
        this.category = category;
        this.infoLines = List.copyOf(infoLines);
    }

    public static SkyBlockReiSimpleRecipeDisplay of(
            CategoryIdentifier<SkyBlockReiSimpleRecipeDisplay> category,
            List<String> inputIds,
            String outputId,
            int outputCount,
            List<String> infoLines
    ) {
        List<EntryIngredient> inputs = new ArrayList<>(inputIds.size());
        for (String inputId : inputIds) {
            inputs.add(SkyBlockReiIngredients.ingredient(inputId));
        }

        List<EntryIngredient> outputs = List.of(SkyBlockReiIngredients.ingredient(outputId, outputCount));
        return new SkyBlockReiSimpleRecipeDisplay(category, inputs, outputs, infoLines);
    }

    public List<String> infoLines() {
        return infoLines;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category;
    }

    @Override
    public DisplaySerializer<? extends SkyBlockReiSimpleRecipeDisplay> getSerializer() {
        return null;
    }
}
