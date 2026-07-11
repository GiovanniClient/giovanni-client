package rocket.giovanniclient.giovanniclient.rei;

import me.shedaniel.rei.api.common.entry.type.EntryTypeRegistry;
import me.shedaniel.rei.api.common.plugins.REICommonPlugin;

public final class GiovanniReiCommonPlugin implements REICommonPlugin {
    @Override
    public void registerEntryTypes(EntryTypeRegistry registry) {
        registry.register(SkyBlockReiEntryDefinition.TYPE_ID, SkyBlockReiEntryDefinition.INSTANCE);
        SkyBlockReiItemRepository.warmupIfReiIsLoaded();
    }
}
