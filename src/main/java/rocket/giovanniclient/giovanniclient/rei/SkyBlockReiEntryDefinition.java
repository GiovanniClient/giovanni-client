package rocket.giovanniclient.giovanniclient.rei;

import com.mojang.serialization.Codec;
import me.shedaniel.rei.api.client.entry.renderer.EntryRenderer;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.client.gui.widgets.TooltipContext;
import me.shedaniel.rei.api.common.entry.EntrySerializer;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.type.EntryDefinition;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import rocket.giovanniclient.giovanniclient.GiovanniClient;

import java.util.Locale;
import java.util.stream.Stream;

public final class SkyBlockReiEntryDefinition implements EntryDefinition<SkyBlockReiItem> {
    public static final SkyBlockReiEntryDefinition INSTANCE = new SkyBlockReiEntryDefinition();
    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath(GiovanniClient.MOD_ID, "skyblockitems");
    private static final EntryType<SkyBlockReiItem> TYPE = EntryType.deferred(TYPE_ID);
    private static final EntryRenderer<SkyBlockReiItem> RENDERER = new SkyBlockReiItemRenderer();
    private static final EntrySerializer<SkyBlockReiItem> SERIALIZER = new SkyBlockReiItemSerializer();

    private SkyBlockReiEntryDefinition() {}

    @Override
    public Class<SkyBlockReiItem> getValueType() {
        return SkyBlockReiItem.class;
    }

    @Override
    public EntryType<SkyBlockReiItem> getType() {
        return TYPE;
    }

    @Override
    public EntryRenderer<SkyBlockReiItem> getRenderer() {
        return RENDERER;
    }

    @Override
    public Identifier getIdentifier(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return Identifier.fromNamespaceAndPath(GiovanniClient.MOD_ID, sanitizePath(value.id()));
    }

    @Override
    public boolean isEmpty(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return value.id().isBlank();
    }

    @Override
    public SkyBlockReiItem copy(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return value.copy();
    }

    @Override
    public SkyBlockReiItem normalize(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return value;
    }

    @Override
    public SkyBlockReiItem wildcard(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return normalize(entry, value);
    }

    @Override
    public ItemStack cheatsAs(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return value.stack().copy();
    }

    @Override
    public long hash(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value, ComparisonContext context) {
        return value.id().hashCode();
    }

    @Override
    public boolean equals(SkyBlockReiItem o1, SkyBlockReiItem o2, ComparisonContext context) {
        return o1.id().equals(o2.id());
    }

    @Override
    public EntrySerializer<SkyBlockReiItem> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public Component asFormattedText(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return Component.literal(value.displayName());
    }

    @Override
    public Stream<? extends TagKey<?>> getTagsFor(EntryStack<SkyBlockReiItem> entry, SkyBlockReiItem value) {
        return Stream.empty();
    }

    private static String sanitizePath(String id) {
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    private static final class SkyBlockReiItemRenderer implements EntryRenderer<SkyBlockReiItem> {
        @Override
        public void render(EntryStack<SkyBlockReiItem> entry, GuiGraphics graphics, me.shedaniel.math.Rectangle bounds, int mouseX, int mouseY, float delta) {
            EntryStack<ItemStack> itemEntry = EntryStack.of(VanillaEntryTypes.ITEM, entry.getValue().stack());
            VanillaEntryTypes.ITEM.getDefinition().getRenderer().render(itemEntry, graphics, bounds, mouseX, mouseY, delta);
        }

        @Override
        public Tooltip getTooltip(EntryStack<SkyBlockReiItem> entry, TooltipContext context) {
            EntryStack<ItemStack> itemEntry = EntryStack.of(VanillaEntryTypes.ITEM, entry.getValue().stack());
            return VanillaEntryTypes.ITEM.getDefinition().getRenderer().getTooltip(itemEntry, context);
        }
    }

    private static final class SkyBlockReiItemSerializer implements EntrySerializer<SkyBlockReiItem> {
        private static final Codec<SkyBlockReiItem> CODEC = Codec.STRING.xmap(
                SkyBlockReiItemRepository::getItemById,
                SkyBlockReiItem::id
        );
        private static final StreamCodec<RegistryFriendlyByteBuf, SkyBlockReiItem> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SkyBlockReiItem decode(RegistryFriendlyByteBuf input) {
                return SkyBlockReiItemRepository.getItemById(ByteBufCodecs.STRING_UTF8.decode(input));
            }

            @Override
            public void encode(RegistryFriendlyByteBuf output, SkyBlockReiItem value) {
                ByteBufCodecs.STRING_UTF8.encode(output, value.id());
            }
        };

        @Override
        public Codec<SkyBlockReiItem> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SkyBlockReiItem> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
