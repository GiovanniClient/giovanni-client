package rocket.giovanniclient.client.features.inventorybuttons.rei;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProvider;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.CollapsibleEntryRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiCraftingDisplay;
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiEntryDefinition;
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiItemRepository;
import rocket.giovanniclient.giovanniclient.rei.SkyBlockReiSimpleRecipeDisplay;
import rocket.giovanniclient.client.features.inventorybuttons.EditModeState;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.EditModeOverlay;
import rocket.giovanniclient.client.features.inventorybuttons.overlay.OverlayManager;

import java.util.stream.Stream;

public class GiovanniReiPlugin implements REIClientPlugin {
    private static boolean refreshQueued;

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new SkyBlockReiCraftingCategory());
        registry.add(new SkyBlockReiSimpleRecipeCategory(
                SkyBlockReiSimpleRecipeDisplay.FORGE_CATEGORY,
                Component.literal("SkyBlock Forge"),
                new ItemStack(Items.ANVIL)
        ));
        registry.add(new SkyBlockReiSimpleRecipeCategory(
                SkyBlockReiSimpleRecipeDisplay.KAT_CATEGORY,
                Component.literal("Kat Upgrade"),
                new ItemStack(Items.NAME_TAG)
        ));
        registry.add(new SkyBlockReiSimpleRecipeCategory(
                SkyBlockReiSimpleRecipeDisplay.SHOP_CATEGORY,
                Component.literal("SkyBlock Shop"),
                new ItemStack(Items.EMERALD)
        ));
        registry.add(new SkyBlockReiSimpleRecipeCategory(
                SkyBlockReiSimpleRecipeDisplay.TRADE_CATEGORY,
                Component.literal("SkyBlock Trade"),
                new ItemStack(Items.EMERALD)
        ));
        registry.add(new SkyBlockReiSimpleRecipeCategory(
                SkyBlockReiSimpleRecipeDisplay.ESSENCE_CATEGORY,
                Component.literal("Essence Upgrade"),
                new ItemStack(Items.NETHER_STAR)
        ));
        registry.add(new SkyBlockReiSimpleRecipeCategory(
                SkyBlockReiSimpleRecipeDisplay.REFORGE_CATEGORY,
                Component.literal("Reforge"),
                new ItemStack(Items.ANVIL)
        ));
        registry.add(new SkyBlockReiMobDropCategory());
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        if (!SkyBlockReiItemRepository.isLoaded()) {
            SkyBlockReiItemRepository.warmupIfReiIsLoaded();
            queueRefreshWhenLoaded();
            return;
        }

        SkyBlockReiItemRepository.getCraftingRecipesForRei().forEach(recipe -> registry.add(recipe));
        SkyBlockReiItemRepository.getSimpleRecipesForRei().forEach(recipe -> registry.add(recipe));
        SkyBlockReiItemRepository.getMobDropsForRei().forEach(recipe -> registry.add(recipe));
    }

    @Override
    public void registerCollapsibleEntries(CollapsibleEntryRegistry registry) {
        if (!SkyBlockReiItemRepository.isLoaded()) {
            SkyBlockReiItemRepository.warmupIfReiIsLoaded();
            queueRefreshWhenLoaded();
            return;
        }

        SkyBlockReiItemRepository.getParentGroupsForRei().forEach((parent, children) -> {
            var entries = Stream.concat(children.stream(), Stream.of(parent))
                    .map(id -> EntryStack.of(SkyBlockReiEntryDefinition.INSTANCE, SkyBlockReiItemRepository.getItemById(id)))
                    .toList();
            registry.group(
                    net.minecraft.resources.Identifier.fromNamespaceAndPath("skyblock", sanitizePath(parent)),
                    Component.literal(SkyBlockReiItemRepository.getItemById(parent).displayName()),
                    entries
            );
        });
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        if (!SkyBlockReiItemRepository.isLoaded()) {
            SkyBlockReiItemRepository.warmupIfReiIsLoaded();
            queueRefreshWhenLoaded();
            return;
        }

        replaceEntries(registry);
    }

    private static void replaceEntries(EntryRegistry registry) {
        var items = SkyBlockReiItemRepository.getItemsForRei();
        if (items.isEmpty()) {
            return;
        }

        registry.removeEntryIf(entry -> true);
        items.forEach(item ->
                registry.addEntry(EntryStack.of(SkyBlockReiEntryDefinition.INSTANCE, item.copy())));
        if (!registry.isReloading()) {
            registry.refilter();
        }
    }

    @Override
    public void registerTransferHandlers(TransferHandlerRegistry registry) {
        registry.register((TransferHandler) context -> {
            if (!(context.getDisplay() instanceof SkyBlockReiCraftingDisplay display)) {
                return TransferHandler.Result.createNotApplicable();
            }
            if (!context.isActuallyCrafting()) {
                return TransferHandler.Result.createSuccessful();
            }

            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.player.connection == null) {
                return TransferHandler.Result.createFailed(Component.literal("Not connected to a server."));
            }
            client.player.connection.sendCommand("viewrecipe " + display.recipeId());
            return TransferHandler.Result.createSuccessful().blocksFurtherHandling(false);
        });
    }

    private static void queueRefreshWhenLoaded() {
        if (refreshQueued) {
            return;
        }

        refreshQueued = true;
        SkyBlockReiItemRepository.whenLoaded(() -> Minecraft.getInstance().execute(() -> {
            refreshQueued = false;
            replaceEntries(EntryRegistry.getInstance());
            SkyBlockReiItemRepository.getCraftingRecipesForRei().forEach(recipe -> DisplayRegistry.getInstance().add(recipe));
            SkyBlockReiItemRepository.getSimpleRecipesForRei().forEach(recipe -> DisplayRegistry.getInstance().add(recipe));
            SkyBlockReiItemRepository.getMobDropsForRei().forEach(recipe -> DisplayRegistry.getInstance().add(recipe));
        }));
    }

    private static String sanitizePath(String id) {
        return id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerDraggableStackProvider(new InventoryButtonReiStackProvider());
        registry.registerDraggableStackVisitor(new InventoryButtonIconDropTarget());
    }

    private static EditModeOverlay getEditOverlay() {
        if (!EditModeState.isEditMode()) {
            return null;
        }

        if (OverlayManager.activeOverlay instanceof EditModeOverlay editOverlay) {
            return editOverlay;
        }

        return null;
    }

    private static final class InventoryButtonReiStackProvider implements DraggableStackProvider<Screen> {
        @Override
        public DraggableStack getHoveredStack(DraggingContext<Screen> context, double mouseX, double mouseY) {
            if (getEditOverlay() == null) {
                return null;
            }

            return ReiOverlayHelper.getHoveredEntryStack(mouseX, mouseY)
                    .map(InventoryButtonReiDraggableStack::new)
                    .orElse(null);
        }

        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            return getEditOverlay() != null;
        }

        @Override
        public double getPriority() {
            return 1000;
        }
    }

    private record InventoryButtonReiDraggableStack(EntryStack<?> stack) implements DraggableStack {
        @Override
        public EntryStack<?> getStack() {
            return stack;
        }

        @Override
        public void drag() {
        }
    }

    private static final class InventoryButtonIconDropTarget implements DraggableStackVisitor<Screen> {
        @Override
        public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
            EditModeOverlay editOverlay = getEditOverlay();
            if (editOverlay == null) {
                return DraggedAcceptorResult.PASS;
            }

            Point position = context.getCurrentPosition();
            if (position == null || !editOverlay.isMouseOverPanel(position.x, position.y)) {
                return DraggedAcceptorResult.PASS;
            }

            ItemStack itemStack = stack.getStack().cheatsAs().getValue();
            editOverlay.applyDraggedIcon(itemStack.copy());
            return DraggedAcceptorResult.CONSUMED;
        }

        @Override
        public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
            EditModeOverlay editOverlay = getEditOverlay();
            if (editOverlay == null) {
                return Stream.empty();
            }

            Rectangle bounds = new Rectangle(
                    editOverlay.getPanelX(),
                    editOverlay.getPanelY(),
                    editOverlay.getPanelWidth(),
                    editOverlay.getPanelHeight()
            );
            return Stream.of(BoundsProvider.ofRectangle(bounds));
        }

        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            return getEditOverlay() != null;
        }

        @Override
        public double getPriority() {
            return 1000;
        }
    }
}
