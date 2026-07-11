package rocket.giovanniclient.giovanniclient.rei;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;

final class SkyBlockReiIngredients {
    private SkyBlockReiIngredients() {}

    static EntryIngredient ingredient(String neuIngredient) {
        if (neuIngredient == null || neuIngredient.isBlank()) {
            return EntryIngredient.empty();
        }

        String id = neuIngredient;
        int count = 1;
        int countSeparator = neuIngredient.lastIndexOf(':');
        if (hasExplicitCount(neuIngredient)) {
            id = neuIngredient.substring(0, countSeparator);
            count = Math.max(1, Integer.parseInt(neuIngredient.substring(countSeparator + 1)));
        }

        SkyBlockReiItem item = SkyBlockReiItemRepository.getItemById(id).withCount(count);
        return EntryIngredient.of(EntryStack.of(SkyBlockReiEntryDefinition.INSTANCE, item));
    }

    static EntryIngredient ingredient(String neuIngredient, int fallbackCount) {
        if (neuIngredient == null || neuIngredient.isBlank()) {
            return EntryIngredient.empty();
        }
        if (hasExplicitCount(neuIngredient)) {
            return ingredient(neuIngredient);
        }
        return ingredient(neuIngredient + ":" + Math.max(1, fallbackCount));
    }

    private static boolean hasExplicitCount(String neuIngredient) {
        int countSeparator = neuIngredient.lastIndexOf(':');
        if (countSeparator <= 0 || countSeparator == neuIngredient.length() - 1) {
            return false;
        }
        for (int i = countSeparator + 1; i < neuIngredient.length(); i++) {
            if (!Character.isDigit(neuIngredient.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
